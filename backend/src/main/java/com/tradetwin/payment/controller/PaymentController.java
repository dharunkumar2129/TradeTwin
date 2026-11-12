package com.tradetwin.payment.controller;

import com.tradetwin.common.api.ApiResponse;
import com.tradetwin.order.model.Order;
import com.tradetwin.payment.model.PaymentAuditEntry;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.service.PayPalService;
import com.tradetwin.payment.service.PaymentAuditService;
import com.tradetwin.payment.service.PaymentExecutionEngine;
import com.tradetwin.user.model.User;
import com.tradetwin.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
public class PaymentController {

    private final PayPalService payPalService;
    private PaymentExecutionEngine paymentExecutionEngine;
    private PaymentAuditService paymentAuditService;
    private UserService userService;

    public PaymentController(PayPalService payPalService) {
        this.payPalService = payPalService;
    }

    @Autowired
    public PaymentController(PayPalService payPalService,
                             @Autowired(required = false) PaymentExecutionEngine paymentExecutionEngine,
                             @Autowired(required = false) PaymentAuditService paymentAuditService,
                             @Autowired(required = false) UserService userService) {
        this.payPalService = payPalService;
        this.paymentExecutionEngine = paymentExecutionEngine;
        this.paymentAuditService = paymentAuditService;
        this.userService = userService;
    }

    public static class CreateOrderRequest {
        private String negotiationId;
        private String dealToken;

        public CreateOrderRequest() {}
        public CreateOrderRequest(String negotiationId, String dealToken) {
            this.negotiationId = negotiationId;
            this.dealToken = dealToken;
        }

        public String getNegotiationId() { return negotiationId; }
        public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
        public String getDealToken() { return dealToken; }
        public void setDealToken(String dealToken) { this.dealToken = dealToken; }
    }

    public static class CaptureOrderRequest {
        private String paypalOrderId;
        private String negotiationId;
        private String dealToken;
        private Map<String, String> shippingDetails;

        public CaptureOrderRequest() {}
        public CaptureOrderRequest(String paypalOrderId, String negotiationId, String dealToken, Map<String, String> shippingDetails) {
            this.paypalOrderId = paypalOrderId;
            this.negotiationId = negotiationId;
            this.dealToken = dealToken;
            this.shippingDetails = shippingDetails;
        }

