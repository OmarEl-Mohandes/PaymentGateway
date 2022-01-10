package com.org.bank;

import com.org.payments.MerchantPayment;
import com.org.payments.PaymentStatus;

public class BankSimulator {
    public PaymentStatus makePayment(MerchantPayment payment) {
        if (payment.getAmount() == 1) {
            return PaymentStatus.Declined;
        } else if (payment.getAmount() == 2) {
            return PaymentStatus.InsufficientFunds;
        } else if (payment.getAmount() == 24) {
            // If it's already marked as pending, will simulate the second request as Accepted.
            if (PaymentStatus.Pending.name().equals(payment.getStatus())) {
                return PaymentStatus.Accepted;
            } else {
                return PaymentStatus.Pending;
            }
        } else {
            return PaymentStatus.Accepted;
        }
    }
}
