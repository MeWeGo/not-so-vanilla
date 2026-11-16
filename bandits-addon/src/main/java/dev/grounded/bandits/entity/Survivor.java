package dev.grounded.bandits.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A neutral survivor. Looks exactly like a bandit (same skins, same patchy gear), so you can't tell friend from foe
 * until it acts. It leaves players alone unless attacked, fights bandits and zombies on sight, backs off when badly
 * hurt, and will swap a little medicine for a proper meal once in a while.
 */
public class Survivor extends PathfinderMob implements RangedAttackMob, SkinnedHuman {

    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(Survivor.class, EntityDataSerializers.INT);
    /** Ticks until this survivor will trade again (half a vanilla day). */
    private static final int TRADE_COOLDOWN = 12000;

    private final RangedBowAttackGoal<Survivor> bowGoal = new RangedBowAttackGoal<>(this, 1.0, 30, 18.0F);
    private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.15, false);
    private int tradeCooldown;

    public Survivor(EntityType<? extends Survivor> type, Level level) {
        super(type, level);
        this.xpReward = 3;
        this.reassessWeaponGoal();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    public int getSkin() {
        return Math.floorMod(this.entityData.get(DATA_SKIN), HumanCombat.SKINS.length);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Below 35% health, run from whoever it is fighting.
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, LivingEntity.class,
                entity -> entity == this.getTarget() && this.getHealth() < this.getMaxHealth() * 0.35F,
                16.0F, 1.0, 1.3, entity -> true));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Neutral: only fights players who hurt it (or its companions).
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Bandit.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
    }

    public void reassessWeaponGoal() {
        if (this.level() == null || this.level().isClientSide()) {
            return;
        }
        this.goalSelector.removeGoal(this.meleeGoal);
        this.goalSelector.removeGoal(this.bowGoal);
        if (HumanCombat.holdsBow(this)) {
            this.bowGoal.setMinAttackInterval(this.level().getDifficulty() == Difficulty.HARD ? 25 : 40);
            this.goalSelector.addGoal(4, this.bowGoal);
        } else {
            this.goalSelector.addGoal(4, this.meleeGoal);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (!this.level().isClientSide()) {
            this.reassessWeaponGoal();
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                                  EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
        RandomSource random = level.getRandom();
        this.entityData.set(DATA_SKIN, random.nextInt(HumanCombat.SKINS.length));
        this.populateDefaultEquipmentSlots(random, difficulty);
        this.reassessWeaponGoal();
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        HumanCombat.equip(this, random, random.nextFloat() < 0.3F);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        HumanCombat.shoot(this, target, power);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.tradeCooldown > 0) {
            this.tradeCooldown--;
        }
    }

    /** Hand it a real meal (4+ hunger) and it hands back some medicine. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack offered = player.getItemInHand(hand);
        FoodProperties food = offered.get(DataComponents.FOOD);
        if (hand != InteractionHand.MAIN_HAND || this.getTarget() != null || food == null || food.nutrition() < 4) {
            return super.mobInteract(player, hand);
        }
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.tradeCooldown > 0) {
                player.sendOverlayMessage(Component.translatable("entity.grounded_bandits.survivor.nothing_left"));
                this.playSound(SoundEvents.VILLAGER_NO, 0.6F, 1.3F);
                return InteractionResult.CONSUME;
            }
            ItemStack medicine = this.rollMedicine();
            if (medicine.isEmpty()) {
                return InteractionResult.PASS;
            }
            offered.consume(1, player);
            this.spawnAtLocation(serverLevel, medicine);
            this.tradeCooldown = TRADE_COOLDOWN;
            player.sendOverlayMessage(Component.translatable("entity.grounded_bandits.survivor.traded"));
            this.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 0.9F);
        }
        return InteractionResult.SUCCESS;
    }

    private ItemStack rollMedicine() {
        float roll = this.getRandom().nextFloat();
        String id = roll < 0.5F ? "medsystem:bandage" : roll < 0.75F ? "medsystem:painkillers"
                : roll < 0.9F ? "medsystem:splint" : "medsystem:tourniquet";
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).map(ItemStack::new).orElse(ItemStack.EMPTY);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Skin", this.entityData.get(DATA_SKIN));
        output.putInt("TradeCooldown", this.tradeCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_SKIN, input.getIntOr("Skin", 0));
        this.tradeCooldown = input.getIntOr("TradeCooldown", 0);
        this.reassessWeaponGoal();
    }
}