        public String getPaypalOrderId() { return paypalOrderId; }
        public void setPaypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; }
        public String getNegotiationId() { return negotiationId; }
        public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
        public String getDealToken() { return dealToken; }
        public void setDealToken(String dealToken) { this.dealToken = dealToken; }
        public Map<String, String> getShippingDetails() { return shippingDetails; }
        public void setShippingDetails(Map<String, String> shippingDetails) { this.shippingDetails = shippingDetails; }
    }

    @PostMapping("/paypal/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createOrder(@RequestBody CreateOrderRequest request) {
        Map<String, Object> orderData = payPalService.createOrder(request.getNegotiationId(), request.getDealToken());
        return ResponseEntity.ok(ApiResponse.ok("PayPal order initiated", orderData));
    }

    @PostMapping("/paypal/capture-order")
    public ResponseEntity<ApiResponse<Order>> captureOrder(@RequestBody CaptureOrderRequest request) {
        Order order = payPalService.captureOrder(
                request.getPaypalOrderId(),
                request.getNegotiationId(),
                request.getDealToken(),
                request.getShippingDetails()
        );
        return ResponseEntity.ok(ApiResponse.ok("Order successfully settled and confirmed", order));
    }

    public static class CancelOrFailOrderRequest {
        private String paypalOrderId;
        private String negotiationId;
        private String reason;

        public CancelOrFailOrderRequest() {}
        public CancelOrFailOrderRequest(String paypalOrderId, String negotiationId, String reason) {
            this.paypalOrderId = paypalOrderId;
            this.negotiationId = negotiationId;
            this.reason = reason;
        }

        public String getPaypalOrderId() { return paypalOrderId; }
        public void setPaypalOrderId(String paypalOrderId) { this.paypalOrderId = paypalOrderId; }
        public String getNegotiationId() { return negotiationId; }
        public void setNegotiationId(String negotiationId) { this.negotiationId = negotiationId; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    @PostMapping("/paypal/cancel-order")
    public ResponseEntity<ApiResponse<PaymentRecord>> cancelOrder(@RequestBody CancelOrFailOrderRequest request) {
        PaymentRecord record = payPalService.cancelOrder(
                request.getPaypalOrderId(),
                request.getNegotiationId(),
                request.getReason()
        );
        return ResponseEntity.ok(ApiResponse.ok("PayPal checkout cancelled cleanly", record));
    }

    @PostMapping("/paypal/fail-order")
    public ResponseEntity<ApiResponse<PaymentRecord>> recordFailure(@RequestBody CancelOrFailOrderRequest request) {
        PaymentRecord record = payPalService.recordPaymentFailure(
                request.getPaypalOrderId(),
                request.getNegotiationId(),
                request.getReason()
        );
        return ResponseEntity.ok(ApiResponse.ok("PayPal payment failure recorded", record));
    }

    @GetMapping("/paypal/orders/{paypalOrderId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPayPalOrderDetails(@PathVariable String paypalOrderId) {
        Map<String, Object> details = payPalService.retrieveOrderDetails(paypalOrderId);
        return ResponseEntity.ok(ApiResponse.ok("PayPal order details retrieved", details));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PaymentRecord>> getPaymentRecord(@PathVariable String id) {
        PaymentRecord record = payPalService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.ok("Payment record retrieved", record));
    }

    @GetMapping("/negotiation/{negotiationId}")
    public ResponseEntity<ApiResponse<PaymentRecord>> getPaymentByNegotiation(@PathVariable String negotiationId) {
        PaymentRecord record = payPalService.getPaymentById(negotiationId);
        return ResponseEntity.ok(ApiResponse.ok("Payment record retrieved for negotiation", record));
    }

    @PostMapping("/paypal/webhook")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePayPalWebhook(
            @RequestHeader Map<String, String> headers,
            @RequestBody String body) {
        boolean verified = payPalService.verifyWebhookSignature(headers, body);
        return ResponseEntity.ok(ApiResponse.ok("Webhook received", Map.of(
                "verified", verified,
                "status", "PROCESSED"
        )));
    }

    @GetMapping("/execution-status/{negotiationId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutionStatus(
            @PathVariable String negotiationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String buyerId = resolveBuyerId(userDetails);
        Map<String, Object> status = paymentExecutionEngine.getExecutionStatus(negotiationId, buyerId);
        return ResponseEntity.ok(ApiResponse.ok("Payment execution status retrieved", status));
    }

    @PostMapping("/delegated/execute")
    public ResponseEntity<ApiResponse<Order>> executeDelegatedPayment(
            @RequestBody CaptureOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        String buyerId = resolveBuyerId(userDetails);
        Order order = paymentExecutionEngine.executeDelegatedPayment(
                request.getNegotiationId(),
                request.getDealToken(),
                buyerId,
                request.getShippingDetails()
        );
        return ResponseEntity.ok(ApiResponse.ok("Delegated payment settled and order locked", order));
    }

    @GetMapping("/audit/my-logs")
    public ResponseEntity<ApiResponse<List<PaymentAuditEntry>>> getMyAuditLogs(
            @AuthenticationPrincipal UserDetails userDetails) {
        String buyerId = resolveBuyerId(userDetails);
        List<PaymentAuditEntry> logs = paymentAuditService != null && buyerId != null
                ? paymentAuditService.getAuditHistoryForBuyer(buyerId)
                : List.of();
        return ResponseEntity.ok(ApiResponse.ok("Buyer payment & mandate audit logs retrieved", logs));
    }

    private String resolveBuyerId(UserDetails userDetails) {
        if (userDetails == null || userService == null) {
            return null;
        }
        User u = userService.getUserByEmail(userDetails.getUsername());
        return u != null ? u.getId() : null;
    }
}
