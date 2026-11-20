package dev.local.vcmusic.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Downloads YouTube thumbnails once and registers them as GUI textures. */
public final class AlbumArt {

    public record Art(Identifier id, int width, int height) {
    }

    private static final int MAX_TEXTURES = 40;
    private static final Map<String, Art> LOADED = new ConcurrentHashMap<>();
    private static final Set<String> ASKED = ConcurrentHashMap.newKeySet();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private AlbumArt() {
    }

    /** Returns the loaded texture and its size, or null while it is still downloading. */
    public static Art get(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        Art loaded = LOADED.get(url);
        if (loaded != null || !ASKED.add(url) || LOADED.size() >= MAX_TEXTURES) {
            return loaded;
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            return null;
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        Identifier id = Identifier.fromNamespaceAndPath("vcmusic", "art_" + Integer.toHexString(url.hashCode()));
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        register(url, id, response.body());
                    } else {
                        ASKED.remove(url);
                    }
                })
                .exceptionally(e -> {
                    ASKED.remove(url);
                    return null;
                });
        return null;
    }

    private static void register(String url, Identifier id, byte[] bytes) {
        Minecraft.getInstance().execute(() -> {
            try {
                NativeImage image = NativeImage.read(bytes);
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "vcmusic-art", image));
                LOADED.put(url, new Art(id, image.getWidth(), image.getHeight()));
            } catch (IOException e) {
                ASKED.remove(url);
            }
        });
    }
}
