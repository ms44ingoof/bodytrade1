package com.example.bodytrade.client;

import com.example.bodytrade.BodyPart;
import com.example.bodytrade.BodyTradeMod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Рентген кибер-глаза: руды рядом подсвечиваются цветным контуром сквозь стены. */
public final class OreEsp {
    private OreEsp() {}

    private record Ore(int x, int y, int z, int rgb) {}

    private static final int RADIUS = 18;
    private static final Map<Block, Integer> COLORS = new HashMap<>();
    private static volatile List<Ore> cache = List.of();

    static {
        COLORS.put(Blocks.DIAMOND_ORE, 0x33E6FF); COLORS.put(Blocks.DEEPSLATE_DIAMOND_ORE, 0x33E6FF);
        COLORS.put(Blocks.EMERALD_ORE, 0x33FF66); COLORS.put(Blocks.DEEPSLATE_EMERALD_ORE, 0x33FF66);
        COLORS.put(Blocks.GOLD_ORE, 0xFFD633); COLORS.put(Blocks.DEEPSLATE_GOLD_ORE, 0xFFD633); COLORS.put(Blocks.NETHER_GOLD_ORE, 0xFFD633);
        COLORS.put(Blocks.IRON_ORE, 0xE8B58A); COLORS.put(Blocks.DEEPSLATE_IRON_ORE, 0xE8B58A);
        COLORS.put(Blocks.REDSTONE_ORE, 0xFF3333); COLORS.put(Blocks.DEEPSLATE_REDSTONE_ORE, 0xFF3333);
        COLORS.put(Blocks.LAPIS_ORE, 0x3366FF); COLORS.put(Blocks.DEEPSLATE_LAPIS_ORE, 0x3366FF);
        COLORS.put(Blocks.COAL_ORE, 0x9A9AA5); COLORS.put(Blocks.DEEPSLATE_COAL_ORE, 0x9A9AA5);
        COLORS.put(Blocks.COPPER_ORE, 0xFF8A33); COLORS.put(Blocks.DEEPSLATE_COPPER_ORE, 0xFF8A33);
        COLORS.put(Blocks.NETHER_QUARTZ_ORE, 0xF2F2F2); COLORS.put(Blocks.ANCIENT_DEBRIS, 0xB06A4A);
    }

    private static boolean active() {
        return BodyTradeClient.IMPLANTS.contains("cyber_eye")
                && !(BodyTradeClient.MISSING.contains(BodyPart.LEFT_EYE) && BodyTradeClient.MISSING.contains(BodyPart.RIGHT_EYE));
    }

    /** Раз в 1.5 секунды сканирует блоки вокруг игрока (чанки уже загружены на клиенте). */
    public static void tick(MinecraftClient mc) {
        if (mc.player == null || mc.world == null || !active()) { cache = List.of(); return; }
        if (mc.player.age % 30 != 0) return;
        BlockPos c = mc.player.getBlockPos();
        List<Ore> list = new ArrayList<>();
        BlockPos.Mutable mp = new BlockPos.Mutable();
        Block neurite = BodyTradeMod.NEURITE_ORE;
        outer:
        for (int dx = -RADIUS; dx <= RADIUS; dx++)
            for (int dy = -RADIUS; dy <= RADIUS; dy++)
                for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                    mp.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    BlockState s = mc.world.getBlockState(mp);
                    if (s.isAir()) continue;
                    Block b = s.getBlock();
                    Integer col = COLORS.get(b);
                    if (col == null && b == neurite) col = 0xC04DFF;
                    if (col != null) {
                        list.add(new Ore(mp.getX(), mp.getY(), mp.getZ(), col));
                        if (list.size() >= 700) break outer;
                    }
                }
        cache = list;
    }

    private static void line(BufferBuilder bb, Matrix4f m, double x1, double y1, double z1, double x2, double y2, double z2, int r, int g, int b) {
        bb.vertex(m, (float) x1, (float) y1, (float) z1).color(r, g, b, 255).next();
        bb.vertex(m, (float) x2, (float) y2, (float) z2).color(r, g, b, 255).next();
    }

    /** Рисует контуры без проверки глубины - поэтому видно сквозь стены. */
    public static void render(WorldRenderContext ctx) {
        List<Ore> list = cache;
        if (list.isEmpty() || !active()) return;
        Vec3d cam = ctx.camera().getPos();
        MatrixStack ms = ctx.matrixStack();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        RenderSystem.lineWidth(2.5f);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder bb = tess.getBuffer();
        bb.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        Matrix4f m = ms.peek().getPositionMatrix();
        double e = 0.003;
        for (Ore o : list) {
            double x0 = o.x() - cam.x - e, y0 = o.y() - cam.y - e, z0 = o.z() - cam.z - e;
            double x1 = x0 + 1 + 2 * e, y1 = y0 + 1 + 2 * e, z1 = z0 + 1 + 2 * e;
            int r = (o.rgb() >> 16) & 255, g = (o.rgb() >> 8) & 255, b = o.rgb() & 255;
            line(bb, m, x0, y0, z0, x1, y0, z0, r, g, b); line(bb, m, x1, y0, z0, x1, y0, z1, r, g, b);
            line(bb, m, x1, y0, z1, x0, y0, z1, r, g, b); line(bb, m, x0, y0, z1, x0, y0, z0, r, g, b);
            line(bb, m, x0, y1, z0, x1, y1, z0, r, g, b); line(bb, m, x1, y1, z0, x1, y1, z1, r, g, b);
            line(bb, m, x1, y1, z1, x0, y1, z1, r, g, b); line(bb, m, x0, y1, z1, x0, y1, z0, r, g, b);
            line(bb, m, x0, y0, z0, x0, y1, z0, r, g, b); line(bb, m, x1, y0, z0, x1, y1, z0, r, g, b);
            line(bb, m, x1, y0, z1, x1, y1, z1, r, g, b); line(bb, m, x0, y0, z1, x0, y1, z1, r, g, b);
        }
        tess.draw();
        RenderSystem.lineWidth(1.0f);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
