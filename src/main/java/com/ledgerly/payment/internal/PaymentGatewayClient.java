package com.ledgerly.payment.internal;

import com.ledgerly.payment.Payment;

/**
 * Abstraction over the external payment gateway. The default implementation
 * below simply approves every transaction; replace it with a real HTTP client
 * when integrating with an actual PSP.
 */
public interface PaymentGatewayClient {

    /**
     * Attempts to charge the customer for the given payment.
     *
     * @return {@code true} if the gateway accepted the charge, {@code false}
     *         otherwise.
     */
    boolean process(Payment payment);

    /**
     * Default stub gateway implementation that approves every payment.
     */
    @org.springframework.stereotype.Component
    public class DefaultPaymentGatewayClient implements PaymentGatewayClient {

        @Override
        public boolean process(Payment payment) {
            return true;
        }
    }
}