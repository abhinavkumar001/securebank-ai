package com.securebank.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Lower-cases, splits on non-letters, drops stop words and applies a light stemmer. */
public final class TextTokenizer {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "is", "am", "are", "was", "were", "be", "been", "to", "of", "in", "on", "at",
            "for", "and", "or", "it", "this", "that", "i", "me", "my", "we", "you", "your", "our", "can",
            "do", "does", "did", "how", "what", "why", "when", "will", "would", "should", "please", "with",
            "from", "by", "as", "so", "if", "has", "have", "had", "there", "their", "they", "them", "its",
            "about", "into", "up", "out", "any", "some", "just", "get", "got", "hi", "hello", "dear");

    private TextTokenizer() {
    }

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        for (String raw : text.toLowerCase(Locale.ROOT).replace("'", "").split("[^a-z0-9]+")) {
            if (raw.length() < 2 || STOP_WORDS.contains(raw)) {
                continue;
            }
            tokens.add(stem(raw));
        }
        return tokens;
    }

    /** A tiny stemmer: "transfers", "transferred", "transferring" -> "transfer". */
    static String stem(String word) {
        if (word.length() > 5 && word.endsWith("ing")) {
            return undouble(word.substring(0, word.length() - 3));
        }
        if (word.length() > 4 && word.endsWith("ed")) {
            return undouble(word.substring(0, word.length() - 2));
        }
        if (word.length() > 4 && word.endsWith("ies")) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    private static String undouble(String w) {
        int n = w.length();
        if (n > 2 && w.charAt(n - 1) == w.charAt(n - 2)) {
            return w.substring(0, n - 1);
        }
        return w;
    }
}
