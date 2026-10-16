package com.fleoxxzy.mcpvpBanAnimation.replica;

import com.fleoxxzy.mcpvpBanAnimation.scheduler.TaskScheduler;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Average shirt/pants/shoe colors of a skin, so the ArmorStand fallback can wear leather armor
 * dyed to match. Skins are downloaded from {@code textures.minecraft.net} on the async scheduler
 * and cached per URL. If the download or decoding fails, the default colors are used.
 */
public final class SkinColors {

    /** Colors used when the skin is unknown (a Steve-like outfit). */
    public static final Palette DEFAULT = new Palette(0x00A8A8, 0x3A3AB0, 0x4A4A4A);

    private static final int MAX_BYTES = 64 * 1024;

    private final TaskScheduler scheduler;
    private final Logger logger;
    private final Map<String, CompletableFuture<Palette>> cache = new ConcurrentHashMap<>();

    public SkinColors(TaskScheduler scheduler, Logger logger) {
        this.scheduler = scheduler;
        this.logger = logger;
    }

    /** RGB colors for the chestplate, leggings and boots. */
    public record Palette(int chest, int legs, int boots) {
    }

    /** Starts (or reuses) the download. The future never completes exceptionally. */
    public CompletableFuture<Palette> get(String skinUrl) {
        if (skinUrl == null || !isTrustedHost(skinUrl)) {
            return CompletableFuture.completedFuture(DEFAULT);
        }
        return cache.computeIfAbsent(skinUrl, url -> {
            CompletableFuture<Palette> future = new CompletableFuture<>();
            scheduler.runAsync(() -> future.complete(download(url)));
            return future;
        });
    }

    /** The palette if it is already known, otherwise null. */
    public Palette getNow(String skinUrl) {
        CompletableFuture<Palette> f = get(skinUrl);
        return f.isDone() ? f.join() : null;
    }

    private Palette download(String url) {
        try {
            URL u = URI.create(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) u.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "MCPvPBanAnimation");
            byte[] bytes;
            try (InputStream in = conn.getInputStream()) {
                bytes = in.readNBytes(MAX_BYTES);
            } finally {
                conn.disconnect();
            }
            return SkinDecoder.palette(bytes);
        } catch (Throwable t) {
            // Includes NoClassDefFoundError on JREs without java.desktop (ImageIO).
            logger.fine("Could not read skin colors from " + url + ": " + t);
            return DEFAULT;
        }
    }

    private static boolean isTrustedHost(String url) {
        try {
            String host = URI.create(url).getHost();
            return host != null && host.toLowerCase(Locale.ROOT).endsWith("textures.minecraft.net");
        } catch (Throwable t) {
            return false;
        }
    }

    /** Kept in its own class, so ImageIO is only loaded when a skin is actually decoded. */
    private static final class SkinDecoder {

        static Palette palette(byte[] png) throws java.io.IOException {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
            if (img == null || img.getWidth() < 64 || img.getHeight() < 32) {
                return DEFAULT;
            }
            // Skin layout (base layer): body front 20..28 x 20..32, right leg front 4..8 x 20..32.
            int chest = average(img, 20, 20, 28, 32, DEFAULT.chest());
            int legs = average(img, 4, 20, 8, 28, DEFAULT.legs());
            int boots = average(img, 4, 29, 8, 32, DEFAULT.boots());
            return new Palette(chest, legs, boots);
        }

        private static int average(java.awt.image.BufferedImage img, int x0, int y0, int x1, int y1, int fallback) {
            long r = 0;
            long g = 0;
            long b = 0;
            int n = 0;
            for (int y = y0; y < y1; y++) {
                for (int x = x0; x < x1; x++) {
                    int argb = img.getRGB(x, y);
                    if ((argb >>> 24) < 128) {
                        continue;
                    }
                    r += (argb >> 16) & 0xFF;
                    g += (argb >> 8) & 0xFF;
                    b += argb & 0xFF;
                    n++;
                }
            }
            if (n == 0) {
                return fallback;
            }
            return (int) ((r / n) << 16 | (g / n) << 8 | (b / n));
        }
    }
}
