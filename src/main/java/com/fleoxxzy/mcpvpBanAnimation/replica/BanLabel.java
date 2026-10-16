package com.fleoxxzy.mcpvpBanAnimation.replica;

import com.fleoxxzy.mcpvpBanAnimation.animation.AnimationContext;
import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import com.fleoxxzy.mcpvpBanAnimation.text.TextAdapter;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The floating {@code ● {username} BANNED ●} label. It always faces the viewer (CENTER billboard)
 * and bobs and pulses gently through interpolated transformations.
 */
final class BanLabel {

    private static final int UPDATE_EVERY = 2;

    private final PluginConfig.Label cfg;
    private final String text;
    private final TextAdapter textAdapter;
    private TextDisplay display;
    private boolean frozen;

    BanLabel(PluginConfig.Label cfg, String renderedMiniMessage, TextAdapter textAdapter) {
        this.cfg = cfg;
        this.text = renderedMiniMessage;
        this.textAdapter = textAdapter;
    }

    void spawn(AnimationContext ctx) {
        Location at = ctx.anchor().add(0, cfg.offsetY(), 0);
        display = ctx.spawn(at, TextDisplay.class, d -> {
            textAdapter.setDisplayText(d, text);
            d.setBillboard(Display.Billboard.CENTER);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setShadowed(cfg.shadow());
            d.setSeeThrough(false);
            d.setLineWidth(400);
            d.setViewRange(2f);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(cfg.backgroundArgb()));
            d.setBrightness(new Display.Brightness(15, 15));
            pose(d, 0f, cfg.scale(), 0);
        });
        if (display != null) {
            ctx.every(this::tick);
        }
    }

    /** Stops the bob/pulse (used after impact so the label "freezes" in shock). */
    void freeze() {
        frozen = true;
    }

    private boolean tick(int age) {
        if (display == null || !display.isValid()) {
            return false;
        }
        if (frozen || age % UPDATE_EVERY != 0) {
            return true;
        }
        double phase = 2 * Math.PI * age / cfg.period();
        float bob = (float) (cfg.bobAmplitude() * Math.sin(phase));
        float scale = (float) (cfg.scale() * (1 + cfg.pulseAmplitude() * Math.sin(phase * 2)));
        pose(display, bob, scale, UPDATE_EVERY);
        return true;
    }

    private static void pose(TextDisplay d, float y, float scale, int duration) {
        d.setInterpolationDelay(0);
        d.setInterpolationDuration(duration);
        d.setTransformation(new Transformation(new Vector3f(0, y, 0), new Quaternionf(),
                new Vector3f(scale, scale, scale), new Quaternionf()));
    }
}
