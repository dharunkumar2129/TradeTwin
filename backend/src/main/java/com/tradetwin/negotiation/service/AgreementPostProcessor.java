package com.tradetwin.negotiation.service;

import com.tradetwin.negotiation.model.Negotiation;

/**
 * Decouples NegotiationOrchestrator from payment execution modules so post-agreement
 * Spend Mandate validation and Supervised PayPal order preparation happen without circular dependencies.
 */
public interface AgreementPostProcessor {
    void onAgreementSealed(Negotiation negotiation);
}
