package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.server.MinecraftServer;
import net.phoenix.core.integration.phoenix_chronicles.flag.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

public final class PhoenixQuestFlags {

    private PhoenixQuestFlags() {}

    private static final Map<String, QuestFlagProvider> providers = new ConcurrentHashMap<>();

    public static final ConfigFileFlagProvider CONFIG = new ConfigFileFlagProvider();
    public static final KubeJsFlagProvider KJS = new KubeJsFlagProvider();

    static {
        registerProvider(new ModLoadedFlagProvider());
        registerProvider(new GameRuleFlagProvider());
        registerProvider(CONFIG);
        registerProvider(KJS);
    }

    public static void registerProvider(QuestFlagProvider provider) {
        providers.put(provider.prefix(), provider);
    }

    private static final Map<String, Boolean> staticFlags = new ConcurrentHashMap<>();
    private static final Map<String, BooleanSupplier> conditions = new ConcurrentHashMap<>();
    private static final Set<String> warnedUnknown = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void setFlag(String name, boolean value) {
        staticFlags.put(name, value);
        conditions.remove(name);
    }

    public static void registerCondition(String name, BooleanSupplier condition) {
        staticFlags.remove(name);
        conditions.put(name, condition);
    }

    public static void clearFlag(String name) {
        staticFlags.remove(name);
        conditions.remove(name);
    }

    public static boolean evaluate(@Nullable String expression) {
        return evaluate(expression, null);
    }

    public static boolean evaluate(@Nullable String expression, @Nullable MinecraftServer server) {
        if (expression == null || expression.isBlank()) return true;
        
        for (String orClause : expression.split("\\|")) {
            boolean andResult = true;
            for (String part : orClause.split(",")) {
                String term = part.trim();
                if (!term.isEmpty() && !evaluateTerm(term, server)) {
                    andResult = false;
                    break;
                }
            }
            if (andResult) return true; 
        }
        return false;
    }

    private static boolean evaluateTerm(String term, @Nullable MinecraftServer server) {
        if (term.startsWith("!")) return !evaluateTerm(term.substring(1), server);
        int colon = term.indexOf(':');
        if (colon > 0) {
            String prefix = term.substring(0, colon);
            String rest = term.substring(colon + 1);

            if ("flag".equals(prefix)) return evaluateStaticFlag(rest);

            QuestFlagProvider provider = providers.get(prefix);
            if (provider != null) return provider.evaluate(rest, server);

            if (warnedUnknown.add("prefix:" + prefix)) {
                System.err.println("[Phoenix Chronicles] Unknown flag prefix '" + prefix + "' in expression '" + term +
                        "'. Register a provider with PhoenixQuestFlags.registerProvider().");
            }
            return false;
        }

        return evaluateStaticFlag(term);
    }

    private static boolean evaluateStaticFlag(String name) {
        Boolean staticVal = staticFlags.get(name);
        if (staticVal != null) return staticVal;

        BooleanSupplier dyn = conditions.get(name);
        if (dyn != null) return dyn.getAsBoolean();

        if (warnedUnknown.add(name)) {
            System.err.println("[Phoenix Chronicles] Unknown quest flag '" + name +
                    "' — defaulting to true. Use PhoenixQuestFlags.setFlag() or" +
                    " registerCondition() to register it, or use a provider prefix" + " (mod:, config:, rule:, kjs:).");
        }
        return true;
    }

    public static void invalidateCaches() {
        CONFIG.invalidateCache();
        KJS.invalidate();
    }
}
