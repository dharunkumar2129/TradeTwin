package com.tradetwin.order.dto;

import com.tradetwin.order.model.Order;

import java.math.BigDecimal;
import java.util.List;

public record BuyerDashboardSummaryDto(
        long totalOrders,
        BigDecimal totalSpent,
        long activeNegotiations,
        long agreedDeals,
        long pendingFulfilmentOrders,
        long shippedOrDeliveredOrders,
        String currency,
        List<Order> recentOrders
) {}
