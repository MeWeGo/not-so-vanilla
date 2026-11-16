package dev.grounded.inventory.client.compat;

import com.thirstwastaken2.api.ThirstApi;
import net.minecraft.world.entity.player.Player;

/** Only class-loaded when Thirst Was Taken 2 is present. Uses its public ThirstApi. */
public final class ThirstCompat {

    /** @return {thirst, quenched, max} or null when thirst is disabled for this player */
    public static int[] read(Player player) {
        if (!ThirstApi.isEnabled(player)) return null;
        return new int[]{ThirstApi.thirst(player), ThirstApi.quenched(player), ThirstApi.maxThirst()};
    }

    private ThirstCompat() {
    }
}
