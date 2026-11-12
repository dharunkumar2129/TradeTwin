package com.tradetwin.negotiation.dto;

import jakarta.validation.constraints.NotBlank;

public record SelectDealRequest(
        @NotBlank(message = "Offer ID or Seller ID is required")
        String offerId
) {}
