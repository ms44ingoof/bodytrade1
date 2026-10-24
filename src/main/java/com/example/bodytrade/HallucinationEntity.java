package com.example.bodytrade;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

import java.util.UUID;

/** Чудовище-галлюцинация (болезнь "Психоз"): охотится только за своим игроком и исчезает через 20 секунд. */
public class HallucinationEntity extends HostileEntity {
    private UUID ownerId;

    public HallucinationEntity(EntityType<? extends HostileEntity> type, World world) { super(type, world); }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.17)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }

    public void setOwner(UUID id) { this.ownerId = id; }
    public UUID getOwnerId() { return ownerId; }

    @Override
    protected void initGoals() {
        goalSelector.add(1, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.add(3, new WanderAroundFarGoal(this, 0.6));
        goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));
        targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, 1, false, false,
                e -> ownerId != null && e.getUuid().equals(ownerId)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && (ownerId == null || age > 400)) discard();
    }

    /** Не исчезать на мирной сложности и вдали от игрока: живём ровно положенное время. */
    @Override
    protected boolean isDisallowedInPeaceful() { return false; }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) { return false; }

    @Override
    protected SoundEvent getAmbientSound() { return SoundEvents.ENTITY_ENDERMAN_SCREAM; }

    @Override
    public int getMinAmbientSoundDelay() { return 200; }
}
