package dev.local.vcmusic.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
                .header("User-Agent", "Mozilla/5.0 (compatible; vcmusic/0.1)")
                .GET()
                .build();
        Identifier id = Identifier.fromNamespaceAndPath("vcmusic", "art_" + Integer.toHexString(url.hashCode()));
        // A failed URL stays in ASKED so a dead thumbnail is not retried every frame.
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        register(url, id, response.body());
                    }
                })
                .exceptionally(e -> null);
        return null;
    }

    /** Draws the whole thumbnail, centre-cropped to a square, at the given size. */
    public static void draw(GuiGraphicsExtractor graphics, Art art, int x, int y, int size) {
        int side = Math.min(art.width(), art.height());
        int u = (art.width() - side) / 2;
        int v = (art.height() - side) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, art.id(), x, y,
                (float) u, (float) v, size, size, side, side, art.width(), art.height());
    }

    private static void register(String url, Identifier id, byte[] bytes) {
        Minecraft.getInstance().execute(() -> {
            NativeImage image = null;
            try {
                // NativeImage.read only accepts PNG, so decode the JPEG thumbnail and re-encode it.
                BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes));
                if (decoded == null) {
                    return;
                }
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                ImageIO.write(decoded, "png", png);
                image = NativeImage.read(png.toByteArray());
                Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(() -> "vcmusic-art", image));
                LOADED.put(url, new Art(id, image.getWidth(), image.getHeight()));
            } catch (IOException e) {
                if (image != null) {
                    image.close();
                }
                System.err.println("[vcmusic] album art failed: " + e);
            }
        });
    }
}
