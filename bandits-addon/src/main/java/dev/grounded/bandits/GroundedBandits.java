package dev.grounded.bandits;

import dev.grounded.bandits.entity.Bandit;
import dev.grounded.bandits.entity.Survivor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Hostile human bandits, plus neutral survivors who look just like them. Both kinds share the {@link Bandit} class (so vanilla's "alert others" makes a melee
 * bandit and a marksman call each other for help); the entity type decides the role and loadout.
 * Natural spawns come from the biome modifier in data/grounded_bandits/neoforge/biome_modifier.
 */
@Mod(GroundedBandits.MOD_ID)
public final class GroundedBandits {

    public static final String MOD_ID = "grounded_bandits";

    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Bandit>> BANDIT = ENTITIES.register("bandit",
            id -> EntityType.Builder.<Bandit>of(Bandit::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    public static final DeferredHolder<EntityType<?>, EntityType<Bandit>> BANDIT_MARKSMAN = ENTITIES.register("bandit_marksman",
            id -> EntityType.Builder.<Bandit>of(Bandit::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    /** Neutral wanderer: same look as a bandit, leaves you alone unless attacked, fights bandits and zombies. */
    public static final DeferredHolder<EntityType<?>, EntityType<Survivor>> SURVIVOR = ENTITIES.register("survivor",
            id -> EntityType.Builder.<Survivor>of(Survivor::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    public GroundedBandits(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        modEventBus.addListener(GroundedBandits::onAttributes);
        modEventBus.addListener(GroundedBandits::onSpawnPlacements);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BANDIT.get(), Bandit.createAttributes().build());
        event.put(BANDIT_MARKSMAN.get(), Bandit.createAttributes().build());
        event.put(SURVIVOR.get(), Survivor.createAttributes().build());
    }

    private static void onSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (EntityType<Bandit> type : java.util.List.of(BANDIT.get(), BANDIT_MARKSMAN.get())) {
            event.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (t, level, reason, pos, random) -> level.getDifficulty() != Difficulty.PEACEFUL
                            && Mob.checkMobSpawnRules(t, level, reason, pos, random)
                            // Natural bandits roam the surface only: no cave bandits.
                            && (reason != EntitySpawnReason.NATURAL || level.canSeeSky(pos)),
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(SURVIVOR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (t, level, reason, pos, random) -> level.getDifficulty() != Difficulty.PEACEFUL
                        && Mob.checkMobSpawnRules(t, level, reason, pos, random)
                        && (reason != EntitySpawnReason.NATURAL || level.canSeeSky(pos)),
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }
}
