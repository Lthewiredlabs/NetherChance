package io.github.lthewiredlabs.netherchance;

import java.time.LocalDate;
import java.time.LocalTime;

final class NetherChancePolicy {
    private NetherChancePolicy() {
    }

    static boolean shouldToggle(int roll, int chancePercent) {
        if (roll < 1 || roll > 100) {
            throw new IllegalArgumentException("roll must be between 1 and 100");
        }
        if (chancePercent < 0 || chancePercent > 100) {
            throw new IllegalArgumentException("chancePercent must be between 0 and 100");
        }
        return roll <= chancePercent;
    }

    static boolean dailyRollIsDue(
            LocalDate today,
            LocalTime currentTime,
            LocalDate lastDailyRoll,
            LocalTime scheduledTime) {
        return !today.equals(lastDailyRoll) && !currentTime.isBefore(scheduledTime);
    }

    static boolean shouldBlockTeleport(boolean netherOpen, boolean startsInNether, boolean endsInNether) {
        return !netherOpen && startsInNether != endsInNether;
    }
}
