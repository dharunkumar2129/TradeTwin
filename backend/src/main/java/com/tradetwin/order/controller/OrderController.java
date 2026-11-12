package com.tradetwin.order.controller;

import com.tradetwin.common.api.ApiResponse;
import com.tradetwin.common.exception.UnauthorizedException;
import com.tradetwin.order.dto.BuyerDashboardSummaryDto;
import com.tradetwin.order.dto.UpdateOrderStatusRequest;
import com.tradetwin.order.model.Order;
import com.tradetwin.order.service.OrderService;
import com.tradetwin.user.model.User;
import com.tradetwin.user.model.UserRole;
import com.tradetwin.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/orders", "/api/v1/orders"})
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    public OrderController(OrderService orderService, UserService userService) {
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Order>> getOrderById(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Order order = orderService.getOrderById(id);
        if (userDetails != null) {
            User user = userService.getUserByEmail(userDetails.getUsername());
            if (user != null && user.getRole() != UserRole.ROLE_ADMIN) {
                if (!user.getId().equals(order.getBuyerId()) && !user.getId().equals(order.getSellerId())) {
                    throw new UnauthorizedException("You do not have permission to view this order.");
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @GetMapping("/my-orders")
    public ResponseEntity<ApiResponse<List<Order>>> getMyOrders(@AuthenticationPrincipal UserDetails userDetails) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        List<Order> orders = orderService.getOrdersByBuyer(buyer.getId());
        return ResponseEntity.ok(ApiResponse.ok("Buyer orders retrieved", orders));
    }

    @GetMapping("/buyer/summary")
    public ResponseEntity<ApiResponse<BuyerDashboardSummaryDto>> getBuyerDashboardSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        User buyer = userService.getUserByEmail(userDetails.getUsername());
        BuyerDashboardSummaryDto summary = orderService.getBuyerDashboardSummary(buyer.getId());
        return ResponseEntity.ok(ApiResponse.ok("Buyer dashboard summary retrieved", summary));
    }

    @GetMapping("/seller/my-orders")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Order>>> getSellerOrders(@AuthenticationPrincipal UserDetails userDetails) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        List<Order> orders = orderService.getOrdersBySeller(seller.getId());
        return ResponseEntity.ok(ApiResponse.ok("Seller incoming orders retrieved", orders));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(
            @PathVariable String id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        User seller = userService.getUserByEmail(userDetails.getUsername());
        Order updated = orderService.updateOrderStatus(id, seller.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Order status updated successfully", updated));
    }
}
