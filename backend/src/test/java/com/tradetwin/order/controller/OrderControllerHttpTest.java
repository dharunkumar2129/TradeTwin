package com.tradetwin.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradetwin.order.dto.BuyerDashboardSummaryDto;
import com.tradetwin.order.dto.UpdateOrderStatusRequest;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.model.OrderStatus;
import com.tradetwin.order.service.OrderService;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerHttpTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UsernamePasswordAuthenticationToken authPrincipal;

    static class StubUserService extends UserService {
        public StubUserService() {
            super(null);
        }

        @Override
        public User getUserByEmail(String email) {
            return User.builder()
                    .id("u-1")
                    .email(email)
                    .role(UserRole.ROLE_SELLER)
                    .build();
        }
    }

    static class StubOrderService extends OrderService {
        public StubOrderService() {
            super(null, null, null, null, null);
        }

        @Override
        public List<Order> getOrdersByBuyer(String buyerId) {
            return List.of(Order.builder()
                    .id("ord-1")
                    .orderNumber("TT-101")
                    .buyerId(buyerId)
                    .productTitle("Studio Monitors")
                    .finalAmount(new BigDecimal("350.00"))
                    .orderStatus(OrderStatus.CONFIRMED)
                    .build());
        }

        @Override
        public BuyerDashboardSummaryDto getBuyerDashboardSummary(String buyerId) {
            return new BuyerDashboardSummaryDto(
                    2,
                    new BigDecimal("500.00"),
                    1,
                    1,
                    1,
                    1,
                    "USD",
                    List.of()
            );
        }

        @Override
        public List<Order> getOrdersBySeller(String sellerId) {
            return List.of(Order.builder()
                    .id("ord-2")
                    .orderNumber("TT-202")
                    .sellerId(sellerId)
                    .productTitle("Pro Headphones")
                    .finalAmount(new BigDecimal("220.00"))
                    .orderStatus(OrderStatus.PROCESSING)
                    .build());
        }

        @Override
        public Order updateOrderStatus(String orderId, String sellerUserId, UpdateOrderStatusRequest request) {
            return Order.builder()
                    .id(orderId)
                    .orderNumber("TT-202")
                    .sellerId(sellerUserId)
                    .orderStatus(request.getStatus())
                    .carrier(request.getCarrier())
                    .trackingNumber(request.getTrackingNumber())
                    .build();
        }
    }

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(new StubOrderService(), new StubUserService());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        UserDetails principal = org.springframework.security.core.userdetails.User.builder()
                .username("seller@tradetwin.ai")
                .password("pass")
                .roles("SELLER")
                .build();
        authPrincipal = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("GET /api/v1/orders/buyer/summary returns real computed buyer stats")
    void testGetBuyerSummary() throws Exception {
        mockMvc.perform(get("/api/v1/orders/buyer/summary").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(2))
                .andExpect(jsonPath("$.data.totalSpent").value(500.00));
    }

    @Test
    @DisplayName("GET /api/v1/orders/seller/my-orders returns incoming orders")
    void testGetSellerOrders() throws Exception {
        mockMvc.perform(get("/api/v1/orders/seller/my-orders").principal(authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].orderNumber").value("TT-202"))
                .andExpect(jsonPath("$.data[0].orderStatus").value("PROCESSING"));
    }

    @Test
    @DisplayName("PUT /api/v1/orders/{id}/status updates fulfilment status to SHIPPED with tracking")
    void testUpdateOrderStatusToShipped() throws Exception {
        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest(OrderStatus.SHIPPED, "FedEx", "FX-98765", "Shipped cleanly");

        mockMvc.perform(put("/api/v1/orders/ord-2/status")
                        .principal(authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.data.carrier").value("FedEx"))
                .andExpect(jsonPath("$.data.trackingNumber").value("FX-98765"));
    }
}
