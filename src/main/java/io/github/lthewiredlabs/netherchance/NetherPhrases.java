package io.github.lthewiredlabs.netherchance;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.random.RandomGenerator;

final class NetherPhrases {
    static final List<String> OPENING = List.of(
            "The veil tears open. The Nether awaits.",
            "Thunder breaks the silence. The Nether gates are open.",
            "Something stirs beyond the storm. Passage to the Nether is granted.",
            "The skies darken as the gates awaken. Enter… while you can.");
    static final List<String> CLOSING = List.of(
            "The skies clear. The gates seal. Those within must wait.",
            "The passage is severed. The Nether keeps those who remain.",
            "The storm retreats, and the gates fall silent. No entry. No escape.",
            "The veil closes once more. Those beyond it must await their fate.");

    private NetherPhrases() {
    }

    static List<String> prepare(List<String> configured, List<String> fallback) {
        LinkedHashSet<String> phrases = new LinkedHashSet<>();
        for (String phrase : configured) {
            if (phrase != null && !phrase.isBlank()) {
                phrases.add(phrase.strip());
            }
        }
        return phrases.isEmpty() ? List.copyOf(fallback) : List.copyOf(phrases);
    }

    static String choose(List<String> phrases, String previous, RandomGenerator random) {
        if (phrases.isEmpty()) {
            throw new IllegalArgumentException("At least one phrase is required");
        }
        List<String> candidates = phrases.stream().filter(phrase -> !phrase.equals(previous)).toList();
        // A single configured phrase is valid, even though repetition is unavoidable.
        if (candidates.isEmpty()) {
            candidates = phrases;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }
}
