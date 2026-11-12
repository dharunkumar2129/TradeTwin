package com.tradetwin.catalog.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogSearchRequest {

    private String query;
    private String productType;
    private List<String> brandPreferences = new ArrayList<>();
    private Map<String, String> requiredSpecifications = new HashMap<>();
    private List<String> optionalPreferences = new ArrayList<>();
    private BigDecimal maxBudget;
    private int quantity = 1;
    private Integer minWarrantyMonths;
    private Integer maxDeliveryDays;
    private List<String> preferredConditions = new ArrayList<>();
    private String currency = "USD";

    public CatalogSearchRequest() {}

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String query;
        private String productType;
        private List<String> brandPreferences = new ArrayList<>();
        private Map<String, String> requiredSpecifications = new HashMap<>();
        private List<String> optionalPreferences = new ArrayList<>();
        private BigDecimal maxBudget;
        private int quantity = 1;
        private Integer minWarrantyMonths;
        private Integer maxDeliveryDays;
        private List<String> preferredConditions = new ArrayList<>();
        private String currency = "USD";

        public Builder query(String query) { this.query = query; return this; }
        public Builder productType(String productType) { this.productType = productType; return this; }
        public Builder brandPreferences(List<String> brandPreferences) { this.brandPreferences = brandPreferences; return this; }
        public Builder requiredSpecifications(Map<String, String> requiredSpecifications) { this.requiredSpecifications = requiredSpecifications; return this; }
        public Builder optionalPreferences(List<String> optionalPreferences) { this.optionalPreferences = optionalPreferences; return this; }
        public Builder maxBudget(BigDecimal maxBudget) { this.maxBudget = maxBudget; return this; }
        public Builder quantity(int quantity) { this.quantity = quantity; return this; }
        public Builder minWarrantyMonths(Integer minWarrantyMonths) { this.minWarrantyMonths = minWarrantyMonths; return this; }
        public Builder maxDeliveryDays(Integer maxDeliveryDays) { this.maxDeliveryDays = maxDeliveryDays; return this; }
        public Builder preferredConditions(List<String> preferredConditions) { this.preferredConditions = preferredConditions; return this; }
        public Builder currency(String currency) { this.currency = currency; return this; }

        public CatalogSearchRequest build() {
            CatalogSearchRequest req = new CatalogSearchRequest();
            req.setQuery(this.query);
            req.setProductType(this.productType);
            req.setBrandPreferences(this.brandPreferences);
            req.setRequiredSpecifications(this.requiredSpecifications);
            req.setOptionalPreferences(this.optionalPreferences);
            req.setMaxBudget(this.maxBudget);
            req.setQuantity(this.quantity);
            req.setMinWarrantyMonths(this.minWarrantyMonths);
            req.setMaxDeliveryDays(this.maxDeliveryDays);
            req.setPreferredConditions(this.preferredConditions);
            req.setCurrency(this.currency);
            return req;
        }
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public List<String> getBrandPreferences() { return brandPreferences; }
    public void setBrandPreferences(List<String> brandPreferences) { this.brandPreferences = brandPreferences; }
    public Map<String, String> getRequiredSpecifications() { return requiredSpecifications; }
    public void setRequiredSpecifications(Map<String, String> requiredSpecifications) { this.requiredSpecifications = requiredSpecifications; }
    public List<String> getOptionalPreferences() { return optionalPreferences; }
    public void setOptionalPreferences(List<String> optionalPreferences) { this.optionalPreferences = optionalPreferences; }
    public BigDecimal getMaxBudget() { return maxBudget; }
    public void setMaxBudget(BigDecimal maxBudget) { this.maxBudget = maxBudget; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity > 0 ? quantity : 1; }
    public Integer getMinWarrantyMonths() { return minWarrantyMonths; }
    public void setMinWarrantyMonths(Integer minWarrantyMonths) { this.minWarrantyMonths = minWarrantyMonths; }
    public Integer getMaxDeliveryDays() { return maxDeliveryDays; }
    public void setMaxDeliveryDays(Integer maxDeliveryDays) { this.maxDeliveryDays = maxDeliveryDays; }
    public List<String> getPreferredConditions() { return preferredConditions; }
    public void setPreferredConditions(List<String> preferredConditions) { this.preferredConditions = preferredConditions; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}
