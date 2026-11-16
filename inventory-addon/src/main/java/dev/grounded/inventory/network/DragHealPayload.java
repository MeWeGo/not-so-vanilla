package dev.grounded.inventory.network;

import dev.grounded.inventory.GroundedInventory;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: "use the medical item on my cursor on this limb of myself".
 * The server validates everything again and drives Medical System's normal item-use path.
 */
public record DragHealPayload(String limbCode) implements CustomPacketPayload {

    public static final Type<DragHealPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(GroundedInventory.MOD_ID, "drag_heal"));
    public static final StreamCodec<ByteBuf, DragHealPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(64), DragHealPayload::limbCode,
            DragHealPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
