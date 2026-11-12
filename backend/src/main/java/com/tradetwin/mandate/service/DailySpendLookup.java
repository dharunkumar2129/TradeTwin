package com.tradetwin.mandate.service;

import java.math.BigDecimal;

/**
 * Decouples SpendMandateService from payment module repositories so there are zero
 * circular package dependencies while supporting real-time daily spend enforcement.
 */
public interface DailySpendLookup {
    BigDecimal getCompletedSpendToday(String buyerId, String currency);
}
