package io.github.lthewiredlabs.netherchance;

import java.time.LocalDate;
import java.time.LocalTime;

public final class NetherChancePolicyTest {
    private NetherChancePolicyTest() {
    }

    public static void main(String[] args) {
        assert NetherChancePolicy.shouldToggle(1, 15);
        assert NetherChancePolicy.shouldToggle(15, 15);
        assert !NetherChancePolicy.shouldToggle(16, 15);
        assert !NetherChancePolicy.shouldToggle(100, 15);

        LocalDate today = LocalDate.of(2026, 9, 3);
        LocalDate yesterday = today.minusDays(1);
        LocalTime midnight = LocalTime.MIDNIGHT;

        assert NetherChancePolicy.dailyRollIsDue(today, midnight, yesterday, midnight);
        assert !NetherChancePolicy.dailyRollIsDue(today, LocalTime.NOON, today, midnight);
        assert !NetherChancePolicy.dailyRollIsDue(
                today,
                LocalTime.of(17, 59),
                yesterday,
                LocalTime.of(18, 0));
        assert NetherChancePolicy.dailyRollIsDue(
                today,
                LocalTime.of(18, 0),
                yesterday,
                LocalTime.of(18, 0));

        assert !NetherChancePolicy.shouldBlockTeleport(true, false, true);
        assert !NetherChancePolicy.shouldBlockTeleport(true, true, false);
        assert NetherChancePolicy.shouldBlockTeleport(false, false, true);
        assert NetherChancePolicy.shouldBlockTeleport(false, true, false);
        assert !NetherChancePolicy.shouldBlockTeleport(false, false, false);
        assert !NetherChancePolicy.shouldBlockTeleport(false, true, true);

        expectIllegalArgument(() -> NetherChancePolicy.shouldToggle(0, 15));
        expectIllegalArgument(() -> NetherChancePolicy.shouldToggle(101, 15));
        expectIllegalArgument(() -> NetherChancePolicy.shouldToggle(50, -1));
        expectIllegalArgument(() -> NetherChancePolicy.shouldToggle(50, 101));

        System.out.println("NetherChancePolicy tests passed");
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
    }
}
