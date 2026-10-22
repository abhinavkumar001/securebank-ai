package com.securebank.fraud;

/**
 * What the model looks at for one transfer.
 * @param amountRatio      amount / this account's average transfer (1.0 when there's no history)
 * @param newRecipient     never sent money to this account before
 * @param oddHour          sent between midnight and 5 am
 * @param recentTransfers  transfers from this account in the last 10 minutes
 * @param balanceShare     amount / available balance (0..1)
 */
public record FraudFeatures(double amountRatio, boolean newRecipient, boolean oddHour,
                            int recentTransfers, double balanceShare) {

    public static final String[] NAMES = {
            "Amount vs usual (log)", "New recipient", "Odd hour", "Rapid repeats", "Share of balance"};

    /** Turns the features into numbers on similar scales, which gradient descent needs. */
    public double[] toVector() {
        return new double[]{
                Math.max(-3, Math.min(5, Math.log(Math.max(amountRatio, 0.01)))),
                newRecipient ? 1 : 0,
                oddHour ? 1 : 0,
                Math.min(recentTransfers, 10) / 5.0,
                Math.max(0, Math.min(1, balanceShare))
        };
    }
}
