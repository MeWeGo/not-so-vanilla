package dev.grounded.bandits.client;

import dev.grounded.bandits.entity.HumanCombat;
import dev.grounded.bandits.entity.SkinnedHuman;
import net.minecraft.world.entity.Mob;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Items;

/**
 * Renders bandits with the vanilla (wide) player model and one of the game's own default player skins,
 * so they read as people, not illagers. Uses only textures that ship with Minecraft.
 */
/** Shared by bandits and survivors, so the two are indistinguishable at a glance. */
public class BanditRenderer<T extends Mob & SkinnedHuman> extends HumanoidMobRenderer<T, BanditRenderer.State, HumanoidModel<BanditRenderer.State>> {

    private static final Identifier[] TEXTURES = new Identifier[HumanCombat.SKINS.length];

    static {
        for (int i = 0; i < HumanCombat.SKINS.length; i++) {
            TEXTURES[i] = Identifier.withDefaultNamespace("textures/entity/player/wide/" + HumanCombat.SKINS[i] + ".png");
        }
    }

    public BanditRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this,
                ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), root -> new HumanoidModel<State>(root)),
                context.getEquipmentRenderer()));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(T entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.skin = entity.getSkin();
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURES[state.skin];
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(T mob, HumanoidArm arm) {
        return mob.getMainArm() == arm && mob.isAggressive() && mob.getMainHandItem().is(Items.BOW)
                ? HumanoidModel.ArmPose.BOW_AND_ARROW
                : super.getArmPose(mob, arm);
    }

    public static class State extends HumanoidRenderState {
        public int skin;
    }
}
