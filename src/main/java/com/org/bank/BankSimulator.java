package com.org.bank;

import com.org.payments.MerchantPayment;
import com.org.payments.PaymentStatus;

public class BankSimulator {
    public PaymentStatus makePayment(MerchantPayment payment) {
        return PaymentStatus.Accepted;
    }
}
