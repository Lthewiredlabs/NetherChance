package io.github.lthewiredlabs.netherchance;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

public final class NetherPhrasesTest {
    public static void main(String[] args) {
        for (List<String> pool : List.of(NetherPhrases.OPENING, NetherPhrases.CLOSING)) {
            Random random = new Random(120);
            HashSet<String> seen = new HashSet<>();
            String previous = null;
            for (int i = 0; i < 1000; i++) {
                String selected = NetherPhrases.choose(pool, previous, random);
                assert pool.contains(selected);
                assert !selected.equals(previous) : "Consecutive repeat: " + selected;
                seen.add(selected);
                previous = selected;
            }
            assert seen.size() == 4 : "Every approved phrase must be reachable";

            // The caller can reload the last phrase from state.yml after a restart.
            String restoredPrevious = pool.getFirst();
            for (int seed = 0; seed < 100; seed++) {
                assert !NetherPhrases.choose(pool, restoredPrevious, new Random(seed)).equals(restoredPrevious);
            }
        }

        assert NetherPhrases.prepare(List.of(), NetherPhrases.OPENING).equals(NetherPhrases.OPENING);
        assert NetherPhrases.prepare(List.of("", "  "), NetherPhrases.CLOSING).equals(NetherPhrases.CLOSING);
        List<String> cleaned = NetherPhrases.prepare(List.of(" One ", "One", "", "Two"), NetherPhrases.OPENING);
        assert cleaned.equals(List.of("One", "Two"));
        assert NetherPhrases.choose(cleaned, "One", new Random()).equals("Two");
        assert NetherPhrases.choose(List.of("Only"), "Only", new Random()).equals("Only");
        assert cleaned.contains(NetherPhrases.choose(cleaned, "Removed from config", new Random()));
        System.out.println("NetherPhrases tests passed");
    }
}
