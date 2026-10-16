package com.fleoxxzy.mcpvpBanAnimation.animation;

import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Helpers for animating Display entities through their transformation, never by teleporting them. */
final class Displays {

    private Displays() {
    }

    /**
     * Sets the transformation and starts client-side interpolation towards it.
     * {@code duration == 0} snaps instantly (used for the initial pose).
     */
    static void pose(Display display, Vector3f translation, Quaternionf rotation, Vector3f scale, int duration) {
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(Math.max(0, duration));
        display.setTransformation(new Transformation(
                new Vector3f(translation), new Quaternionf(rotation), new Vector3f(scale), new Quaternionf()));
    }

    static Vector3f uniform(float s) {
        return new Vector3f(s, s, s);
    }

    /** Cubic ease-in with a small linear start, so the motion never looks stuck at t=0. */
    static double easeInFall(double t) {
        double c = Math.max(0, Math.min(1, t));
        return 0.12 * c + 0.88 * c * c * c;
    }
}
