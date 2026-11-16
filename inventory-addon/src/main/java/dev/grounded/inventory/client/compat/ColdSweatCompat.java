package dev.grounded.inventory.client.compat;

import com.momosoftworks.coldsweat.api.util.Temperature;
import net.minecraft.world.entity.player.Player;

/**
 * Only class-loaded when Cold Sweat is present. Body temperature is Cold Sweat's own scale:
 * 0 is neutral, beyond ±100 the player starts taking damage (max ±150).
 */
public final class ColdSweatCompat {

    public static double bodyTemperature(Player player) {
        return Temperature.get(player, Temperature.Trait.BODY);
    }

    private ColdSweatCompat() {
    }
}
