package com.example.bodytrade;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Модуль апгрейда: ПКМ в воздухе открывает дерево улучшений и импланты. Работает из любого места, пока модуль в инвентаре. */
public class UpgradeItem extends Item {
    public static Runnable CLIENT_OPEN = () -> {};

    public UpgradeItem(Settings settings) { super(settings); }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) CLIENT_OPEN.run();
        return TypedActionResult.success(stack, world.isClient);
    }
}
