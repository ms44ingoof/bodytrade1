package com.example.bodytrade;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.World;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class BodyTradeMod implements ModInitializer {
    public static final String ID = "bodytrade";
    public static final Identifier TRADE_PACKET = new Identifier(ID, "trade");
    public static final Identifier RETURN_PACKET = new Identifier(ID, "return");
    public static final Identifier SYNC_PACKET = new Identifier(ID, "sync");
    public static final Identifier LIMBS_PACKET = new Identifier(ID, "limbs");
    public static final Identifier ADMIN_PACKET = new Identifier(ID, "admin");
    public static final Identifier MODE_OPEN_PACKET = new Identifier(ID, "mode_open");
    public static final Identifier MODE_PICK_PACKET = new Identifier(ID, "mode_pick");
    public static final Identifier DEV_BUY_PACKET = new Identifier(ID, "dev_buy");
    public static final Identifier UPG_BUY_PACKET = new Identifier(ID, "upg_buy");
    public static final Identifier TOGGLE_PACKET = new Identifier(ID, "toggle");
    public static final Identifier SANITY_PACKET = new Identifier(ID, "sanity");
    public static final Identifier IMPLANT_BUY_PACKET = new Identifier(ID, "implant_buy");

    /** Шанс обычной болезни/побочного эффекта после каждой сделки. */
    public static final double SIDE_EFFECT_CHANCE = 0.4;
    /** После смерти все части тела, протезы и болезни сбрасываются. */
    public static final boolean RESTORE_ON_DEATH = true;

    /** Клиентская копия списка потерянных частей (заполняется на клиенте, на сервере пуста). */
    public static final Set<BodyPart> CLIENT_MISSING = EnumSet.noneOf(BodyPart.class);
    /** Клиентские маски конечностей/протезов всех игроков (нужны миксину позы "ползёт"). */
    public static final Map<UUID, Integer> CLIENT_LIMBS = new HashMap<>();
    public static final Map<UUID, Integer> CLIENT_PROS = new HashMap<>();
    /** Серверная копия: у кого обе ноги отсутствуют (они ползают). */
    private static final Set<UUID> CRAWLERS = new HashSet<>();
    private static final Map<UUID, Direction> LAST_FACE = new HashMap<>();
    private static final Map<UUID, Integer> FUEL = new HashMap<>();
    private static boolean DRILLING = false;

    /** Игрок ползёт (обе ноги отсутствуют и не заменены протезами)? Работает и на клиенте, и на сервере. */
    public static boolean isCrawler(PlayerEntity p) {
        if (p.getWorld().isClient) {
            Integer m = CLIENT_LIMBS.get(p.getUuid());
            if (m == null) return false;
            int pm = CLIENT_PROS.getOrDefault(p.getUuid(), 0);
            return ((m & ~pm) & 12) == 12;
        }
        return CRAWLERS.contains(p.getUuid());
    }

    private static final Set<Item> SOFT_FOOD = Set.of(Items.MUSHROOM_STEW, Items.BEETROOT_SOUP, Items.RABBIT_STEW,
            Items.SUSPICIOUS_STEW, Items.HONEY_BOTTLE, Items.MELON_SLICE, Items.SWEET_BERRIES, Items.GLOW_BERRIES, Items.PUMPKIN_PIE);

    public static Block TRADE_BLOCK, ADMIN_BLOCK, DEV_BLOCK, NEURITE_ORE, SELENITE_ORE, LIFE_CONVERTER;
    public static Item LIFE_PARTICLE, UPGRADE_DEVICE, NEURITE, SELENITE, SPECTRAL_PRISM;
    public static Item FLASH_CYBER_EYE, FLASH_LANTERN, FLASH_BLADE_ARM, FLASH_TITAN_BONES, FLASH_NEUROCHIP, FLASH_CHRONO, FLASH_STASIS, FLASH_MECH_EAR;
    public static final Identifier DECAY_PACKET = new Identifier(ID, "decay");
    public static final Identifier CONVERT_PACKET = new Identifier(ID, "convert");
    /** Игроки, которым нужно отправить обновлённое состояние (объединяем запросы за тик, чтобы не спамить пакетами). */
    private static final Set<UUID> NEED_SYNC = new HashSet<>();
    private static final Map<UUID, Integer> LAST_DECAY = new HashMap<>();
    private static final Map<UUID, Integer> LAST_HASH = new HashMap<>();
    private static final Map<UUID, Integer> LAST_SANITY = new HashMap<>();
    private static final Map<UUID, Integer> LAST_BLADE = new HashMap<>();
    public static EntityType<HallucinationEntity> SMILER, SCREAMER;

    private static final Map<UUID, Float> LAST_FALL = new HashMap<>();
    private static final Map<UUID, Integer> LAST_FOOD = new HashMap<>();
    private static final Map<UUID, Integer> LAST_JUMP = new HashMap<>();
    private static final Map<UUID, Integer> LAST_WALK = new HashMap<>();
    private static final Map<UUID, Integer> NERVE_CD = new HashMap<>();

    @Override
    public void onInitialize() {
        TRADE_BLOCK = Registry.register(Registries.BLOCK, new Identifier(ID, "trade_altar"),
                new TradeBlock(FabricBlockSettings.create().strength(3.0f)));
        Registry.register(Registries.ITEM, new Identifier(ID, "trade_altar"), new BlockItem(TRADE_BLOCK, new FabricItemSettings()));
        ADMIN_BLOCK = Registry.register(Registries.BLOCK, new Identifier(ID, "admin_altar"),
                new AdminBlock(FabricBlockSettings.create().strength(3.0f)));
        Registry.register(Registries.ITEM, new Identifier(ID, "admin_altar"), new BlockItem(ADMIN_BLOCK, new FabricItemSettings()));

        DEV_BLOCK = Registry.register(Registries.BLOCK, new Identifier(ID, "development_block"),
                new DevBlock(FabricBlockSettings.create().strength(3.0f)));
        Registry.register(Registries.ITEM, new Identifier(ID, "development_block"), new BlockItem(DEV_BLOCK, new FabricItemSettings()));
        UPGRADE_DEVICE = Registry.register(Registries.ITEM, new Identifier(ID, "upgrade_device"), new UpgradeItem(new FabricItemSettings().maxCount(1)));
        // нейритовая руда (фиолетовая): из неё делают флешки имплантов
        NEURITE_ORE = Registry.register(Registries.BLOCK, new Identifier(ID, "neurite_ore"),
                new Block(FabricBlockSettings.create().strength(4.0f).requiresTool()));
        Registry.register(Registries.ITEM, new Identifier(ID, "neurite_ore"), new BlockItem(NEURITE_ORE, new FabricItemSettings()));
        NEURITE = Registry.register(Registries.ITEM, new Identifier(ID, "neurite"), new Item(new FabricItemSettings()));
        // селенитовая руда (белая) и спектральная призма: из них делают флешки
        SELENITE_ORE = Registry.register(Registries.BLOCK, new Identifier(ID, "selenite_ore"),
                new Block(FabricBlockSettings.create().strength(4.0f).requiresTool()));
        Registry.register(Registries.ITEM, new Identifier(ID, "selenite_ore"), new BlockItem(SELENITE_ORE, new FabricItemSettings()));
        SELENITE = Registry.register(Registries.ITEM, new Identifier(ID, "selenite"), new Item(new FabricItemSettings()));
        SPECTRAL_PRISM = Registry.register(Registries.ITEM, new Identifier(ID, "spectral_prism"), new Item(new FabricItemSettings()));
        FLASH_CYBER_EYE = Registry.register(Registries.ITEM, new Identifier(ID, "flash_cyber_eye"), new Item(new FabricItemSettings().maxCount(16)));
        FLASH_LANTERN = Registry.register(Registries.ITEM, new Identifier(ID, "flash_lantern"), new Item(new FabricItemSettings().maxCount(16)));
        FLASH_BLADE_ARM = Registry.register(Registries.ITEM, new Identifier(ID, "flash_blade_arm"), new Item(new FabricItemSettings().maxCount(16)));
        FLASH_TITAN_BONES = Registry.register(Registries.ITEM, new Identifier(ID, "flash_titan_bones"), new Item(new FabricItemSettings().maxCount(16)));
        FLASH_NEUROCHIP = Registry.register(Registries.ITEM, new Identifier(ID, "flash_neurochip"), new Item(new FabricItemSettings().maxCount(16)));
        FLASH_CHRONO = Registry.register(Registries.ITEM, new Identifier(ID, "flash_chrono"), new Item(new FabricItemSettings().maxCount(16)));
        FLASH_STASIS = Registry.register(Registries.ITEM, new Identifier(ID, "flash_stasis"), new Item(new FabricItemSettings().maxCount(16)));
        FLASH_MECH_EAR = Registry.register(Registries.ITEM, new Identifier(ID, "flash_mech_ear"), new Item(new FabricItemSettings().maxCount(16)));
        // Синтезатор жизни: очень дорогой блок, обменивает алмазы на частицы жизни 1:1
        LIFE_CONVERTER = Registry.register(Registries.BLOCK, new Identifier(ID, "life_converter"),
                new ConverterBlock(FabricBlockSettings.create().strength(5.0f)));
        Registry.register(Registries.ITEM, new Identifier(ID, "life_converter"), new BlockItem(LIFE_CONVERTER, new FabricItemSettings()));
        // генерация руды в новых чанках Верхнего мира (на глубине от -60 до 16)
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES,
                RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(ID, "neurite_ore")));
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.UNDERGROUND_ORES,
                RegistryKey.of(RegistryKeys.PLACED_FEATURE, new Identifier(ID, "selenite_ore")));
        LIFE_PARTICLE = Registry.register(Registries.ITEM, new Identifier(ID, "life_particle"), new Item(new FabricItemSettings()));

        SMILER = Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "smiler"),
                FabricEntityTypeBuilder.<HallucinationEntity>create(SpawnGroup.MONSTER, HallucinationEntity::new)
                        .dimensions(EntityDimensions.fixed(0.8f, 2.0f)).build());
        SCREAMER = Registry.register(Registries.ENTITY_TYPE, new Identifier(ID, "screamer"),
                FabricEntityTypeBuilder.<HallucinationEntity>create(SpawnGroup.MONSTER, HallucinationEntity::new)
                        .dimensions(EntityDimensions.fixed(1.0f, 2.2f)).build());
        FabricDefaultAttributeRegistry.register(SMILER, HallucinationEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SCREAMER, HallucinationEntity.createAttributes());

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(e -> { e.add(TRADE_BLOCK); e.add(ADMIN_BLOCK); e.add(DEV_BLOCK); e.add(UPGRADE_DEVICE); e.add(LIFE_PARTICLE); e.add(NEURITE_ORE); e.add(NEURITE); e.add(SELENITE_ORE); e.add(SELENITE); e.add(SPECTRAL_PRISM);
            e.add(FLASH_CYBER_EYE); e.add(FLASH_LANTERN); e.add(FLASH_BLADE_ARM); e.add(FLASH_TITAN_BONES); e.add(FLASH_NEUROCHIP); e.add(FLASH_CHRONO); e.add(FLASH_STASIS); e.add(FLASH_MECH_EAR); e.add(LIFE_CONVERTER); });

        Story.init();

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity p = handler.player;
            BodyState st = BodyState.of(server);
            if (st.markGaveBlock(p.getUuid())) {
                // сложность теперь выбирается в сюжете: сначала добыча дерева, потом приходит Доктор (см. Story)
                Story.begin(p, st);
            }
            sync(p);
            // рецепты мода сразу появляются в книге рецептов
            java.util.List<net.minecraft.recipe.Recipe<?>> modRecipes = new ArrayList<>();
            for (net.minecraft.recipe.Recipe<?> rc : server.getRecipeManager().values()) if (rc.getId().getNamespace().equals(ID)) modRecipes.add(rc);
            p.unlockRecipes(modRecipes);
            LAST_SANITY.remove(p.getUuid());
            for (ServerPlayerEntity q : server.getPlayerManager().getPlayerList())
                sendLimbs(p, q.getUuid(), BodyPart.limbMask(st.get(q.getUuid())), BodyPart.limbMask(st.pros(q.getUuid())));
            broadcastLimbs(server, p);
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            Story.onRespawn(newP);
            if (!alive && RESTORE_ON_DEATH) {
                BodyState ds = BodyState.of(newP.getServer());
                if (ds.isDevelop(newP.getUuid())) { ds.effects(newP.getUuid()).clear(); ds.markDirty(); } // в Перерождение прогресс не теряется
                else ds.clear(newP.getUuid());
                ds.setSanity(newP.getUuid(), 1000);
                sync(newP);
                broadcastLimbs(newP.getServer(), newP);
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(TRADE_PACKET, (server, player, h, buf, resp) -> {
            BlockPos pos = buf.readBlockPos();
            server.execute(() -> handleTrade(server, player, pos));
        });
        ServerPlayNetworking.registerGlobalReceiver(RETURN_PACKET, (server, player, h, buf, resp) -> {
            BlockPos pos = buf.readBlockPos();
            BodyPart part = BodyPart.byId(buf.readString());
            server.execute(() -> handleReturn(server, player, pos, part));
        });
        ServerPlayNetworking.registerGlobalReceiver(ADMIN_PACKET, (server, player, h, buf, resp) -> {
            BlockPos pos = buf.readBlockPos();
            String action = buf.readString();
            server.execute(() -> handleAdmin(server, player, pos, action));
        });
        ServerPlayNetworking.registerGlobalReceiver(MODE_PICK_PACKET, (server, player, h, buf, resp) -> {
            String mode = buf.readString();
            server.execute(() -> applyMode(server, player, mode.equals("develop")));
        });
        ServerPlayNetworking.registerGlobalReceiver(IMPLANT_BUY_PACKET, (server, player, h, buf, resp) -> {
            Implant im = Implant.byId(buf.readString());
            server.execute(() -> handleImplantBuy(server, player, im));
        });
        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_PACKET, (server, player, h, buf, resp) -> {
            String what = buf.readString();
            server.execute(() -> {
                BodyState st = BodyState.of(server);
                if (what.equals("esp")) {
                    if (!st.hasImplant(player.getUuid(), "cyber_eye")) return;
                    boolean on = !st.isEspOn(player.getUuid());
                    st.setEspOn(player.getUuid(), on);
                    player.sendMessage(Text.literal("Подсветка кибер-глаза: " + (on ? "включена" : "выключена")).formatted(on ? Formatting.AQUA : Formatting.GRAY), true);
                    sync(player);
                    return;
                }
                if (!what.equals("drill")) return;
                if (st.upgrades(player.getUuid(), Upgrade.values().length)[Upgrade.DRILL.ordinal()] <= 0) return;
                boolean on = !st.isDrillOn(player.getUuid());
                st.setDrillOn(player.getUuid(), on);
                player.sendMessage(Text.literal("Бур: " + (on ? "включён" : "выключен")).formatted(on ? Formatting.GREEN : Formatting.GRAY), true);
                sync(player);
            });
        });
        ServerPlayNetworking.registerGlobalReceiver(UPG_BUY_PACKET, (server, player, h, buf, resp) -> {
            Upgrade u = Upgrade.byId(buf.readString());
            server.execute(() -> handleUpgBuy(server, player, u));
        });
        ServerPlayNetworking.registerGlobalReceiver(DEV_BUY_PACKET, (server, player, h, buf, resp) -> {
            BlockPos pos = buf.readBlockPos();
            BodyPart part = BodyPart.byId(buf.readString());
            server.execute(() -> handleDevBuy(server, player, pos, part));
        });

        // /bodymode normal | develop - сменить сложность в любой момент
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(
                CommandManager.literal("bodymode")
                        .then(CommandManager.literal("normal").executes(ctx -> {
                            applyMode(ctx.getSource().getServer(), ctx.getSource().getPlayerOrThrow(), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("develop").executes(ctx -> {
                            applyMode(ctx.getSource().getServer(), ctx.getSource().getPlayerOrThrow(), true);
                            return 1;
                        }))));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var players = server.getPlayerManager().getPlayerList();
            for (ServerPlayerEntity p : players) tickPlayer(server, p);
            Story.tick(server);
            if (!NEED_SYNC.isEmpty()) {
                for (ServerPlayerEntity p : players) if (NEED_SYNC.remove(p.getUuid())) doSync(p);
                NEED_SYNC.clear();
            }
        });

        // при выходе игрока чистим его данные в памяти (важно для долгой игры на сервере с друзьями)
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { Story.onDisconnect(server, handler.player); forget(handler.player.getUuid()); });
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> forgetAll());
        ServerPlayNetworking.registerGlobalReceiver(CONVERT_PACKET, (server, player, h, buf, resp) -> {
            BlockPos pos = buf.readBlockPos();
            int amount = buf.readVarInt();
            server.execute(() -> handleConvert(server, player, pos, amount));
        });

        // Рот / язык / гортань: нельзя говорить в чате
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            Set<BodyPart> m = BodyState.of(sender.getServer()).functional(sender.getUuid());
            if (m.contains(BodyPart.MOUTH) || m.contains(BodyPart.TONGUE) || m.contains(BodyPart.LARYNX)) {
                sender.sendMessage(Text.literal("Вы не можете говорить.").formatted(Formatting.RED), true);
                return false;
            }
            return true;
        });

        // Рот / глотка / зубы: ограничения на еду и питьё; оголённый нерв при использовании предмета
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (!world.isClient && player instanceof ServerPlayerEntity sp && !stack.isEmpty()) nerveUse(sp, hand, "использование предмета");
            boolean food = stack.isFood();
            boolean drink = stack.getUseAction() == UseAction.DRINK;
            if (!food && !drink) return TypedActionResult.pass(stack);
            Set<BodyPart> m = world.isClient ? CLIENT_MISSING : BodyState.of(world.getServer()).functional(player.getUuid());
            String why = null;
            if (m.contains(BodyPart.MOUTH)) why = "Без рта вы не можете есть и пить.";
            else if (m.contains(BodyPart.THROAT)) why = "Без глотки вы не можете глотать.";
            else if (food && m.contains(BodyPart.TEETH) && !SOFT_FOOD.contains(stack.getItem())) why = "Без зубов вы можете есть только мягкую пищу.";
            if (why == null) return TypedActionResult.pass(stack);
            if (!world.isClient) player.sendMessage(Text.literal(why).formatted(Formatting.RED), true);
            return TypedActionResult.fail(stack);
        });

        // Оголённый нерв: действия рукой причиняют боль
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) nerveUse(sp, hand, "ставите или используете блок");
            return ActionResult.PASS;
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!world.isClient && player instanceof ServerPlayerEntity sp) { nerveUse(sp, hand, "бьёте"); bladeHit(sp, entity); }
            return ActionResult.PASS;
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
            if (!world.isClient) LAST_FACE.put(player.getUuid(), dir);
            return ActionResult.PASS;
        });
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
            if (player instanceof ServerPlayerEntity sp) {
                nerveUse(sp, Hand.MAIN_HAND, "ломаете блок");
                Story.onBlockBroken(sp, state);
                if (world instanceof ServerWorld sw) drillBreak(sp, sw, pos);
            }
        });
    }

    // ---------------------------------------------------------------- обмен
    private static boolean nearAltar(ServerPlayerEntity p, BlockPos pos) {
        return p.getWorld().getBlockState(pos).isOf(TRADE_BLOCK) && p.squaredDistanceTo(Vec3d.ofCenter(pos)) <= 64;
    }

    private static void handleTrade(MinecraftServer server, ServerPlayerEntity p, BlockPos pos) {
        if (!nearAltar(p, pos) || !p.isAlive()) return;
        BodyState st = BodyState.of(server);
        BodyPart part = BodyPart.roll(st.get(p.getUuid()), p.getRandom());
        if (part == null) {
            p.sendMessage(Text.literal("Вам больше нечего отдавать.").formatted(Formatting.GRAY));
            return;
        }
        ItemStack reward = part.tier.roll(p.getRandom());
        Text rewardText = Text.literal("Получено: " + reward.getCount() + " × ").append(reward.getName());
        p.getInventory().offerOrDrop(reward);
        st.add(p.getUuid(), part);
        if (part.id.contains("arm") || part.id.contains("leg") || part.id.contains("foot"))   // хруст кости
            p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.PLAYERS, 1.2f, 0.7f);
        unlock(p, st, "part:" + part.id, part.nom);

        title(p, Text.literal("Вы лишились " + part.gen).formatted(Formatting.DARK_RED), rewardText.copy().formatted(Formatting.GRAY));
        p.sendMessage(Text.literal(part.consequence).formatted(Formatting.RED));

        SideEffect se = null;
        for (SideEffect x : SideEffect.values())
            if (x.isRare() && p.getRandom().nextDouble() < x.rareChance) { se = x; break; }
        if (se == null && p.getRandom().nextDouble() < SIDE_EFFECT_CHANCE) {
            List<SideEffect> common = new ArrayList<>();
            for (SideEffect x : SideEffect.values()) if (!x.isRare()) common.add(x);
            se = common.get(p.getRandom().nextInt(common.size()));
        }
        if (se != null) {
            st.setEffect(p.getUuid(), se, server.getOverworld().getTime() + se.seconds * 20L);
            unlock(p, st, "fx:" + se.name(), se.title);
            p.sendMessage(Text.literal(se.title + ": " + se.message).formatted(Formatting.YELLOW));
        }
        sync(p);
        broadcastLimbs(server, p);
    }

    private static void handleReturn(MinecraftServer server, ServerPlayerEntity p, BlockPos pos, BodyPart part) {
        if (part == null || !nearAltar(p, pos)) return;
        BodyState st = BodyState.of(server);
        if (!st.get(p.getUuid()).contains(part)) return;
        Item item = part.tier.refundItem;
        int need = part.tier.refundCount;
        var inv = p.getInventory();
        if (inv.count(item) < need) {
            p.sendMessage(Text.literal("Не хватает: нужно " + need + " × ").append(new ItemStack(item).getName()).formatted(Formatting.RED));
            return;
        }
        removeItems(p, item, need);
        st.remove(p.getUuid(), part);
        st.removePros(p.getUuid(), part);
        title(p, Text.literal("Вы вернули: " + part.nom.toLowerCase()).formatted(Formatting.GREEN), Text.literal("Последствия сняты").formatted(Formatting.GRAY));
        p.sendMessage(Text.literal("Часть тела возвращена: " + part.nom + ". Связанные последствия исчезли.").formatted(Formatting.GREEN));
        sync(p);
        broadcastLimbs(server, p);
    }

    private static void removeItems(ServerPlayerEntity p, Item item, int count) {
        var inv = p.getInventory();
        int left = count;
        for (int i = 0; i < inv.size() && left > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) { int t = Math.min(left, s.getCount()); s.decrement(t); left -= t; }
        }
    }

    /** Админ-блок: выдать/снять любую часть тела или болезнь. Доступно в креативе или операторам (уровень 2). */
    private static void handleAdmin(MinecraftServer server, ServerPlayerEntity p, BlockPos pos, String action) {
        if (!p.getWorld().getBlockState(pos).isOf(ADMIN_BLOCK) || p.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64) return;
        if (!(p.isCreative() || p.hasPermissionLevel(2))) {
            p.sendMessage(Text.literal("Нужны права администратора (креатив или оператор).").formatted(Formatting.RED));
            return;
        }
        BodyState st = BodyState.of(server);
        UUID id = p.getUuid();
        long now = server.getOverworld().getTime();
        if (action.startsWith("part:")) {
            BodyPart part = BodyPart.byId(action.substring(5));
            if (part == null) return;
            if (st.get(id).contains(part)) {
                st.remove(id, part);
                st.removePros(id, part);
                p.sendMessage(Text.literal("[Админ] Возвращено: " + part.nom).formatted(Formatting.GREEN));
            } else {
                st.add(id, part);
                unlock(p, st, "part:" + part.id, part.nom);
                p.sendMessage(Text.literal("[Админ] Отнято: " + part.nom).formatted(Formatting.RED));
                p.sendMessage(Text.literal(part.consequence).formatted(Formatting.RED));
            }
        } else if (action.startsWith("effect:")) {
            SideEffect se;
            try { se = SideEffect.valueOf(action.substring(7)); } catch (IllegalArgumentException ex) { return; }
            if (st.effects(id).containsKey(se) && st.effects(id).get(se) > now) {
                st.effects(id).remove(se);
                st.markDirty();
                p.sendMessage(Text.literal("[Админ] Снята болезнь: " + se.title).formatted(Formatting.GREEN));
            } else {
                st.setEffect(id, se, now + se.seconds * 20L);
                unlock(p, st, "fx:" + se.name(), se.title);
                p.sendMessage(Text.literal("[Админ] Выдана болезнь: " + se.title).formatted(Formatting.YELLOW));
                p.sendMessage(Text.literal(se.title + ": " + se.message).formatted(Formatting.YELLOW));
            }
        } else if (action.equals("parts_clear")) {
            st.get(id).clear();
            st.pros(id).clear();
            st.markDirty();
            p.sendMessage(Text.literal("[Админ] Все части тела возвращены.").formatted(Formatting.GREEN));
        } else if (action.equals("effects_clear")) {
            st.effects(id).clear();
            st.markDirty();
            p.sendMessage(Text.literal("[Админ] Все болезни сняты.").formatted(Formatting.GREEN));
        } else {
            return;
        }
        sync(p);
        broadcastLimbs(server, p);
    }

    private static void title(ServerPlayerEntity p, Text title, Text sub) {
        var h = p.networkHandler;
        h.sendPacket(new TitleFadeS2CPacket(10, 70, 20));
        h.sendPacket(new SubtitleS2CPacket(sub));
        h.sendPacket(new TitleS2CPacket(title));
    }

    // ---------------------------------------------------------------- сеть
    /** Просит отправить игроку актуальное состояние (будет отправлено один раз в конце тика). */
    public static void sync(ServerPlayerEntity p) { NEED_SYNC.add(p.getUuid()); }

    private static void doSync(ServerPlayerEntity p) {
        MinecraftServer s = p.getServer();
        BodyState st = BodyState.of(s);
        long now = s.getOverworld().getTime();
        Set<BodyPart> m = st.get(p.getUuid());
        Set<BodyPart> pr = st.pros(p.getUuid());
        List<Map.Entry<SideEffect, Long>> act = new ArrayList<>();
        for (Map.Entry<SideEffect, Long> e : st.effects(p.getUuid()).entrySet()) if (e.getValue() > now) act.add(e);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(m.size());
        for (BodyPart bp : m) buf.writeString(bp.id);
        buf.writeVarInt(pr.size());
        for (BodyPart bp : pr) buf.writeString(bp.id);
        buf.writeVarInt(act.size());
        for (Map.Entry<SideEffect, Long> e : act) { buf.writeString(e.getKey().title); buf.writeVarInt((int) ((e.getValue() - now) / 20)); }
        boolean dev = st.isDevelop(p.getUuid());
        buf.writeBoolean(dev);
        if (dev) {
            List<DevTasks.Task> tasks = DevTasks.tasks();
            int n = tasks.size();
            int[] base = st.taskBase(p.getUuid(), n), done = st.taskDone(p.getUuid(), n);
            buf.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                DevTasks.Task t = tasks.get(i);
                boolean d = done[i] == 1;
                int prog = d ? t.need() : (base[i] < 0 ? 0 : Math.max(0, Math.min(t.need(), (DevTasks.progress(p, t) - base[i]) / t.div())));
                buf.writeVarInt(prog);
                buf.writeBoolean(d);
            }
        }
        int[] lv = st.upgrades(p.getUuid(), Upgrade.values().length);
        buf.writeVarInt(lv.length);
        for (int x : lv) buf.writeVarInt(x);
        buf.writeBoolean(st.isDrillOn(p.getUuid()));
        Map<String, Integer> imp = st.implantMap(p.getUuid());
        buf.writeVarInt(imp.size());
        for (Map.Entry<String, Integer> ie : imp.entrySet()) { buf.writeString(ie.getKey()); buf.writeVarInt(ie.getValue()); }
        Set<String> lor = st.lore(p.getUuid());
        buf.writeVarInt(lor.size());
        for (String k : lor) buf.writeString(k);
        buf.writeBoolean(st.isEspOn(p.getUuid()));
        ServerPlayNetworking.send(p, SYNC_PACKET, buf);
    }

    private static void sendLimbs(ServerPlayerEntity to, UUID who, int mask, int prosMask) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(who);
        buf.writeVarInt(mask);
        buf.writeVarInt(prosMask);
        ServerPlayNetworking.send(to, LIMBS_PACKET, buf);
    }

    /** Все игроки узнают, каких конечностей не хватает у p и какие протезы стоят (для отрисовки модели). */
    private static void broadcastLimbs(MinecraftServer server, ServerPlayerEntity p) {
        BodyState st = BodyState.of(server);
        int mask = BodyPart.limbMask(st.get(p.getUuid()));
        int pm = BodyPart.limbMask(st.pros(p.getUuid()));
        for (ServerPlayerEntity q : server.getPlayerManager().getPlayerList()) sendLimbs(q, p.getUuid(), mask, pm);
    }

    // ---------------------------------------------------------------- режим "Перерождение"
    /** Выбор сложности: develop - без рук и ног, возвращать их надо заданиями; normal - обычная игра. */
    public static void applyMode(MinecraftServer server, ServerPlayerEntity p, boolean develop) {
        BodyState st = BodyState.of(server);
        UUID id = p.getUuid();
        if (develop) {
            st.setDevelop(id, true);
            for (BodyPart bp : DevTasks.PRICES.keySet()) { st.add(id, bp); st.removePros(id, bp); unlock(p, st, "part:" + bp.id, bp.nom); }
            List<DevTasks.Task> tasks = DevTasks.tasks();
            int[] base = st.taskBase(id, tasks.size()), done = st.taskDone(id, tasks.size());
            for (int i = 0; i < tasks.size(); i++) { base[i] = DevTasks.progress(p, tasks.get(i)); done[i] = 0; }
            st.markDirty();
            LAST_HASH.remove(id);
            // блоки ставятся прямо рядом с игроком (если места нет - попадают в инвентарь)
            if (!placeNear(p, DEV_BLOCK, 0)) p.getInventory().offerOrDrop(new ItemStack(DEV_BLOCK));
            st.setDecay(id, BodyState.DECAY_TICKS);
            LAST_DECAY.remove(id);
            title(p, Text.literal("Перерождение").formatted(Formatting.GREEN), Text.literal("Вы лишились всех конечностей").formatted(Formatting.GRAY));
            p.sendMessage(Text.literal("Режим «Перерождение»: у вас нет рук и ног. Выполняйте задания в любом порядке (список: кнопка «Задания» в инвентаре), "
                    + "получайте частицы жизни и покупайте конечности в Блоке развития (он стоит рядом с вами). Для улучшений органов скрафтите Модуль апгрейда (рецепт в книге рецептов).")
                    .formatted(Formatting.GREEN));
        } else {
            st.setDevelop(id, false);
            for (BodyPart bp : DevTasks.PRICES.keySet()) { st.remove(id, bp); st.removePros(id, bp); }
            p.sendMessage(Text.literal("Обычная сложность. Конечности возвращены.").formatted(Formatting.GRAY));
            if (st.markGaveAltar(id)) {
                p.getInventory().offerOrDrop(new ItemStack(TRADE_BLOCK));
                p.sendMessage(Text.literal("Вам выдан Алтарь обмена. Поставьте его и нажмите ПКМ.").formatted(Formatting.GRAY));
            }
        }
        sync(p);
        broadcastLimbs(server, p);
    }

    /** Ставит блок рядом с игроком (slot 0 - слева, 1 - справа от направления взгляда). */
    private static boolean placeNear(ServerPlayerEntity p, Block block, int slot) {
        if (!(p.getWorld() instanceof ServerWorld w)) return false;
        Direction f = p.getHorizontalFacing();
        Direction r = f.rotateYClockwise();
        BlockPos base = p.getBlockPos();
        int[] sides = slot == 0 ? new int[]{-1, -2, 0, -3, 1} : new int[]{1, 2, 3, 0, -1};
        for (int d : new int[]{2, 3, 4}) {
            for (int sd : sides) {
                for (int dy : new int[]{0, 1, -1}) {
                    BlockPos pos = base.offset(f, d).offset(r, sd).up(dy);
                    BlockPos below = pos.down();
                    if (w.getBlockState(pos).isAir() && w.getBlockState(pos.up()).isAir() && w.getBlockState(below).isSolidBlock(w, below)) {
                        w.setBlockState(pos, block.getDefaultState());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void handleDevBuy(MinecraftServer server, ServerPlayerEntity p, BlockPos pos, BodyPart part) {
        if (part == null || !DevTasks.PRICES.containsKey(part)) return;
        if (!p.getWorld().getBlockState(pos).isOf(DEV_BLOCK) || p.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64) return;
        BodyState st = BodyState.of(server);
        UUID id = p.getUuid();
        if (!st.get(id).contains(part)) return;
        int price = DevTasks.PRICES.get(part);
        if (p.getInventory().count(LIFE_PARTICLE) < price) {
            p.sendMessage(Text.literal("Не хватает частиц жизни: нужно " + price + ".").formatted(Formatting.RED));
            return;
        }
        removeItems(p, LIFE_PARTICLE, price);
        st.remove(id, part);
        st.removePros(id, part);
        title(p, Text.literal("Вы вернули: " + part.nom.toLowerCase()).formatted(Formatting.GREEN), Text.literal("Куплено за частицы жизни").formatted(Formatting.GRAY));
        p.sendMessage(Text.literal("Часть тела возвращена: " + part.nom + ".").formatted(Formatting.GREEN));
        sync(p);
        broadcastLimbs(server, p);
    }

    /** Выдаёт частицы жизни стопками (награда может быть больше 64). */
    private static void giveParticles(ServerPlayerEntity p, int n) {
        while (n > 0) {
            int k = Math.min(64, n);
            p.getInventory().offerOrDrop(new ItemStack(LIFE_PARTICLE, k));
            n -= k;
        }
    }

    /** Раз в секунду проверяет все задания (они выполняются в любом порядке). */
    private static void devTick(ServerPlayerEntity p, BodyState st) {
        UUID id = p.getUuid();
        if (!st.isDevelop(id)) return;
        List<DevTasks.Task> tasks = DevTasks.tasks();
        int n = tasks.size();
        int[] base = st.taskBase(id, n), done = st.taskDone(id, n);
        int hash = 1;
        boolean changed = false;
        for (int i = 0; i < n; i++) {
            if (done[i] == 1) { hash = 31 * hash + 1_000_000; continue; }
            DevTasks.Task t = tasks.get(i);
            int raw = DevTasks.progress(p, t);
            if (base[i] < 0) { base[i] = raw; changed = true; }
            int prog = Math.max(0, (raw - base[i]) / t.div());
            if (prog >= t.need()) {
                done[i] = 1;
                changed = true;
                giveParticles(p, t.reward());
                title(p, Text.literal("Задание выполнено").formatted(Formatting.GREEN), Text.literal(t.text() + ": +" + t.reward() + " ч. жизни").formatted(Formatting.GRAY));
                p.sendMessage(Text.literal("Задание «" + t.text() + "» выполнено! Получено частиц жизни: " + t.reward() + ".").formatted(Formatting.GREEN));
                prog = t.need();
            }
            hash = 31 * hash + prog;
        }
        if (changed) st.markDirty();
        Integer last = LAST_HASH.get(id);
        if (changed || last == null || last != hash) { LAST_HASH.put(id, hash); sync(p); }
    }

    // ---------------------------------------------------------------- ветки: бур и реактивные ноги
    /** Бур: после обычной добычи ломает область вокруг блока (3x3 / 6x6 / 9x9) в плоскости удара. */
    private static void drillBreak(ServerPlayerEntity p, ServerWorld w, BlockPos origin) {
        if (DRILLING) return;
        BodyState st = BodyState.of(w.getServer());
        UUID id = p.getUuid();
        int lvl = st.upgrades(id, Upgrade.values().length)[Upgrade.DRILL.ordinal()];
        if (lvl <= 0 || !st.isDrillOn(id)) return;
        Set<BodyPart> m = st.functional(id);
        if (m.contains(BodyPart.LEFT_ARM) && m.contains(BodyPart.RIGHT_ARM)) return;
        int size = new int[]{0, 3, 6, 9}[lvl];
        int lo = -((size - 1) / 2), hi = size / 2;
        Direction face = LAST_FACE.getOrDefault(id, Direction.getEntityFacingOrder(p)[0]);
        Direction.Axis ax = face.getAxis();
        ItemStack tool = p.getMainHandStack();
        DRILLING = true;
        try {
            for (int i = lo; i <= hi; i++) {
                for (int j = lo; j <= hi; j++) {
                    if (i == 0 && j == 0) continue;
                    BlockPos pos = switch (ax) {
                        case X -> origin.add(0, i, j);
                        case Y -> origin.add(i, 0, j);
                        default -> origin.add(i, j, 0);
                    };
                    BlockState s = w.getBlockState(pos);
                    if (s.isAir() || !s.getFluidState().isEmpty() || w.getBlockEntity(pos) != null) continue;
                    float hard = s.getHardness(w, pos);
                    if (hard < 0 || hard > 10f) continue;
                    p.incrementStat(Stats.MINED.getOrCreateStat(s.getBlock()));
                    if (!p.isCreative() && p.canHarvest(s)) Block.dropStacks(s, w, pos, null, p, tool);
                    w.breakBlock(pos, false, p);
                }
            }
        } finally {
            DRILLING = false;
        }
    }

    /** Реактивные ноги: полёт с ограниченным топливом (10 / 25 / 60 с), топливо восстанавливается на земле. */
    private static void jetTick(ServerPlayerEntity p, int lvl, boolean legsOk) {
        if (p.isCreative() || p.isSpectator()) return;
        UUID id = p.getUuid();
        var ab = p.getAbilities();
        if (lvl <= 0 || !legsOk) {
            if (FUEL.containsKey(id) && ab.allowFlying) { ab.allowFlying = false; ab.flying = false; ab.setFlySpeed(0.05f); p.sendAbilitiesUpdate(); }
            FUEL.remove(id);
            return;
        }
        int max = new int[]{0, 200, 500, 1200}[lvl];
        float speed = new float[]{0f, 0.025f, 0.05f, 0.12f}[lvl];
        int fuel = Math.min(FUEL.getOrDefault(id, max), max);
        if (ab.flying) {
            fuel--;
            if (fuel <= 0) {
                fuel = 0;
                ab.flying = false;
                ab.allowFlying = false;
                p.sendAbilitiesUpdate();
                effT(p, StatusEffects.SLOW_FALLING, 100, 0);
                bar(p, "Топливо кончилось!");
            }
        } else if (p.isOnGround()) {
            int regen = lvl == 1 ? (p.age % 2 == 0 ? 1 : 0) : (lvl == 2 ? 1 : 2);
            fuel = Math.min(max, fuel + regen);
        }
        FUEL.put(id, fuel);
        boolean canFly = fuel >= (ab.allowFlying ? 1 : 20);
        if (canFly && (!ab.allowFlying || ab.getFlySpeed() != speed)) {
            ab.allowFlying = true;
            ab.setFlySpeed(speed);
            p.sendAbilitiesUpdate();
        } else if (!canFly && ab.allowFlying) {
            ab.allowFlying = false;
            ab.flying = false;
            p.sendAbilitiesUpdate();
        }
        if (p.age % 10 == 0 && (ab.flying || fuel < max))
            p.sendMessage(Text.literal("Топливо: " + (fuel * 100 / max) + "%").formatted(fuel * 100 / max > 25 ? Formatting.AQUA : Formatting.RED), true);
    }

    // ---------------------------------------------------------------- апгрейды
    private static final UUID U_HEART = UUID.fromString("5a1d0001-0000-4000-8000-000000000001");
    private static final UUID U_LEGS = UUID.fromString("5a1d0001-0000-4000-8000-000000000002");
    private static final UUID U_ARMS = UUID.fromString("5a1d0001-0000-4000-8000-000000000003");
    private static final UUID U_SKIN = UUID.fromString("5a1d0001-0000-4000-8000-000000000004");
    private static final UUID U_BONES = UUID.fromString("5a1d0001-0000-4000-8000-000000000005");

    private static void handleUpgBuy(MinecraftServer server, ServerPlayerEntity p, Upgrade u) {
        if (u == null) return;
        if (p.getInventory().count(UPGRADE_DEVICE) == 0) return; // нужен Модуль апгрейда в инвентаре
        BodyState st = BodyState.of(server);
        int[] lv = st.upgrades(p.getUuid(), Upgrade.values().length);
        int cur = lv[u.ordinal()];
        if (cur >= u.max()) return;
        if (u.requiresId != null) {
            Upgrade req = Upgrade.byId(u.requiresId);
            if (req != null && lv[req.ordinal()] < u.requiresLevel) {
                p.sendMessage(Text.literal("Сначала прокачайте «" + req.nom + "» до " + u.requiresLevel + " уровня.").formatted(Formatting.RED));
                return;
            }
        }
        int cost = u.costs[cur];
        if (p.getInventory().count(LIFE_PARTICLE) < cost) {
            p.sendMessage(Text.literal("Не хватает частиц жизни: нужно " + cost + ".").formatted(Formatting.RED));
            return;
        }
        for (ItemStack extra : u.extraCost()) {
            if (p.getInventory().count(extra.getItem()) < extra.getCount()) {
                p.sendMessage(Text.literal("Для этого уровня нужно также: " + extra.getCount() + " × ").append(extra.getName()).formatted(Formatting.RED));
                return;
            }
        }
        removeItems(p, LIFE_PARTICLE, cost);
        for (ItemStack extra : u.extraCost()) removeItems(p, extra.getItem(), extra.getCount());
        lv[u.ordinal()] = cur + 1;
        st.markDirty();
        title(p, Text.literal("Улучшено: " + u.nom).formatted(Formatting.AQUA), Text.literal("Уровень " + (cur + 1) + " из " + u.max()).formatted(Formatting.GRAY));
        p.sendMessage(Text.literal(u.nom + ": уровень " + (cur + 1) + ". " + u.desc).formatted(Formatting.AQUA));
        sync(p);
    }

    private static void setMod(ServerPlayerEntity p, EntityAttribute attr, UUID uuid, String name, double value, EntityAttributeModifier.Operation op) {
        EntityAttributeInstance inst = p.getAttributeInstance(attr);
        if (inst == null) return;
        EntityAttributeModifier cur = inst.getModifier(uuid);
        if (cur != null && cur.getValue() == value) return;
        if (cur != null) inst.removeModifier(uuid);
        if (value != 0) inst.addTemporaryModifier(new EntityAttributeModifier(uuid, name, value, op));
    }

    private static void upgradeTick(ServerPlayerEntity p, BodyState st, Set<BodyPart> m) {
        UUID id = p.getUuid();
        int[] lv = st.upgrades(id, Upgrade.values().length);
        boolean any = false;
        for (int x : lv) if (x > 0) { any = true; break; }
        if (!any) return;
        int a = p.age;
        // сила каждой ветки организма: 0..1 (растёт с каждой ступенью ветки)
        double fSt = Upgrade.frac("stomach", lv), fLi = Upgrade.frac("liver", lv), fKi = Upgrade.frac("kidneys", lv), fLu = Upgrade.frac("lungs", lv);
        double fHe = Upgrade.frac("heart", lv), fEy = Upgrade.frac("eyes", lv), fSk = Upgrade.frac("skin", lv), fBo = Upgrade.frac("bones", lv), fIm = Upgrade.frac("immune", lv);

        // желудок: сам подкармливает (без срабатывания "еды" для пищеварительных болезней)
        if (fSt > 0 && !m.contains(BodyPart.STOMACH)) {
            int interval = (int) Math.max(600, 6000 - fSt * 5400);
            var hm = p.getHungerManager();
            if (a % interval == 0 && hm.getFoodLevel() < 20) { hm.add(1, 0.3f); LAST_FOOD.put(id, hm.getFoodLevel()); }
        }
        // печень: регенерация
        if (fLi > 0 && !m.contains(BodyPart.LIVER)) {
            int interval = (int) Math.max(300, 3600 - fLi * 3300);
            if (a % interval == 0 && p.getHealth() < p.getMaxHealth()) p.heal(1f);
        }

        jetTick(p, lv[Upgrade.JETLEGS.ordinal()], !(m.contains(BodyPart.LEFT_LEG) && m.contains(BodyPart.RIGHT_LEG)));

        if (a % 20 != 0) return;
        int legsL = lv[Upgrade.LEGS.ordinal()], armsL = lv[Upgrade.ARMS.ordinal()];

        setMod(p, EntityAttributes.GENERIC_MAX_HEALTH, U_HEART, "bt_heart", Math.round(fHe * 40) / 2.0, EntityAttributeModifier.Operation.ADDITION);
        boolean legsOk = !(m.contains(BodyPart.LEFT_LEG) && m.contains(BodyPart.RIGHT_LEG));
        setMod(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, U_LEGS, "bt_legs", legsOk ? 0.08 * legsL : 0, EntityAttributeModifier.Operation.MULTIPLY_BASE);
        boolean armsOk = !(m.contains(BodyPart.LEFT_ARM) && m.contains(BodyPart.RIGHT_ARM));
        setMod(p, EntityAttributes.GENERIC_ATTACK_DAMAGE, U_ARMS, "bt_arms", armsOk ? 1.0 * armsL : 0, EntityAttributeModifier.Operation.ADDITION);
        if (armsOk && armsL > 0) effT(p, StatusEffects.HASTE, 60, armsL - 1);
        setMod(p, EntityAttributes.GENERIC_ARMOR, U_SKIN, "bt_skin", Math.round(fSk * 24) / 2.0, EntityAttributeModifier.Operation.ADDITION);
        setMod(p, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, U_BONES, "bt_bones", fBo, EntityAttributeModifier.Operation.ADDITION);

        boolean eyesOk = !(m.contains(BodyPart.LEFT_EYE) && m.contains(BodyPart.RIGHT_EYE));
        if (eyesOk && (fEy >= 0.4 || (fEy >= 0.12 && !p.getWorld().isSkyVisible(p.getBlockPos())))) effT(p, StatusEffects.NIGHT_VISION, 260, 0);

        boolean kidneyOk = !(m.contains(BodyPart.LEFT_KIDNEY) && m.contains(BodyPart.RIGHT_KIDNEY));
        if (kidneyOk && fKi > 0) {
            if (fKi >= 0.12) p.removeStatusEffect(StatusEffects.POISON);
            if (fKi >= 0.35) p.removeStatusEffect(StatusEffects.NAUSEA);
            if (fKi >= 0.65) { p.removeStatusEffect(StatusEffects.HUNGER); p.removeStatusEffect(StatusEffects.WEAKNESS); }
        }
        boolean lungOk = !(m.contains(BodyPart.LEFT_LUNG) && m.contains(BodyPart.RIGHT_LUNG));
        if (fLu > 0 && lungOk && p.isSubmergedIn(FluidTags.WATER)) p.setAir(Math.min(p.getMaxAir(), p.getAir() + (int) (fLu * 30) + 2));

        if (fIm > 0) {
            Map<SideEffect, Long> fx = st.effects(id);
            long extra = (long) (20 * fIm * 4);
            for (Map.Entry<SideEffect, Long> e : fx.entrySet()) e.setValue(e.getValue() - extra);
            if (!fx.isEmpty()) st.markDirty();
        }
    }

    // ---------------------------------------------------------------- очистка памяти, конвертер
    private static void forget(UUID id) {
        LAST_FALL.remove(id); LAST_FOOD.remove(id); LAST_JUMP.remove(id); LAST_WALK.remove(id); NERVE_CD.remove(id);
        LAST_HASH.remove(id); LAST_SANITY.remove(id); LAST_BLADE.remove(id); LAST_DECAY.remove(id); FUEL.remove(id);
        CRAWLERS.remove(id); LAST_FACE.remove(id); NEED_SYNC.remove(id);
        VoiceState.remove(id);
        SideEffect.forget(id);
    }

    private static void forgetAll() {
        LAST_FALL.clear(); LAST_FOOD.clear(); LAST_JUMP.clear(); LAST_WALK.clear(); NERVE_CD.clear(); LAST_HASH.clear(); LAST_SANITY.clear();
        LAST_BLADE.clear(); LAST_DECAY.clear(); FUEL.clear(); CRAWLERS.clear(); LAST_FACE.clear(); NEED_SYNC.clear();
        VoiceState.clear();
        Story.clear();
    }

    /** Синтезатор жизни: алмазы -> частицы жизни по курсу 1:1 (amount = 0 - обменять все). */
    private static void handleConvert(MinecraftServer server, ServerPlayerEntity p, BlockPos pos, int amount) {
        if (!p.getWorld().getBlockState(pos).isOf(LIFE_CONVERTER) || p.squaredDistanceTo(Vec3d.ofCenter(pos)) > 64) return;
        if (amount < 0 || amount > 100000) return;
        int have = p.getInventory().count(Items.DIAMOND);
        int n = amount == 0 ? have : Math.min(amount, have);
        if (n <= 0) {
            p.sendMessage(Text.literal("У вас нет алмазов для обмена.").formatted(Formatting.RED), true);
            return;
        }
        removeItems(p, Items.DIAMOND, n);
        giveParticles(p, n);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1f, 1.2f);
        p.sendMessage(Text.literal("Обменяно: " + n + " алмазов на " + n + " частиц жизни.").formatted(Formatting.GREEN), true);
    }

    // ---------------------------------------------------------------- рассудок, импланты, энциклопедия
    private static void unlock(ServerPlayerEntity p, BodyState st, String key, String title) {
        if (st.unlock(p.getUuid(), key)) p.sendMessage(Text.literal("[Энциклопедия] Новая запись: " + title).formatted(Formatting.AQUA));
    }

    private static boolean nearCampfire(World w, BlockPos c) {
        for (int dx = -4; dx <= 4; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -4; dz <= 4; dz++) {
                    var s = w.getBlockState(c.add(dx, dy, dz));
                    if ((s.isOf(Blocks.CAMPFIRE) || s.isOf(Blocks.SOUL_CAMPFIRE)) && s.get(CampfireBlock.LIT)) return true;
                }
        return false;
    }

    /** Рассудок (0..100%): падает в темноте, рядом с монстрами и при болезнях; растёт у костра, во сне и в компании. */
    private static void sanityTick(ServerPlayerEntity p, BodyState st, Set<BodyPart> m) {
        if (p.isCreative() || p.isSpectator() || p.age % 20 != 0) return;
        UUID id = p.getUuid();
        var w = p.getWorld();
        boolean lantern = st.hasImplant(id, "lantern"), neuro = st.hasImplant(id, "neurochip");
        int delta = 0;
        int light = w.getLightLevel(p.getBlockPos());
        if (light <= 3 && !lantern) delta -= 3; else if (light >= 8) delta += 2;
        int mobs = w.getEntitiesByClass(HostileEntity.class, p.getBoundingBox().expand(10), e -> true).size();
        delta -= Math.min(6, mobs * 2);
        delta -= Math.min(4, st.effects(id).size());
        if (p.isSleeping()) delta += 25;
        int others = 0;
        for (PlayerEntity o : w.getPlayers()) if (o != p && !o.isSpectator() && o.squaredDistanceTo(p) < 64) others++;
        delta += Math.min(6, others * 3);
        if (nearCampfire(w, p.getBlockPos())) delta += 6;
        if (neuro) { if (delta < 0) delta /= 2; delta += 1; }
        int cur = Math.max(0, Math.min(1000, st.sanity(id) + delta));
        st.setSanity(id, cur);

        int a = p.age;
        if (cur < 500 && a % 600 == 0 && p.getRandom().nextDouble() < 0.5) SideEffect.whisper(p);
        if (cur < 300 && a % 400 == 0) { effT(p, StatusEffects.DARKNESS, 80, 0); SideEffect.play(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0f, 0.8f); }
        if (cur < 150 && a % 600 == 0 && p.getRandom().nextDouble() < 0.4 && SideEffect.spawnMonsters(p)) bar(p, "Они идут за вами...");
        if (cur <= 0 && !st.effects(id).containsKey(SideEffect.PSYCHOSIS)) {
            long now = p.getServer().getOverworld().getTime();
            st.setEffect(id, SideEffect.PSYCHOSIS, now + 20L * SideEffect.PSYCHOSIS.seconds);
            unlock(p, st, "fx:PSYCHOSIS", SideEffect.PSYCHOSIS.title);
            st.setSanity(id, 150);
            p.sendMessage(Text.literal("Рассудок иссяк. Психоз: " + SideEffect.PSYCHOSIS.message).formatted(Formatting.DARK_RED));
            sync(p);
            cur = 150;
        }
        int shown = cur / 10;
        Integer ls = LAST_SANITY.get(id);
        if (ls == null || ls != shown) {
            LAST_SANITY.put(id, shown);
            PacketByteBuf b = PacketByteBufs.create();
            b.writeVarInt(cur);
            ServerPlayNetworking.send(p, SANITY_PACKET, b);
        }
    }

    private static void handleImplantBuy(MinecraftServer server, ServerPlayerEntity p, Implant im) {
        if (im == null) return;
        if (p.getInventory().count(UPGRADE_DEVICE) == 0) return; // нужен Модуль апгрейда в инвентаре
        BodyState st = BodyState.of(server);
        UUID id = p.getUuid();
        int cur = st.implantLevel(id, im.id);
        if (cur >= im.maxLevel()) return;
        int particles = im.particles[cur];
        var inv = p.getInventory();
        boolean ok = inv.count(LIFE_PARTICLE) >= particles;
        for (ItemStack need : im.cost()) if (inv.count(need.getItem()) < need.getCount()) ok = false;
        if (!ok) {
            p.sendMessage(Text.literal("Не хватает материалов для «" + im.nom + "» (уровень " + (cur + 1) + "). Нужно: " + (particles > 0 ? particles + " ч. жизни, " : "")
                    + costText(im)).formatted(Formatting.RED));
            return;
        }
        removeItems(p, LIFE_PARTICLE, particles);
        for (ItemStack need : im.cost()) removeItems(p, need.getItem(), need.getCount());
        if (cur == 0) for (Implant o : Implant.values()) if (o != im && o.slot.equals(im.slot)) st.setImplantLevel(id, o.id, 0); // один имплант на слот
        st.setImplantLevel(id, im.id, cur + 1);
        title(p, Text.literal(cur == 0 ? "Имплант установлен" : "Имплант улучшен").formatted(Formatting.AQUA),
                Text.literal(im.nom + (im.maxLevel() > 1 ? ", уровень " + (cur + 1) + " из " + im.maxLevel() : "")).formatted(Formatting.GRAY));
        p.sendMessage(Text.literal(im.nom + ": " + im.desc).formatted(Formatting.AQUA));
        VoiceState.update(p, st.functional(id).contains(BodyPart.MOUTH), st.implantLevel(id, "mech_ear"));
        sync(p);
    }

    private static String costText(Implant im) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack k : im.cost()) sb.append(sb.length() > 0 ? ", " : "").append(k.getCount()).append(" × ").append(k.getName().getString());
        return sb.toString();
    }

    // ---------------------------------------------------------------- таймер распада (режим Перерождение)
    private static void sendDecay(ServerPlayerEntity p, int value) {
        Integer last = LAST_DECAY.get(p.getUuid());
        if (last != null && last == value) return;
        LAST_DECAY.put(p.getUuid(), value);
        PacketByteBuf b = PacketByteBufs.create();
        b.writeVarInt(value + 2); // -2 (отключён) и -1 (нет режима) сдвинуты в неотрицательные
        ServerPlayNetworking.send(p, DECAY_PACKET, b);
    }

    /** Раз в 15 минут режим Перерождение отнимает случайную конечность или выдаёт болезнь. Отключается имплантом "Хроно-стабилизатор". */
    private static void decayTick(ServerPlayerEntity p, BodyState st) {
        UUID id = p.getUuid();
        if (!st.isDevelop(id)) { sendDecay(p, -1); return; }
        if (st.hasImplant(id, "chrono_core")) { sendDecay(p, -2); return; }
        int left = st.decay(id) - 1;
        if (left <= 0) { applyDecay(p, st); left = BodyState.DECAY_TICKS; }
        st.setDecay(id, left);
        if (p.age % 200 == 0) st.markDirty();
        if (p.age % 20 == 0) sendDecay(p, left / 20);
    }

    private static void applyDecay(ServerPlayerEntity p, BodyState st) {
        UUID id = p.getUuid();
        var r = p.getRandom();
        Set<BodyPart> missing = st.get(id);
        List<BodyPart> candidates = new ArrayList<>();
        for (BodyPart bp : BodyPart.values()) if (!missing.contains(bp)) candidates.add(bp);   // любая часть тела
        if (!candidates.isEmpty() && r.nextDouble() < 0.5) {
            BodyPart bp = candidates.get(r.nextInt(candidates.size()));
            st.add(id, bp);
            st.removePros(id, bp);
            unlock(p, st, "part:" + bp.id, bp.nom);
            p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.PLAYERS, 1.2f, 0.7f);
            title(p, Text.literal("ТАЙМЕР РАСПАДА").formatted(Formatting.DARK_RED), Text.literal("Вы лишились " + bp.gen).formatted(Formatting.GRAY));
            p.sendMessage(Text.literal("Таймер распада: " + bp.consequence).formatted(Formatting.RED));
            sync(p);
            broadcastLimbs(p.getServer(), p);
        } else {
            List<SideEffect> common = new ArrayList<>();
            for (SideEffect x : SideEffect.values()) if (!x.isRare()) common.add(x);
            SideEffect se = common.get(r.nextInt(common.size()));
            st.setEffect(id, se, p.getServer().getOverworld().getTime() + se.seconds * 20L);
            unlock(p, st, "fx:" + se.name(), se.title);
            title(p, Text.literal("ТАЙМЕР РАСПАДА").formatted(Formatting.DARK_RED), Text.literal("Болезнь: " + se.title).formatted(Formatting.GRAY));
            p.sendMessage(Text.literal("Таймер распада: " + se.title + ". " + se.message).formatted(Formatting.YELLOW));
            sync(p);
        }
    }

    private static void implantTick(ServerPlayerEntity p, BodyState st, Set<BodyPart> m) {
        if (p.age % 20 != 0) return;
        UUID id = p.getUuid();
        boolean eyesOk = !(m.contains(BodyPart.LEFT_EYE) && m.contains(BodyPart.RIGHT_EYE));
        int eye = st.implantLevel(id, "cyber_eye");
        if (eye > 0 && eyesOk && st.isEspOn(id) && p.getWorld() instanceof ServerWorld w) {
            // контур врагов сквозь стены; руды подсвечивает клиент. Радиус по уровням: 4 / 8 / 16 блоков
            int r = new int[]{0, 4, 8, 16}[eye];
            for (HostileEntity h : w.getEntitiesByClass(HostileEntity.class, p.getBoundingBox().expand(r), e -> true))
                h.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 40, 0, true, false, false));
        }
        if (st.hasImplant(id, "lantern") && eyesOk && p.getWorld().getLightLevel(p.getBlockPos()) < 8) effT(p, StatusEffects.NIGHT_VISION, 260, 0);
        if (st.hasImplant(id, "titan_bones")) effT(p, StatusEffects.RESISTANCE, 60, 0);
    }

    /** Рентген кибер-глаза: ближайшие руды в радиусе 6 блоков. */
    private static void orePing(ServerPlayerEntity p, ServerWorld w) {
        List<TagKey<Block>> tags = List.of(BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES, BlockTags.GOLD_ORES, BlockTags.IRON_ORES,
                BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES, BlockTags.COAL_ORES, BlockTags.COPPER_ORES);
        String[] names = {"Алмазы", "Изумруды", "Золото", "Железо", "Красная пыль", "Лазурит", "Уголь", "Медь"};
        int[] best = new int[tags.size()];
        Arrays.fill(best, 99);
        BlockPos c = p.getBlockPos();
        for (int dx = -6; dx <= 6; dx++)
            for (int dy = -6; dy <= 6; dy++)
                for (int dz = -6; dz <= 6; dz++) {
                    BlockState s = w.getBlockState(c.add(dx, dy, dz));
                    if (s.isAir()) continue;
                    int d = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                    for (int i = 0; i < tags.size(); i++) if (d < best[i] && s.isIn(tags.get(i))) best[i] = d;
                }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.size(); i++) if (best[i] < 99) sb.append(sb.length() > 0 ? ", " : "").append(names[i]).append(" ").append(best[i]).append(" бл.");
        if (sb.length() > 0) p.sendMessage(Text.literal("Рентген: " + sb).formatted(Formatting.AQUA), true);
    }

    /** Рука-клинок: удар пустой рукой добавляет урон клинком. */
    private static void bladeHit(ServerPlayerEntity p, Entity target) {
        if (!(target instanceof LivingEntity le)) return;
        BodyState st = BodyState.of(p.getServer());
        UUID id = p.getUuid();
        if (!st.hasImplant(id, "blade_arm") || !p.getMainHandStack().isEmpty()) return;
        Set<BodyPart> m = st.functional(id);
        if (m.contains(BodyPart.LEFT_ARM) && m.contains(BodyPart.RIGHT_ARM)) return;
        Integer last = LAST_BLADE.get(id);
        if (last != null && p.age >= last && p.age - last < 10) return;
        LAST_BLADE.put(id, p.age);
        le.damage(p.getDamageSources().playerAttack(p), 6f);
    }

    // ---------------------------------------------------------------- оголённый нерв
    private static void nerveHit(ServerPlayerEntity p, String what) {
        Integer last = NERVE_CD.get(p.getUuid());
        if (last != null && p.age >= last && p.age - last < 15) return;
        NERVE_CD.put(p.getUuid(), p.age);
        p.damage(p.getDamageSources().generic(), 2f);
        p.sendMessage(Text.literal("Оголённый нерв! Больно: " + what + ".").formatted(Formatting.RED), true);
    }

    private static boolean hasEffect(Map<SideEffect, Long> fx, SideEffect e) { return fx.containsKey(e); }

    /** Действие рукой: если на этой руке оголённый нерв - урон. */
    private static void nerveUse(ServerPlayerEntity p, Hand hand, String what) {
        Map<SideEffect, Long> fx = BodyState.of(p.getServer()).effects(p.getUuid());
        if (fx.isEmpty()) return;
        boolean leftMain = p.getMainArm() == Arm.LEFT;
        boolean leftHand = (hand == Hand.MAIN_HAND) == leftMain;
        if (hasEffect(fx, leftHand ? SideEffect.NERVE_LEFT_ARM : SideEffect.NERVE_RIGHT_ARM)) nerveHit(p, what);
    }

    private static void nerveTick(ServerPlayerEntity p, Map<SideEffect, Long> fx) {
        UUID id = p.getUuid();
        var stats = p.getStatHandler();
        int jumps = stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.JUMP));
        int walk = stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.WALK_ONE_CM))
                + stats.getStat(Stats.CUSTOM.getOrCreateStat(Stats.SPRINT_ONE_CM));
        int lastJ = LAST_JUMP.getOrDefault(id, jumps);
        int lastW = LAST_WALK.getOrDefault(id, walk);
        LAST_JUMP.put(id, jumps);
        boolean legL = hasEffect(fx, SideEffect.NERVE_LEFT_LEG), legR = hasEffect(fx, SideEffect.NERVE_RIGHT_LEG);
        if (legL || legR) {
            String leg = legL ? "левая нога" : "правая нога";
            if (jumps > lastJ) nerveHit(p, "прыжок (" + leg + ")");
            if (walk - lastW >= 300) { nerveHit(p, "шаги (" + leg + ")"); LAST_WALK.put(id, walk); }
            else LAST_WALK.put(id, lastW);
        } else {
            LAST_WALK.put(id, walk);
        }
        // держите предмет в больной руке
        if (p.age % 40 == 0) {
            boolean leftMain = p.getMainArm() == Arm.LEFT;
            ItemStack leftStack = leftMain ? p.getMainHandStack() : p.getOffHandStack();
            ItemStack rightStack = leftMain ? p.getOffHandStack() : p.getMainHandStack();
            if (hasEffect(fx, SideEffect.NERVE_LEFT_ARM) && !leftStack.isEmpty()) nerveHit(p, "вы держите предмет в левой руке");
            if (hasEffect(fx, SideEffect.NERVE_RIGHT_ARM) && !rightStack.isEmpty()) nerveHit(p, "вы держите предмет в правой руке");
        }
    }

    // ---------------------------------------------------------------- последствия
    private static void eff(ServerPlayerEntity p, StatusEffect e, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, 60, amp, true, false, true));
    }
    private static void effT(ServerPlayerEntity p, StatusEffect e, int ticks, int amp) {
        p.addStatusEffect(new StatusEffectInstance(e, ticks, amp, true, false, true));
    }
    private static void bar(ServerPlayerEntity p, String s) { p.sendMessage(Text.literal(s).formatted(Formatting.RED), true); }

    private static void tickPlayer(MinecraftServer server, ServerPlayerEntity p) {
        BodyState st = BodyState.of(server);
        UUID id = p.getUuid();
        long now = server.getOverworld().getTime();
        st.prune(id, now);
        Set<BodyPart> m = st.functional(id);   // без частей, заменённых протезом
        Set<BodyPart> pr = st.pros(id);
        if (p.age % 20 == 0) {
            // снимок для Simple Voice Chat (голосовой поток работает в другом потоке)
            VoiceState.update(p, m.contains(BodyPart.MOUTH), st.implantLevel(id, "mech_ear"));
            devTick(p, st);
        }
        upgradeTick(p, st, m);
        decayTick(p, st);
        // звуки тела: сердцебиение при низком здоровье
        if (!p.isCreative() && p.getHealth() <= 6f && p.age % (p.getHealth() <= 3f ? 20 : 35) == 0)
            SideEffect.play(p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0f, 1f);
        sanityTick(p, st, m);
        implantTick(p, st, m);

        // отслеживаем приём пищи
        int food = p.getHungerManager().getFoodLevel();
        Integer prev = LAST_FOOD.get(id);
        if (prev != null && food > prev && !m.isEmpty()) food = onEat(p, m, prev, food);
        LAST_FOOD.put(id, food);

        Map<SideEffect, Long> fx = st.effects(id);
        for (SideEffect e : fx.keySet()) e.tick(p);
        nerveTick(p, fx);
        if (m.isEmpty() && pr.isEmpty()) { LAST_FALL.remove(id); CRAWLERS.remove(id); return; }

        int legs = cnt(m, BodyPart.LEFT_LEG, BodyPart.RIGHT_LEG);
        int feet = cnt(m, BodyPart.LEFT_FOOT, BodyPart.RIGHT_FOOT);
        int lungs = cnt(m, BodyPart.LEFT_LUNG, BodyPart.RIGHT_LUNG);
        int kidneys = cnt(m, BodyPart.LEFT_KIDNEY, BodyPart.RIGHT_KIDNEY);
        int eyes = cnt(m, BodyPart.LEFT_EYE, BodyPart.RIGHT_EYE);
        boolean la = m.contains(BodyPart.LEFT_ARM), ra = m.contains(BodyPart.RIGHT_ARM);
        if (legs == 2) CRAWLERS.add(id); else CRAWLERS.remove(id);   // без обеих ног - только ползком

        // хромота / ступни: урон при падении с небольшой высоты (ванильный урон начинается с 3 блоков)
        if (legs > 0 || feet > 0) {
            float last = LAST_FALL.getOrDefault(id, 0f);
            if (p.isOnGround()) {
                float threshold = legs > 0 ? 1.4f : 2.2f;
                if (last >= threshold && last < 3.2f && !p.getAbilities().creativeMode) {
                    p.damage(p.getDamageSources().fall(), Math.min(5f, 1f + legs + feet));
                    p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundCategory.PLAYERS, 1f, 0.8f);
                }
                LAST_FALL.put(id, 0f);
            } else {
                LAST_FALL.put(id, Math.max(last, p.fallDistance));
            }
        }

        // хриплое дыхание без лёгких
        if (lungs >= 1 && p.age % (lungs == 2 ? 70 : 130) == 0) SideEffect.play(p, SoundEvents.ENTITY_PLAYER_BREATH, 0f, 0.65f);
        boolean noSprint = lungs > 0 || m.contains(BodyPart.TRACHEA);
        if (noSprint) p.setSprinting(false);

        if (la && !p.getOffHandStack().isEmpty()) {
            p.dropItem(p.getOffHandStack().copy(), false);
            p.getInventory().offHand.set(0, ItemStack.EMPTY);
        }
        if (la && ra && !p.getMainHandStack().isEmpty()) {
            p.dropItem(p.getMainHandStack().copy(), false);
            p.getInventory().setStack(p.getInventory().selectedSlot, ItemStack.EMPTY);
        }

        int a = p.age;
        if (m.contains(BodyPart.DIAPHRAGM) && a % 300 == 0) {
            p.setVelocity(p.getVelocity().add(0, 0.3, 0));
            p.velocityModified = true;
            bar(p, "Ик!");
        }
        if (m.contains(BodyPart.SPLEEN) && a % 2400 == 0 && p.getRandom().nextDouble() < 0.4) {
            effT(p, StatusEffects.POISON, 80, 0);
            bar(p, "Вы подхватили инфекцию.");
        }
        if (kidneys == 2 && a % 600 == 0) effT(p, StatusEffects.POISON, 60, 0);
        if (m.contains(BodyPart.NOSE) && m.contains(BodyPart.MOUTH) && a % 80 == 0 && p.getHealth() > 4f) {
            p.damage(p.getDamageSources().drown(), 1f);
            bar(p, "Нечем дышать!");
        }
        if (m.contains(BodyPart.BRONCHI) && p.isSprinting() && a % 40 == 0) effT(p, StatusEffects.SLOWNESS, 100, 1);
        if (m.contains(BodyPart.LIVER) && a % 600 == 0) effT(p, StatusEffects.NAUSEA, 120, 0);

        if (a % 20 != 0) return;

        // протезы работают, но хуже родных конечностей
        if (pr.contains(BodyPart.LEFT_ARM) || pr.contains(BodyPart.RIGHT_ARM)) eff(p, StatusEffects.MINING_FATIGUE, 0);
        if (pr.contains(BodyPart.LEFT_LEG) || pr.contains(BodyPart.RIGHT_LEG)) eff(p, StatusEffects.SLOWNESS, 0);

        if (p.isSubmergedIn(FluidTags.WATER)) {
            int drain = (m.contains(BodyPart.NOSE) ? 20 : 0) + lungs * 20;
            if (drain > 0) p.setAir(p.getAir() - drain);
        }
        if (legs == 1) eff(p, StatusEffects.SLOWNESS, 0);
        if (legs == 2) { eff(p, StatusEffects.SLOWNESS, 3); eff(p, StatusEffects.JUMP_BOOST, 128); }
        if (ra) { eff(p, StatusEffects.MINING_FATIGUE, 1); eff(p, StatusEffects.WEAKNESS, 1); }
        if (eyes == 2) eff(p, StatusEffects.BLINDNESS, 0);
        if (kidneys >= 1) eff(p, StatusEffects.HUNGER, 0);
        if (kidneys == 2) { eff(p, StatusEffects.HUNGER, 1); eff(p, StatusEffects.WEAKNESS, 0); }
        if (lungs >= 1) eff(p, StatusEffects.WEAKNESS, 0);
        if (lungs == 2) { eff(p, StatusEffects.WEAKNESS, 1); eff(p, StatusEffects.SLOWNESS, 1); }
        if (m.contains(BodyPart.TRACHEA)) eff(p, StatusEffects.SLOWNESS, 0);
        if (m.contains(BodyPart.THYROID)) { eff(p, StatusEffects.SLOWNESS, 0); eff(p, StatusEffects.MINING_FATIGUE, 0); }
        if (feet == 2) eff(p, StatusEffects.SLOWNESS, 0);
    }

    private static int cnt(Set<BodyPart> m, BodyPart a, BodyPart b) { return (m.contains(a) ? 1 : 0) + (m.contains(b) ? 1 : 0); }

    /** Последствия еды для пищеварительных органов. Возвращает итоговый уровень сытости. */
    private static int onEat(ServerPlayerEntity p, Set<BodyPart> m, int prev, int food) {
        int gain = food - prev;
        var hm = p.getHungerManager();
        if (m.contains(BodyPart.ESOPHAGUS)) {
            hm.setFoodLevel(prev);
            effT(p, StatusEffects.NAUSEA, 200, 0);
            effT(p, StatusEffects.HUNGER, 200, 0);
            bar(p, "Пища не дошла до желудка...");
            return prev;
        }
        double mult = 1.0;
        if (m.contains(BodyPart.STOMACH)) mult *= 0.5;
        if (m.contains(BodyPart.SMALL_INTESTINE)) mult *= 0.34;
        if (m.contains(BodyPart.TONGUE)) mult *= 0.7;
        int result = food;
        if (mult < 1.0) { result = prev + (int) Math.round(gain * mult); hm.setFoodLevel(result); }
        if (m.contains(BodyPart.LARGE_INTESTINE) && p.getRandom().nextDouble() < 0.5) {
            effT(p, StatusEffects.NAUSEA, 100, 0); effT(p, StatusEffects.HUNGER, 100, 0);
            bar(p, "Желудок бунтует.");
        }
        if (m.contains(BodyPart.GALLBLADDER)) effT(p, StatusEffects.NAUSEA, 100, 0);
        if (m.contains(BodyPart.PANCREAS)) { effT(p, StatusEffects.SLOWNESS, 300, 1); effT(p, StatusEffects.WEAKNESS, 300, 0); }
        return result;
    }
}
