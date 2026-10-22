package com.securebank.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formats rupees with Indian digit grouping (₹2,00,000 rather than ₹200,000).
 * java.text.DecimalFormat only supports equal-size groups, so this is done by hand.
 */
public final class Rupees {

    private Rupees() {
    }

    public static String format(BigDecimal amount) {
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        boolean negative = value.signum() < 0;
        String plain = value.abs().toPlainString();
        String whole = plain.substring(0, plain.indexOf('.'));
        String paise = plain.substring(plain.indexOf('.') + 1);

        String grouped;
        if (whole.length() <= 3) {
            grouped = whole;
        } else {
            String lastThree = whole.substring(whole.length() - 3);
            String rest = whole.substring(0, whole.length() - 3);
            StringBuilder sb = new StringBuilder();
            while (rest.length() > 2) {
                sb.insert(0, "," + rest.substring(rest.length() - 2));
                rest = rest.substring(0, rest.length() - 2);
            }
            grouped = rest + sb + "," + lastThree;
        }
        return (negative ? "-" : "") + "₹" + grouped + (paise.equals("00") ? "" : "." + paise);
    }
}
