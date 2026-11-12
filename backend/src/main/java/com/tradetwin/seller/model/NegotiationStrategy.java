package com.tradetwin.seller.model;

public enum NegotiationStrategy {
    AGGRESSIVE,   // Reluctant concessions; defends list price
    BALANCED,     // Measured step-down towards mid-point
    CONSERVATIVE, // Quick concessions towards target price to close volume
    COOPERATIVE   // Emphasizes non-monetary sweeteners (warranty, shipping)
}
