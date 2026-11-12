package com.tradetwin.seller.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class SellerDashboardSummaryDto {

    private long totalProducts;
    private long activeListings;
    private int totalInventory;
    private int reservedInventory;
    private int lowStockCount;
    private long negotiationsReceived;
    private long acceptedDeals;
    private long totalOrders;
    private long pendingFulfilmentOrders;
    private long shippedOrders;
    private long deliveredOrders;
    private BigDecimal totalSalesVolume = BigDecimal.ZERO;
    private String currency = "USD";
    private List<SellerActivityItem> recentActivity = new ArrayList<>();

    public static class SellerActivityItem {
        private String id;
        private String type;
        private String title;
        private String description;
        private BigDecimal amount;
        private String currency;
        private Instant timestamp;

        public SellerActivityItem() {}

        public SellerActivityItem(String id, String type, String title, String description, BigDecimal amount, String currency, Instant timestamp) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.description = description;
            this.amount = amount;
            this.currency = currency;
            this.timestamp = timestamp;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    }

    public SellerDashboardSummaryDto() {}

    public SellerDashboardSummaryDto(long totalProducts, long activeListings, int totalInventory,
                                     int reservedInventory, int lowStockCount, long negotiationsReceived,
                                     long acceptedDeals, long totalOrders, BigDecimal totalSalesVolume,
                                     String currency, List<SellerActivityItem> recentActivity) {
        this.totalProducts = totalProducts;
        this.activeListings = activeListings;
        this.totalInventory = totalInventory;
        this.reservedInventory = reservedInventory;
        this.lowStockCount = lowStockCount;
        this.negotiationsReceived = negotiationsReceived;
        this.acceptedDeals = acceptedDeals;
        this.totalOrders = totalOrders;
        this.totalSalesVolume = totalSalesVolume != null ? totalSalesVolume : BigDecimal.ZERO;
        this.currency = currency != null ? currency : "USD";
        this.recentActivity = recentActivity != null ? recentActivity : new ArrayList<>();
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private long totalProducts;
        private long activeListings;
        private int totalInventory;
        private int reservedInventory;
        private int lowStockCount;
        private long negotiationsReceived;
        private long acceptedDeals;
        private long totalOrders;
        private long pendingFulfilmentOrders;
        private long shippedOrders;
        private long deliveredOrders;
        private BigDecimal totalSalesVolume = BigDecimal.ZERO;
        private String currency = "USD";
        private List<SellerActivityItem> recentActivity = new ArrayList<>();

        public Builder totalProducts(long totalProducts) { this.totalProducts = totalProducts; return this; }
        public Builder activeListings(long activeListings) { this.activeListings = activeListings; return this; }
        public Builder totalInventory(int totalInventory) { this.totalInventory = totalInventory; return this; }
        public Builder reservedInventory(int reservedInventory) { this.reservedInventory = reservedInventory; return this; }
        public Builder lowStockCount(int lowStockCount) { this.lowStockCount = lowStockCount; return this; }
        public Builder negotiationsReceived(long negotiationsReceived) { this.negotiationsReceived = negotiationsReceived; return this; }
        public Builder acceptedDeals(long acceptedDeals) { this.acceptedDeals = acceptedDeals; return this; }
        public Builder totalOrders(long totalOrders) { this.totalOrders = totalOrders; return this; }
        public Builder pendingFulfilmentOrders(long pendingFulfilmentOrders) { this.pendingFulfilmentOrders = pendingFulfilmentOrders; return this; }
        public Builder shippedOrders(long shippedOrders) { this.shippedOrders = shippedOrders; return this; }
        public Builder deliveredOrders(long deliveredOrders) { this.deliveredOrders = deliveredOrders; return this; }
        public Builder totalSalesVolume(BigDecimal totalSalesVolume) { this.totalSalesVolume = totalSalesVolume; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }
        public Builder recentActivity(List<SellerActivityItem> recentActivity) { this.recentActivity = recentActivity; return this; }

        public SellerDashboardSummaryDto build() {
            SellerDashboardSummaryDto dto = new SellerDashboardSummaryDto(totalProducts, activeListings, totalInventory, reservedInventory, lowStockCount, negotiationsReceived, acceptedDeals, totalOrders, totalSalesVolume, currency, recentActivity);
            dto.setPendingFulfilmentOrders(pendingFulfilmentOrders);
            dto.setShippedOrders(shippedOrders);
            dto.setDeliveredOrders(deliveredOrders);
            return dto;
        }
    }

    public long getTotalProducts() { return totalProducts; }
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
    public long getActiveListings() { return activeListings; }
    public void setActiveListings(long activeListings) { this.activeListings = activeListings; }
    public int getTotalInventory() { return totalInventory; }
    public void setTotalInventory(int totalInventory) { this.totalInventory = totalInventory; }
    public int getReservedInventory() { return reservedInventory; }
    public void setReservedInventory(int reservedInventory) { this.reservedInventory = reservedInventory; }
    public int getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(int lowStockCount) { this.lowStockCount = lowStockCount; }
    public long getNegotiationsReceived() { return negotiationsReceived; }
    public void setNegotiationsReceived(long negotiationsReceived) { this.negotiationsReceived = negotiationsReceived; }
    public long getAcceptedDeals() { return acceptedDeals; }
    public void setAcceptedDeals(long acceptedDeals) { this.acceptedDeals = acceptedDeals; }
    public long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(long totalOrders) { this.totalOrders = totalOrders; }
    public long getPendingFulfilmentOrders() { return pendingFulfilmentOrders; }
    public void setPendingFulfilmentOrders(long pendingFulfilmentOrders) { this.pendingFulfilmentOrders = pendingFulfilmentOrders; }
    public long getShippedOrders() { return shippedOrders; }
    public void setShippedOrders(long shippedOrders) { this.shippedOrders = shippedOrders; }
    public long getDeliveredOrders() { return deliveredOrders; }
    public void setDeliveredOrders(long deliveredOrders) { this.deliveredOrders = deliveredOrders; }
    public BigDecimal getTotalSalesVolume() { return totalSalesVolume; }
    public void setTotalSalesVolume(BigDecimal totalSalesVolume) { this.totalSalesVolume = totalSalesVolume; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<SellerActivityItem> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<SellerActivityItem> recentActivity) { this.recentActivity = recentActivity; }
}
