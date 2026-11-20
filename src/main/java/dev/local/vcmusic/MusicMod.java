package dev.local.vcmusic;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.sdl.SDLScancode;

public final class MusicMod implements ClientModInitializer {

    public static final String MOD_ID = "vcmusic";

    public static KeyMapping openPanel;

    @Override
    public void onInitializeClient() {
        MusicConfig.load();
        Library.load();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, MOD_ID));
        openPanel = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.vcmusic.panel", SDLScancode.SDL_SCANCODE_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openPanel.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new dev.local.vcmusic.gui.MusicScreen());
                }
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            MusicPlayer.INSTANCE.stop();
            MusicPlayer.INSTANCE.resetLocalChannel();
        });

        MusicCommands.register();
    }
}
