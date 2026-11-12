package com.tradetwin.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.common.exception.BusinessRuleException;
import com.tradetwin.common.exception.GlobalExceptionHandler;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.model.OrderStatus;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import com.tradetwin.payment.service.PayPalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerHttpTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    static class StubPayPalService extends PayPalService {
        public StubPayPalService() {
            super(null, null, null, null, null, new ObjectMapper());
        }

        @Override
        public Map<String, Object> createOrder(String negotiationId, String dealToken) {
            if ("expired-neg".equals(negotiationId)) {
                throw new BusinessRuleException("Deal agreement lock has expired (15-minute TTL).");
            }
            return Map.of(
                    "paypalOrderId", "SANDBOX-ORDER-HTTP1",
                    "amount", new BigDecimal("89.00"),
                    "currency", "USD",
                    "status", "PENDING",
                    "simulated", true
            );
        }

        @Override
        public Order captureOrder(String paypalOrderId, String negotiationId, String dealToken, Map<String, String> shippingDetails) {
            if ("BAD-TOKEN".equals(dealToken)) {
                throw new BusinessRuleException("Invalid agreement deal token provided for capture authorization");
            }
            return Order.builder()
                    .id("ord-http-1")
                    .orderNumber("TT-HTTP-1")
                    .negotiationId(negotiationId)
                    .finalAmount(new BigDecimal("89.00"))
                    .currency("USD")
                    .orderStatus(OrderStatus.CONFIRMED)
                    .build();
        }

        @Override
        public PaymentRecord cancelOrder(String paypalOrderId, String negotiationId, String reason) {
            return PaymentRecord.builder()
                    .id("pay-cancel-1")
                    .paypalOrderId(paypalOrderId)
                    .negotiationId(negotiationId)
                    .status(PaymentStatus.CANCELLED)
                    .failureReason(reason)
                    .amount(new BigDecimal("89.00"))
                    .currency("USD")
                    .build();
        }

        @Override
        public PaymentRecord recordPaymentFailure(String paypalOrderId, String negotiationId, String reason) {
            return PaymentRecord.builder()
                    .id("pay-fail-1")
                    .paypalOrderId(paypalOrderId)
                    .negotiationId(negotiationId)
                    .status(PaymentStatus.DECLINED)
                    .failureReason(reason)
                    .amount(new BigDecimal("89.00"))
                    .currency("USD")
                    .build();
        }

        @Override
        public boolean verifyWebhookSignature(Map<String, String> headers, String body) {
            return body != null && body.contains("PAYMENT.CAPTURE.COMPLETED");
        }
    }

    @BeforeEach
    void setUp() {
        PaymentController controller = new PaymentController(new StubPayPalService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/payments/paypal/create-order returns server-calculated amount")
    void testCreateOrderHttp() throws Exception {
        mockMvc.perform(post("/api/v1/payments/paypal/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "negotiationId", "neg-sony-1",
                                "dealToken", "tok_valid"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paypalOrderId").value("SANDBOX-ORDER-HTTP1"))
                .andExpect(jsonPath("$.data.amount").value(89.0));
    }

    @Test
    @DisplayName("POST /api/v1/payments/paypal/create-order returns 422 when agreement lock is expired")
    void testCreateOrderExpiredReturns422() throws Exception {
        mockMvc.perform(post("/api/v1/payments/paypal/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "negotiationId", "expired-neg",
                                "dealToken", "tok_expired"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/payments/paypal/capture-order settles order on valid capture and rejects invalid tokens")
    void testCaptureOrderSuccessAndInvalidTokenFailure() throws Exception {
        mockMvc.perform(post("/api/v1/payments/paypal/capture-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "paypalOrderId", "SANDBOX-ORDER-HTTP1",
                                "negotiationId", "neg-sony-1",
                                "dealToken", "tok_valid",
                                "shippingDetails", Map.of("fullName", "Alice Walker", "city", "Springfield")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.finalAmount").value(89.0));

        mockMvc.perform(post("/api/v1/payments/paypal/capture-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "paypalOrderId", "SANDBOX-ORDER-HTTP1",
                                "negotiationId", "neg-sony-1",
                                "dealToken", "BAD-TOKEN"
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/payments/paypal/cancel-order and fail-order persist cancelled/declined states")
    void testCancelAndFailEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/payments/paypal/cancel-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "paypalOrderId", "SANDBOX-ORDER-HTTP1",
                                "negotiationId", "neg-sony-1",
                                "reason", "User clicked cancel"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/payments/paypal/fail-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "paypalOrderId", "SANDBOX-ORDER-HTTP1",
                                "negotiationId", "neg-sony-1",
                                "reason", "Payment instrument declined"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DECLINED"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/paypal/webhook verifies signature and handles event payload")
    void testWebhookEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/payments/paypal/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"event_type\":\"PAYMENT.CAPTURE.COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true));
    }
}
