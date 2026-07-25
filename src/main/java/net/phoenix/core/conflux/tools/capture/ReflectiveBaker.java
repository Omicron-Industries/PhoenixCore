package net.phoenix.core.conflux.tools.capture;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class ReflectiveBaker implements CaptureBakeable {

    private final String id;
    private final Class<?> targetClass;
    private final int frameCount;
    private final int width;
    private final int height;

    public ReflectiveBaker(String id, Class<?> targetClass, int width, int height, int frameCount) {
        this.id = id;
        this.targetClass = targetClass;
        this.width = width;
        this.height = height;
        this.frameCount = frameCount;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public int frameCount() {
        return frameCount;
    }

    @Override
    public int frameWidth() {
        return width;
    }

    @Override
    public int frameHeight() {
        return height;
    }

    @Override
    public void renderFrame(GuiGraphics g, int frame, float t, int w, int h) {
        try {
            Object instance = instantiate(w, h);
            render(instance, g, frame, t, w, h);
        } catch (Exception e) {

            g.fill(0, 0, w, h, 0xFF3A0000);
            g.drawString(Minecraft.getInstance().font,
                    "Reflect error: " + e.getClass().getSimpleName(), 8, 8, 0xFFFF4444, false);
            g.drawString(Minecraft.getInstance().font,
                    truncate(e.getMessage(), 60), 8, 20, 0xFFFF8888, false);
            System.err.println("[ReflectiveBaker:" + id + "] " + e);
        }
    }

    private Object instantiate(int w, int h) throws ReflectiveOperationException {
        try {
            return targetClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException ignored) {}

        try {
            return targetClass.getDeclaredConstructor(Screen.class).newInstance((Object) null);
        } catch (NoSuchMethodException ignored) {}

        try {
            return targetClass.getDeclaredConstructor(Screen.class, Component.class)
                    .newInstance(null, Component.literal("Bake"));
        } catch (NoSuchMethodException ignored) {}

        try {
            return targetClass.getDeclaredConstructor(Minecraft.class, int.class, int.class, Screen.class)
                    .newInstance(Minecraft.getInstance(), w, h, null);
        } catch (NoSuchMethodException ignored) {}

        for (Constructor<?> ctor : targetClass.getDeclaredConstructors()) {
            try {
                Object[] args = buildArgs(ctor.getParameterTypes(), w, h);
                if (args != null) {
                    ctor.setAccessible(true);
                    return ctor.newInstance(args);
                }
            } catch (Exception ignored) {}
        }

        throw new IllegalStateException("No usable constructor found on " + targetClass.getName());
    }

    private static Object[] buildArgs(Class<?>[] types, int w, int h) {
        Object[] args = new Object[types.length];
        int intsSeen = 0;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == Minecraft.class) {
                args[i] = Minecraft.getInstance();
            } else if (t == Screen.class) {
                args[i] = null;
            } else if (t == Component.class) {
                args[i] = Component.literal("Bake");
            } else if (t == String.class) {
                args[i] = "bake";
            } else if (t == boolean.class) {
                args[i] = false;
            } else if (t == float.class) {
                args[i] = 0f;
            } else if (t == double.class) {
                args[i] = 0.0;
            } else if (t == long.class) {
                args[i] = 0L;
            } else if (t == int.class) {

                args[i] = (intsSeen == 0) ? w : (intsSeen == 1) ? h : 0;
                intsSeen++;
            } else if (!t.isPrimitive()) {
                args[i] = null;
            } else {
                return null;
            }
        }
        return args;
    }

    private void render(Object instance, GuiGraphics g, int frame, float t, int w, int h)
                                                                                          throws ReflectiveOperationException {
        if (instance instanceof Screen screen) {
            screen.init(Minecraft.getInstance(), w, h);
            screen.render(g, -1, -1, 0f);
            return;
        }

        Method m = findMethod("renderBackground", GuiGraphics.class, Object.class);
        if (m != null) {
            m.invoke(instance, g, BakeRenderContext.of(w, h, t * 30f));
            return;
        }

        m = findMethod("render", GuiGraphics.class, int.class, int.class, float.class);
        if (m != null) {
            m.invoke(instance, g, -1, -1, t);
            return;
        }

        m = findMethod("render", GuiGraphics.class);
        if (m != null) {
            m.invoke(instance, g);
            return;
        }

        throw new IllegalStateException(
                "No recognised render method on " + targetClass.getName() +
                        " — add a .java baker file to sprite_bakers/ for full control.");
    }

    private Method findMethod(String name, Class<?>... params) {
        Class<?> cls = targetClass;
        while (cls != null && cls != Object.class) {
            try {
                Method m = cls.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static String truncate(String s, int max) {
        if (s == null) return "(null)";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
