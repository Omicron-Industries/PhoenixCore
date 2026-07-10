package net.phoenix.core.conflux.tools.capture;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Compiles and loads {@link CaptureBakeable} implementations from loose {@code .java}
 * files dropped in the {@code sprite_bakers/} folder next to the game's run directory.
 *
 * Also reads {@code sprite_bakers/auto.list} for zero-code reflective bakers.
 *
 * Requires a JDK (not just JRE) — always true in Gradle/Forge dev runs.
 */
public final class HotBakerLoader {

    /** Relative to the game's working directory (i.e. {@code run/sprite_bakers/}). */
    public static final Path BAKERS_DIR = Paths.get("sprite_bakers");
    private static final Path OUTPUT_DIR = BAKERS_DIR.resolve("out");

    private HotBakerLoader() {}

    public static int reload() {
        if (!Files.isDirectory(BAKERS_DIR)) {
            try { Files.createDirectories(BAKERS_DIR); } catch (IOException ignored) {}
            return 0;
        }

        int count = 0;
        count += loadAutoList();

        List<Path> sources = collectSources();
        if (!sources.isEmpty()) {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new IllegalStateException(
                    "No JavaCompiler — sprite_bakers hot-loading requires a JDK, not a plain JRE.");
            if (compile(compiler, sources)) count += loadAndRegister();
        }

        return count;
    }

    // ── auto.list ─────────────────────────────────────────────────────────────

    private static int loadAutoList() {
        Path listFile = BAKERS_DIR.resolve("auto.list");
        if (!Files.exists(listFile)) return 0;

        ClassLoader modLoader = Thread.currentThread().getContextClassLoader();
        int count = 0;

        try {
            for (String rawLine : Files.readAllLines(listFile)) {
                String line = rawLine.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\s+");
                if (parts.length < 4) {
                    System.err.println("[HotBakerLoader] auto.list bad line (need: id class w h): " + line);
                    continue;
                }

                String id = parts[0], className = parts[1];
                int w, h, frames;
                try {
                    w      = Integer.parseInt(parts[2]);
                    h      = Integer.parseInt(parts[3]);
                    frames = parts.length >= 5 ? Integer.parseInt(parts[4]) : 1;
                } catch (NumberFormatException e) {
                    System.err.println("[HotBakerLoader] auto.list bad dimensions: " + line);
                    continue;
                }

                try {
                    // Use the FML/Forge mod classloader — sees all loaded mods
                    Class<?> cls = Class.forName(className, true, modLoader);
                    SpriteCaptureRegistry.register(new ReflectiveBaker(id, cls, w, h, frames));
                    System.out.println("[HotBakerLoader] Auto-registered: " + id + " → " + className);
                    count++;
                } catch (ClassNotFoundException e) {
                    System.err.println("[HotBakerLoader] Class not found: " + className
                            + " — is the mod loaded and on the classpath?");
                }
            }
        } catch (IOException e) {
            System.err.println("[HotBakerLoader] Could not read auto.list: " + e.getMessage());
        }
        return count;
    }

    // ── Compilation ───────────────────────────────────────────────────────────

    private static List<Path> collectSources() {
        try (Stream<Path> walk = Files.walk(BAKERS_DIR, 1)) {
            return walk.filter(p -> p.toString().endsWith(".java")).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static boolean compile(JavaCompiler compiler, List<Path> sources) {
        try { Files.createDirectories(OUTPUT_DIR); } catch (IOException e) { return false; }

        List<String> args = new ArrayList<>(List.of(
                "-classpath", buildFullClasspath(),
                "-d", OUTPUT_DIR.toAbsolutePath().toString(),
                "-source", "17", "-target", "17"
        ));
        sources.forEach(p -> args.add(p.toAbsolutePath().toString()));

        java.io.ByteArrayOutputStream errBuf = new java.io.ByteArrayOutputStream();
        int result = compiler.run(null, null, errBuf, args.toArray(new String[0]));

        if (result != 0) {
            System.err.println("[HotBakerLoader] Compilation errors:\n" + errBuf);
            return false;
        }
        return true;
    }

    /**
     * Builds a classpath string by combining:
     * <ol>
     *   <li>{@code java.class.path} system property (JVM bootstrap classpath)</li>
     *   <li>All URLs from every {@link URLClassLoader} in the classloader hierarchy
     *       starting from the current thread's context classloader — this captures
     *       Forge's mod classloader and therefore all loaded mod JARs.</li>
     * </ol>
     */
    private static String buildFullClasspath() {
        Set<String> paths = new LinkedHashSet<>();

        // JVM bootstrap classpath
        String jvmCp = System.getProperty("java.class.path");
        if (jvmCp != null) paths.addAll(Arrays.asList(jvmCp.split(File.pathSeparator)));

        // Walk classloader hierarchy — Forge's mod loader is a URLClassLoader (or wraps one)
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        while (cl != null) {
            if (cl instanceof URLClassLoader urlCl) {
                for (URL url : urlCl.getURLs()) {
                    try { paths.add(new File(url.toURI()).getAbsolutePath()); }
                    catch (Exception ignored) {}
                }
            }
            // Also try getURLs via reflection for non-URLClassLoader implementations
            // (e.g. FML's SecureJarHandler wrapping)
            try {
                java.lang.reflect.Method getURLs = cl.getClass().getMethod("getURLs");
                URL[] urls = (URL[]) getURLs.invoke(cl);
                for (URL url : urls) {
                    try { paths.add(new File(url.toURI()).getAbsolutePath()); }
                    catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            cl = cl.getParent();
        }

        return String.join(File.pathSeparator, paths);
    }

    // ── Class loading & registration ──────────────────────────────────────────

    private static int loadAndRegister() {
        int count = 0;
        try {
            URL outUrl = OUTPUT_DIR.toUri().toURL();
            // Parent to the FML mod classloader so compiled bakers can reference mod classes
            URLClassLoader loader = new URLClassLoader(
                    new URL[]{ outUrl },
                    Thread.currentThread().getContextClassLoader()
            );

            List<Path> classFiles = new ArrayList<>();
            try (Stream<Path> walk = Files.walk(OUTPUT_DIR)) {
                walk.filter(p -> p.toString().endsWith(".class")).forEach(classFiles::add);
            }

            for (Path classFile : classFiles) {
                String className = classNameFrom(OUTPUT_DIR, classFile);
                try {
                    Class<?> cls = loader.loadClass(className);
                    if (!CaptureBakeable.class.isAssignableFrom(cls) || cls.isInterface()) continue;

                    CaptureBakeable baker = (CaptureBakeable) cls.getDeclaredConstructor().newInstance();
                    SpriteCaptureRegistry.register(baker);
                    System.out.println("[HotBakerLoader] Registered hot baker: " + baker.id());
                    count++;
                } catch (Exception e) {
                    System.err.println("[HotBakerLoader] Failed to load " + className + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[HotBakerLoader] Load error: " + e.getMessage());
        }
        return count;
    }

    private static String classNameFrom(Path root, Path classFile) {
        String rel = root.relativize(classFile).toString();
        return rel.replace(File.separatorChar, '.').replaceAll("\\.class$", "");
    }
}
