package com.fleoxxzy.mcpvpBanAnimation.core;

import java.lang.reflect.Method;

/**
 * Tiny reflection helpers for runtime feature detection. Nothing here ever throws.
 */
public final class Reflect {

    private Reflect() {
    }

    public static boolean hasClass(String name) {
        return findClass(name) != null;
    }

    public static Class<?> findClass(String name) {
        try {
            // initialize=false: only check presence, never run static initializers of foreign classes.
            return Class.forName(name, false, Reflect.class.getClassLoader());
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean hasMethod(Class<?> owner, String name, Class<?>... params) {
        return findMethod(owner, name, params) != null;
    }

    public static Method findMethod(Class<?> owner, String name, Class<?>... params) {
        if (owner == null) {
            return null;
        }
        try {
            return owner.getMethod(name, params);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean hasField(Class<?> owner, String name) {
        if (owner == null) {
            return false;
        }
        try {
            owner.getField(name);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** True if {@code enumClass} is an enum containing a constant called {@code constant}. */
    public static boolean hasEnumConstant(Class<?> enumClass, String constant) {
        if (enumClass == null || !enumClass.isEnum()) {
            return hasField(enumClass, constant);
        }
        for (Object value : enumClass.getEnumConstants()) {
            if (((Enum<?>) value).name().equals(constant)) {
                return true;
            }
        }
        return false;
    }

    /** Invokes a public no-arg method and returns its result, or {@code null} on any failure. */
    public static Object invokeNoArgs(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
