package dev.local.vcmusic;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class MusicCommands {

    private MusicCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> dispatcher.register(
                ClientCommands.literal("music")
                        .then(ClientCommands.literal("panel").executes(c -> {
                            Minecraft.getInstance().gui.setScreen(new dev.local.vcmusic.gui.MusicScreen());
                            return 1;
                        }))
                        .then(ClientCommands.literal("play")
                                .then(ClientCommands.argument("query", StringArgumentType.greedyString())
                                        .executes(c -> {
                                            String query = StringArgumentType.getString(c, "query");
                                            c.getSource().sendFeedback(Component.literal("Searching: " + query));
                                            CompletableFuture.supplyAsync(() -> {
                                                try {
                                                    return YtDlp.search(MusicConfig.INSTANCE, query, 1);
                                                } catch (Exception e) {
                                                    return List.<Track>of();
                                                }
                                            }).thenAccept(tracks -> Minecraft.getInstance().execute(() -> {
                                                if (tracks.isEmpty()) {
                                                    c.getSource().sendError(Component.literal("No result for: " + query));
                                                    return;
                                                }
                                                Track track = tracks.get(0);
                                                MusicPlayer.INSTANCE.enqueue(track);
                                                if (!MusicPlayer.INSTANCE.lastError().isEmpty()) {
                                                    c.getSource().sendError(Component.literal(MusicPlayer.INSTANCE.lastError()));
                                                    return;
                                                }
                                                c.getSource().sendFeedback(Component.literal("Queued: " + track.title()));
                                            }));
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("pause").executes(c -> {
                            MusicPlayer.INSTANCE.setPaused(true);
                            c.getSource().sendFeedback(Component.literal("Paused"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("resume").executes(c -> {
                            MusicPlayer.INSTANCE.setPaused(false);
                            c.getSource().sendFeedback(Component.literal("Resumed"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("skip").executes(c -> {
                            MusicPlayer.INSTANCE.next();
                            c.getSource().sendFeedback(Component.literal("Skipped"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("stop").executes(c -> {
                            MusicPlayer.INSTANCE.stop();
                            c.getSource().sendFeedback(Component.literal("Stopped"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("vol")
                                .then(ClientCommands.argument("percent", IntegerArgumentType.integer(0, 100))
                                        .executes(c -> {
                                            int percent = IntegerArgumentType.getInteger(c, "percent");
                                            MusicConfig.INSTANCE.volume = percent / 100F;
                                            MusicConfig.INSTANCE.save();
                                            c.getSource().sendFeedback(Component.literal("Music volume: " + percent + "%"));
                                            return 1;
                                        })))
                        .then(ClientCommands.literal("save").executes(c -> {
                            Track current = MusicPlayer.INSTANCE.current();
                            if (current == null) {
                                c.getSource().sendError(Component.literal("Nothing playing"));
                                return 0;
                            }
                            boolean added = Library.INSTANCE.add(current);
                            c.getSource().sendFeedback(Component.literal(added ? "Saved: " + current.title() : "Already saved"));
                            return 1;
                        }))
                        .then(ClientCommands.literal("list").executes(c -> {
                            if (Library.INSTANCE.songs.isEmpty()) {
                                c.getSource().sendFeedback(Component.literal("Library is empty"));
                                return 0;
                            }
                            for (Track track : Library.INSTANCE.songs) {
                                c.getSource().sendFeedback(Component.literal(track.display()));
                            }
                            return 1;
                        }))
        ));
    }
}
