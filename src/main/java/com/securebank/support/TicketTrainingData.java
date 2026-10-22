package com.securebank.support;

import java.util.List;
import java.util.Map;

/** Labelled example messages the Naive Bayes classifier learns from. */
final class TicketTrainingData {

    private TicketTrainingData() {
    }

    static final Map<TicketCategory, List<String>> EXAMPLES = Map.of(
            TicketCategory.FAILED_TRANSFER, List.of(
                    "my transfer is stuck and has not reached the recipient",
                    "money was debited but the payment failed",
                    "transfer is showing held for review for hours",
                    "why is my transaction pending, I need the funds sent today",
                    "the amount did not reach my friend's account",
                    "payment failed but balance was deducted",
                    "my neft transfer has not been credited yet",
                    "transaction held, please release the payment",
                    "sent money to wrong account number, need it reversed"),
            TicketCategory.FRAUD_REPORT, List.of(
                    "i do not recognise this withdrawal on my account",
                    "someone made an unauthorized transaction from my account",
                    "i think my account has been hacked",
                    "got a call asking for my otp and now money is missing",
                    "fraud transaction please block my account immediately",
                    "there is a transfer i never made",
                    "scam call and money stolen from savings",
                    "suspicious activity on my account"),
            TicketCategory.CARD_ISSUE, List.of(
                    "my debit card is not working at the atm",
                    "card was declined at the store",
                    "lost my debit card please block it",
                    "card got stuck in the atm machine",
                    "need a replacement card, mine is damaged",
                    "how do I unfreeze my card",
                    "card payment declined online"),
            TicketCategory.ACCOUNT_ACCESS, List.of(
                    "i cannot log in to my account",
                    "forgot my password and the reset is not working",
                    "account is locked after wrong password attempts",
                    "login page shows an error",
                    "unable to sign in to net banking",
                    "my account is frozen and I cannot access it"),
            TicketCategory.PROFILE_UPDATE, List.of(
                    "how do I change my registered email address",
                    "need to update my mobile number",
                    "please change my address in the records",
                    "update my name spelling on the account",
                    "want to update kyc documents",
                    "change nominee details"),
            TicketCategory.GENERAL, List.of(
                    "what is the interest rate on savings account",
                    "how do I open a fixed deposit",
                    "what are the branch timings",
                    "where can I find my ifsc code",
                    "how to download account statement",
                    "what charges apply on a current account",
                    "can I open a second account")
    );
}
