package com.fleoxxzy.mcpvpBanAnimation.animation;

import com.fleoxxzy.mcpvpBanAnimation.config.PluginConfig;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** The impact: sounds, particle bursts, the expanding shockwave ring and the camera shake. */
final class ImpactEffects {

    private ImpactEffects() {
    }

    static void play(AnimationContext ctx) {
        PluginConfig.Impact cfg = ctx.config().impact();
        Location center = ctx.anchor().add(0, 0.9, 0);

        for (PluginConfig.SoundSpec sound : cfg.sounds()) {
            ctx.effects().sound(center, sound);
        }
        for (PluginConfig.ParticleSpec particle : cfg.particles()) {
            ctx.effects().burst(center, particle);
        }
        if (cfg.shockwave().enabled()) {
            shockwave(ctx, cfg.shockwave());
        }
        if (cfg.cameraShake().enabled()) {
            cameraShake(ctx, cfg.cameraShake());
        }
    }

    private static void shockwave(AnimationContext ctx, PluginConfig.Shockwave wave) {
        for (int ring = 1; ring <= wave.rings(); ring++) {
            double radius = wave.maxRadius() * ring / wave.rings();
            int points = Math.max(4, (int) Math.round(wave.pointsPerRing() * (0.5 + 0.5 * ring / wave.rings())));
            ctx.schedule((ring - 1) * wave.ringInterval(), () -> {
                Location base = ctx.anchor().add(0, 0.15, 0);
                double phase = ctx.random().nextDouble() * Math.PI * 2;
                for (int i = 0; i < points; i++) {
                    double angle = phase + Math.PI * 2 * i / points;
                    Location at = base.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
                    ctx.effects().simple(at, wave.particle(), 1, 0.03, 0.03, 0.03, 0.01);
                }
            });
        }
    }

    /**
     * The vanilla hurt camera tilt, without damage. Nearby players may live in another region on
     * Folia, so each one is updated on their own entity scheduler, and the distance is re-checked there.
     */
    private static void cameraShake(AnimationContext ctx, PluginConfig.CameraShake shake) {
        World world = ctx.world();
        Location anchor = ctx.anchor();
        double radiusSq = shake.radius() * shake.radius();
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (int pulse = 0; pulse < shake.pulses(); pulse++) {
                float yaw = ctx.random().nextFloat() * 360f;
                ctx.platform().scheduler().runForLater(player, () -> {
                    Location loc = player.getLocation();
                    if (loc.getWorld() == world && loc.distanceSquared(anchor) <= radiusSq) {
                        player.playHurtAnimation(yaw);
                    }
                }, null, 1L + (long) pulse * shake.interval());
            }
        }
    }
}
