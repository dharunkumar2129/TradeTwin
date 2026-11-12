package com.tradetwin.catalog.dto;

import java.util.ArrayList;
import java.util.List;

public class CatalogSearchResponse {

    private List<SearchResultItemDto> results = new ArrayList<>();
    private int totalMatched;
    private String searchSummary;

    public CatalogSearchResponse() {}

    public CatalogSearchResponse(List<SearchResultItemDto> results, int totalMatched, String searchSummary) {
        this.results = results != null ? results : new ArrayList<>();
        this.totalMatched = totalMatched;
        this.searchSummary = searchSummary;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private List<SearchResultItemDto> results = new ArrayList<>();
        private int totalMatched;
        private String searchSummary;

        public Builder results(List<SearchResultItemDto> results) { this.results = results; return this; }
        public Builder totalMatched(int totalMatched) { this.totalMatched = totalMatched; return this; }
        public Builder searchSummary(String searchSummary) { this.searchSummary = searchSummary; return this; }

        public CatalogSearchResponse build() {
            return new CatalogSearchResponse(results, totalMatched, searchSummary);
        }
    }

    public List<SearchResultItemDto> getResults() { return results; }
    public void setResults(List<SearchResultItemDto> results) { this.results = results; }
    public int getTotalMatched() { return totalMatched; }
    public void setTotalMatched(int totalMatched) { this.totalMatched = totalMatched; }
    public String getSearchSummary() { return searchSummary; }
    public void setSearchSummary(String searchSummary) { this.searchSummary = searchSummary; }
}
