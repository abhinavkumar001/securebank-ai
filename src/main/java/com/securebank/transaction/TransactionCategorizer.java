package com.securebank.transaction;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Assigns a spending category from the transaction note using keyword rules.
 * Simple and explainable; the spending insights on the home screen are built on it.
 */
@Component
public class TransactionCategorizer {

    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    static {
        RULES.put("Housing", List.of("rent", "landlord", "maintenance", "society", "property", "lease"));
        RULES.put("Groceries", List.of("grocery", "groceries", "bigbasket", "blinkit", "zepto", "dmart", "vegetable", "kirana"));
        RULES.put("Food", List.of("dinner", "lunch", "breakfast", "restaurant", "swiggy", "zomato", "cafe", "food", "pizza"));
        RULES.put("Bills", List.of("bill", "electricity", "water", "gas", "recharge", "broadband", "wifi", "postpaid", "dth"));
        RULES.put("Travel", List.of("uber", "ola", "flight", "train", "irctc", "bus", "taxi", "fuel", "petrol", "metro"));
        RULES.put("Shopping", List.of("amazon", "flipkart", "myntra", "shopping", "clothes", "electronics"));
        RULES.put("Health", List.of("hospital", "doctor", "pharmacy", "medicine", "clinic", "insurance"));
        RULES.put("Education", List.of("fees", "school", "college", "tuition", "course", "books"));
        RULES.put("Investments", List.of("sip", "mutual fund", "stocks", "fd", "deposit", "ppf"));
        RULES.put("Family", List.of("mom", "dad", "mother", "father", "family", "brother", "sister", "wife", "husband"));
    }

    public String categorize(TransactionType type, String description) {
        if (type == TransactionType.DEPOSIT) {
            return "Income";
        }
        if (description != null && !description.isBlank()) {
            String text = description.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, List<String>> rule : RULES.entrySet()) {
                for (String keyword : rule.getValue()) {
                    if (text.contains(keyword)) {
                        return rule.getKey();
                    }
                }
            }
        }
        return type == TransactionType.WITHDRAWAL ? "Cash" : "Transfers";
    }
}
