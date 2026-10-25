package com.example.bodytrade;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Протезист: неподвижный торговец, продаёт протезы за алмазы. Неуязвим (кроме творческого режима). */
public class ProsthetistEntity extends PathAwareEntity {
    public ProsthetistEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setInvulnerable(true);
        setPersistent();
        setCustomName(Text.literal("Протезист"));
        setCustomNameVisible(true);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(2, new LookAroundGoal(this));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient && player instanceof ServerPlayerEntity sp) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeVarInt(getId());
            ServerPlayNetworking.send(sp, BodyTradeMod.SHOP_PACKET, buf);
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) { return false; }
}
