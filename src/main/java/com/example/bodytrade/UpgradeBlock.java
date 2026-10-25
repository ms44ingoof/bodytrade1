package com.example.bodytrade;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Consumer;

/** Блок апгрейда: улучшение органов и конечностей за частицы жизни. */
public class UpgradeBlock extends Block {
    public static Consumer<BlockPos> CLIENT_OPEN = pos -> {};

    public UpgradeBlock(AbstractBlock.Settings settings) { super(settings); }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) CLIENT_OPEN.accept(pos);
        return ActionResult.SUCCESS;
    }
}
