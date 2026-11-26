package gg.laydzr.plus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import java.math.BigDecimal;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.logging.Logger;

public final class LaydzrPlusClient implements ClientModInitializer {
    public static final String MOD_ID = "laydzr_plus";
    public static final Logger LOGGER = Logger.getLogger("LAYDZR+");
    public static Path CONFIG_DIR;
    private static KeyMapping openTrackerKey;
    private static KeyMapping spotifyConnectKey;
    private static int lastAlertedLossStreak;

    @Override
    public void onInitializeClient() {
        CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID);
        try {
            java.nio.file.Files.createDirectories(CONFIG_DIR);
            PortfolioLedger.load(CONFIG_DIR.resolve("portfolio.csv"));
            MusicPreferences.load(CONFIG_DIR.resolve("music.properties"));
            SpotifyBridge.initialize(CONFIG_DIR);
            SpotifyBridge.setClientId(MusicPreferences.spotifyClientId());
        } catch (Exception e) {
            LOGGER.severe("Could not initialize portfolio ledger: " + e.getMessage());
        }

        openTrackerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.laydzr_plus.open_tracker",
                GLFW.GLFW_KEY_L,
                KeyMapping.Category.MISC
        ));

        spotifyConnectKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.laydzr_plus.spotify_connect", GLFW.GLFW_KEY_O, KeyMapping.Category.MISC));

        // DonutSMP's payment notifications are server game/system messages.
        // Only exact recognized payment patterns are recorded.
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                TrackerState.INSTANCE.accept(message.getString()));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (spotifyConnectKey.consumeClick()) {
                SpotifyBridge.connect();
                if (client.player != null) client.player.displayClientMessage(
                        Component.literal("§bLAYDZR+ §7" + SpotifyBridge.status()), false);
            }
            while (openTrackerKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new TrackerScreen());
                else if (client.screen instanceof TrackerScreen) client.setScreen(null);
            }

            int streak = TrackerState.INSTANCE.stats().currentLossStreak();
            int threshold = TrackerState.INSTANCE.lossAlertThreshold();
            if (streak < threshold) {
                lastAlertedLossStreak = 0;
            } else if (streak >= threshold && streak > lastAlertedLossStreak && client.player != null) {
                client.player.displayClientMessage(
                        Component.literal("§cLAYDZR+ §7Loss streak reached " + threshold + " outgoing payments."),
                        false);
                lastAlertedLossStreak = streak;
            }
        });

        LaydzrHud.register();
        AuctionHud.register();
        registerAuctionCommands();

        LOGGER.info("LAYDZR+ initialized. Press L for tracker, O to connect Spotify.");
    }

    private static void registerAuctionCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("laydzr").then(literal("auction")
                        .then(literal("start")
                                .then(argument("quantity", IntegerArgumentType.integer(1))
                                        .then(argument("buyTotal", DoubleArgumentType.doubleArg(0))
                                                .then(argument("item", StringArgumentType.greedyString())
                                                        .executes(ctx -> {
                                                            AuctionState.start(StringArgumentType.getString(ctx, "item"),
                                                                    IntegerArgumentType.getInteger(ctx, "quantity"),
                                                                    BigDecimal.valueOf(DoubleArgumentType.getDouble(ctx, "buyTotal")));
                                                            ctx.getSource().sendFeedback(Component.literal("LAYDZR Auction: tracking " + StringArgumentType.getString(ctx, "item")));
                                                            return 1;
                                                        })))))
                        .then(literal("bid")
                                .then(argument("amount", DoubleArgumentType.doubleArg(0))
                                        .then(argument("player", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    try {
                                                        AuctionState.bid(StringArgumentType.getString(ctx, "player"),
                                                                BigDecimal.valueOf(DoubleArgumentType.getDouble(ctx, "amount")));
                                                        ctx.getSource().sendFeedback(Component.literal("LAYDZR Auction: bid updated"));
                                                        return 1;
                                                    } catch (IllegalStateException | IllegalArgumentException e) {
                                                        ctx.getSource().sendError(Component.literal(e.getMessage()));
                                                        return 0;
                                                    }
                                                }))))
                        .then(literal("worth").then(argument("amount", DoubleArgumentType.doubleArg(0)).executes(ctx -> {
                            try {
                                AuctionState.worth(BigDecimal.valueOf(DoubleArgumentType.getDouble(ctx, "amount")));
                                ctx.getSource().sendFeedback(Component.literal("LAYDZR Auction: estimated item worth updated"));
                                return 1;
                            } catch (IllegalStateException | IllegalArgumentException e) {
                                ctx.getSource().sendError(Component.literal(e.getMessage()));
                                return 0;
                            }
                        })))
                        .then(literal("sold").then(argument("sellTotal", DoubleArgumentType.doubleArg(0)).executes(ctx -> {
                            try {
                                BigDecimal result = AuctionState.finish(BigDecimal.valueOf(DoubleArgumentType.getDouble(ctx, "sellTotal")));
                                ctx.getSource().sendFeedback(Component.literal("LAYDZR flip closed. Profit/Loss: $" + result.toPlainString()));
                                return 1;
                            } catch (Exception e) {
                                ctx.getSource().sendError(Component.literal("Could not close auction: " + e.getMessage()));
                                return 0;
                            }
                        })))
                        .then(literal("clear").executes(ctx -> {
                            AuctionState.clear();
                            ctx.getSource().sendFeedback(Component.literal("LAYDZR Auction HUD cleared"));
                            return 1;
                        }))));
    }
}
