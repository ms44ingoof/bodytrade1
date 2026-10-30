package com.example.bodytrade;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.UUID;

/**
 * Доктор из сюжета: подходит к игроку после первого добытого дерева и предлагает зелье перерождения.
 * Бессмертен, ходит только к своему игроку, а если тот ушёл или слишком долго не подходит - исчезает.
 */
public class DoctorEntity extends PathAwareEntity {
    private UUID ownerId;
    private boolean offered;
    private int offeredAt;
    /** Идёт сцена (выпивание зелья или удар): доктор стоит на месте и не исчезает. */
    public boolean busy;

    public DoctorEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        setPersistent();
        setCustomName(Text.literal("Доктор"));
        setCustomNameVisible(true);
        ItemStack potion = new ItemStack(Items.POTION);
        potion.getOrCreateNbt().putInt("CustomPotionColor", 0x55E08A);   // зелёное зелье в руке
        equipStack(EquipmentSlot.MAINHAND, potion);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0);
    }

    public void setOwner(UUID id) { this.ownerId = id; }
    public UUID getOwnerId() { return ownerId; }
    public boolean isOffered() { return offered; }

    @Override
    public void tick() {
        super.tick();
        if (getWorld().isClient || !(getWorld() instanceof net.minecraft.server.world.ServerWorld sw)) return;
        // после перезагрузки мира владельца нет - просто пропадаем (сюжет сам вернётся на шаг "дерево")
        ServerPlayerEntity owner = ownerId == null ? null : sw.getServer().getPlayerManager().getPlayer(ownerId);
        if (owner == null || owner.getWorld() != getWorld() || (!busy && !offered && age > 6000)) {
            Story.poof(this);
            discard();
            return;
        }
        getLookControl().lookAt(owner, 30f, 30f);
        if (busy) { getNavigation().stop(); return; }
        if (offered) {
            // диалог открыт, а в мультиплеере игра не на паузе: защищаем игрока от мобов и других игроков
            if (age % 10 == 0) owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 40, 4, false, false));
            // игрок ушёл «в афк» с открытым диалогом: Доктор уходит, предложение повторится на следующем бревне
            if (age - offeredAt > 6000) Story.expire(owner, this);
            return;
        }
        {
            if (squaredDistanceTo(owner) > 12.0) {
                if (age % 10 == 0) getNavigation().startMovingTo(owner, 1.15);
                if (age >= 400 && age % 200 == 0) {   // застрял - переносим поближе
                    Vec3d spot = Story.findSpot(owner, new int[]{3, 4, 5});
                    if (spot != null) refreshPositionAndAngles(spot.x, spot.y, spot.z, getYaw(), 0f);
                }
            } else {
                getNavigation().stop();
                offered = true;
                offeredAt = age;
                Story.offer(owner);
            }
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return source.isOf(DamageTypes.OUT_OF_WORLD) && super.damage(source, amount);
    }

    @Override
    public boolean isFireImmune() { return true; }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) { return false; }
}
