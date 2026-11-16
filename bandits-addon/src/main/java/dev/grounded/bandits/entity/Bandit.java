package dev.grounded.bandits.entity;

import dev.grounded.bandits.GroundedBandits;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A human bandit. Grounded rules: normal human health (20), no stat inflation, weak armour, and it backs off
 * when badly hurt instead of fighting to the death. Bandits raid villagers (incl. MCA humans) and players.
 */
public class Bandit extends Monster implements RangedAttackMob, SkinnedHuman {

    public static final String[] SKINS = HumanCombat.SKINS;
    private static final EntityDataAccessor<Integer> DATA_SKIN = SynchedEntityData.defineId(Bandit.class, EntityDataSerializers.INT);

    private final RangedBowAttackGoal<Bandit> bowGoal = new RangedBowAttackGoal<>(this, 1.0, 30, 18.0F);
    private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.15, false);

    public Bandit(EntityType<? extends Bandit> type, Level level) {
        super(type, level);
        this.xpReward = 6;
        this.reassessWeaponGoal();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    public boolean isMarksman() {
        return this.getType() == GroundedBandits.BANDIT_MARKSMAN.get();
    }

    @Override
    public int getSkin() {
        return Math.floorMod(this.entityData.get(DATA_SKIN), SKINS.length);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SKIN, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RetreatWhenWoundedGoal(this));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        // Whoever hurts one bandit draws the whole group.
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        // Bandits prey on lone survivors too; survivors fight back.
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Survivor.class, true));
    }

    /** Bow in hand = keep distance and shoot; anything else = close in for melee (skeleton pattern). */
    public void reassessWeaponGoal() {
        if (this.level() == null || this.level().isClientSide()) {
            return;
        }
        this.goalSelector.removeGoal(this.meleeGoal);
        this.goalSelector.removeGoal(this.bowGoal);
        ItemStack held = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        if (held.getItem() instanceof BowItem) {
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
        this.entityData.set(DATA_SKIN, random.nextInt(SKINS.length));
        this.populateDefaultEquipmentSlots(random, difficulty);
        this.reassessWeaponGoal();
        return data;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        HumanCombat.equip(this, random, this.isMarksman());
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        HumanCombat.shoot(this, target, power);
    }

    // Bandits are quiet: no ambient chatter that would give them away. They sound human when hurt.
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
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_SKIN, input.getIntOr("Skin", 0));
        this.reassessWeaponGoal();
    }

    /** Below 35% health a bandit breaks off and runs from the player instead of fighting to the death. */
    static final class RetreatWhenWoundedGoal extends AvoidEntityGoal<Player> {
        private final Mob bandit;

        RetreatWhenWoundedGoal(Bandit bandit) {
            super(bandit, Player.class, 16.0F, 1.0, 1.3);
            this.bandit = bandit;
        }

        private boolean wounded() {
            return this.bandit.getHealth() < this.bandit.getMaxHealth() * 0.35F;
        }

        @Override
        public boolean canUse() {
            return this.wounded() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return this.wounded() && super.canContinueToUse();
        }
    }
}
