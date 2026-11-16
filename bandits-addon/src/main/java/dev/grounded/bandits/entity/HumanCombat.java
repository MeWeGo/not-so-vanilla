package dev.grounded.bandits.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Loadout and bow logic shared by bandits and survivors: same people, same gear, different intentions. */
public final class HumanCombat {

    /** Default player skins shipped with the game (textures/entity/player/wide/<name>.png). */
    public static final String[] SKINS = {"alex", "ari", "efe", "kai", "makena", "noor", "steve", "sunny", "zuri"};

    private HumanCombat() {
    }

    public static boolean holdsBow(Mob mob) {
        return mob.getItemInHand(ProjectileUtil.getWeaponHoldingHand(mob, item -> item instanceof BowItem)).getItem() instanceof BowItem;
    }

    /** A bow, or a stone/iron sword or axe, plus patchy leather armour (rarely a chainmail chest). */
    public static void equip(Mob mob, RandomSource random, boolean bow) {
        if (bow) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
        } else {
            Item[] weapons = {Items.STONE_SWORD, Items.STONE_SWORD, Items.IRON_SWORD, Items.STONE_AXE, Items.IRON_AXE};
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapons[random.nextInt(weapons.length)]));
        }
        float armourChance = bow ? 0.6F : 1.0F;
        if (random.nextFloat() < 0.25F * armourChance) mob.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        if (random.nextFloat() < 0.6F * armourChance) {
            mob.setItemSlot(EquipmentSlot.CHEST, new ItemStack(random.nextFloat() < 0.2F ? Items.CHAINMAIL_CHESTPLATE : Items.LEATHER_CHESTPLATE));
        }
        if (random.nextFloat() < 0.5F * armourChance) mob.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.LEATHER_LEGGINGS));
        if (random.nextFloat() < 0.4F * armourChance) mob.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.LEATHER_BOOTS));
        // Gear is worth scavenging, but most of it is damaged or lost in the fight.
        mob.setDropChance(EquipmentSlot.MAINHAND, 0.15F);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            mob.setDropChance(slot, 0.10F);
        }
    }

    /** The skeleton's bow shot. */
    public static void shoot(Mob mob, LivingEntity target, float power) {
        ItemStack bow = mob.getItemInHand(ProjectileUtil.getWeaponHoldingHand(mob, item -> item instanceof BowItem));
        ItemStack projectile = mob.getProjectile(bow);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(mob, projectile, power, bow);
        double xd = target.getX() - mob.getX();
        double yd = target.getY(0.3333333333333333) - arrow.getY();
        double zd = target.getZ() - mob.getZ();
        double distance = Math.sqrt(xd * xd + zd * zd);
        if (mob.level() instanceof ServerLevel serverLevel) {
            Projectile.spawnProjectileUsingShoot(arrow, serverLevel, projectile, xd, yd + distance * 0.2F, zd,
                    1.6F, 14 - serverLevel.getDifficulty().getId() * 4);
        }
        mob.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (mob.getRandom().nextFloat() * 0.4F + 0.8F));
    }
}
