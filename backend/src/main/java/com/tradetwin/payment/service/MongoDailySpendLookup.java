package com.tradetwin.payment.service;

import com.tradetwin.mandate.service.DailySpendLookup;
import com.tradetwin.payment.model.PaymentRecord;
import com.tradetwin.payment.model.PaymentStatus;
import com.tradetwin.payment.repository.PaymentRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class MongoDailySpendLookup implements DailySpendLookup {

    private final PaymentRepository paymentRepository;

    public MongoDailySpendLookup(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public BigDecimal getCompletedSpendToday(String buyerId, String currency) {
        if (!StringUtils.hasText(buyerId)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        List<PaymentRecord> completed = paymentRepository.findByBuyerIdAndStatus(buyerId, PaymentStatus.COMPLETED);
        if (completed == null || completed.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Instant startOfDayUtc = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentRecord r : completed) {
            Instant txTime = r.getPaidAt() != null ? r.getPaidAt() : r.getUpdatedAt();
            if (txTime == null || txTime.isBefore(startOfDayUtc)) {
                continue;
            }
            if (StringUtils.hasText(currency) && StringUtils.hasText(r.getCurrency())
                    && !currency.trim().equalsIgnoreCase(r.getCurrency().trim())) {
                continue;
            }
            if (r.getAmount() != null) {
                total = total.add(r.getAmount());
            }
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
