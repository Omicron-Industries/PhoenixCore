package net.phoenix.core.integration.phoenix_chronicles;

import net.minecraft.server.MinecraftServer;
import net.phoenix.core.integration.phoenix_chronicles.flag.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

import javax.annotation.Nullable;

/**
 * Central flag registry for PhoenixCore quest conditional activation.
 *
 * Flags are referenced in quest SNBT via the {@code enable_if} field.
 * When a flag expression evaluates to {@code false}, the quest is treated as
 * HIDDEN + DISABLED — invisible, non-completable, and non-gating.
 *
 * ── Expression syntax ────────────────────────────────────────────────────────
 *
 * enable_if: "expression"
 * enable_if: "expr1,expr2" (AND — all must be true)
 * enable_if: "expr1|expr2" (OR — any must be true)
 * enable_if: "!expr" (NOT)
 * enable_if: "a,b|c,d" (= (a AND b) OR (c AND d) — | splits AND-groups)
 *
 * Each expression is either a plain flag name (no prefix) or:
 *
 * PREFIX:EXPRESSION[OPERATOR VALUE]
 *
 * Built-in prefixes:
 *
 * flag:name Registered boolean flag/condition (same as no prefix)
 * mod:modid Mod is currently loaded
 * rule:ruleName Game rule truthy check (boolean) or comparison
 * rule:ruleName>=3 Game rule numeric comparison
 * config:file#key=val Value from a config file (.toml / .json / .properties)
 * kjs:key Flag exported from a KubeJS startup script
 *
 * Operators (available where a VALUE is given): = != > >= < <=
 *
 * ── Java API ─────────────────────────────────────────────────────────────────
 *
 * // Static (set once):
 * PhoenixQuestFlags.setFlag("expert_mode", true);
 *
 * // Dynamic (re-evaluated every call):
 * PhoenixQuestFlags.registerCondition("has_rs",
 * () -> ModList.get().isLoaded("refinedstorage"));
 *
 * // Custom provider (for a whole prefix namespace):
 * PhoenixQuestFlags.registerProvider(new MyCustomProvider());
 *
 * ── KubeJS startup script ────────────────────────────────────────────────────
 *
 * // startup_scripts/phoenix_flags.js
 * const file = new java.io.File("config/phoenix_chronicles/kjs_flags.json")
 * file.getParentFile().mkdirs()
 * file.text = JSON.stringify({
 * expert_mode: Platform.isLoaded("somemod"),
 * pack_tier: 3
 * }, null, 2)
 *
 * // Then in quest SNBT:
 * enable_if: "kjs:expert_mode"
 * enable_if: "kjs:pack_tier>=2"
 *
 * ── Unknown flag names ────────────────────────────────────────────────────────
 *
 * Unknown plain-name flags default to TRUE with a one-time console warning.
 * This prevents typos from silently hiding quests.
 * Unknown prefixes return false and log a warning.
 */
public final class PhoenixQuestFlags {

    private PhoenixQuestFlags() {}

    // ── Provider registry ─────────────────────────────────────────────────────

    private static final Map<String, QuestFlagProvider> providers = new ConcurrentHashMap<>();

    // Built-in provider singletons (exposed so callers can invalidate caches)
    public static final ConfigFileFlagProvider CONFIG = new ConfigFileFlagProvider();
    public static final KubeJsFlagProvider KJS = new KubeJsFlagProvider();

    static {
        registerProvider(new ModLoadedFlagProvider());
        registerProvider(new GameRuleFlagProvider());
        registerProvider(CONFIG);
        registerProvider(KJS);
    }

    /**
     * Registers a custom flag provider. The provider's {@link QuestFlagProvider#prefix()}
     * is used to route expressions. Overwrites any existing provider with the same prefix.
     */
    public static void registerProvider(QuestFlagProvider provider) {
        providers.put(provider.prefix(), provider);
    }

    // ── Static flag registry (no-prefix / "flag:" expressions) ───────────────

    private static final Map<String, Boolean> staticFlags = new ConcurrentHashMap<>();
    private static final Map<String, BooleanSupplier> conditions = new ConcurrentHashMap<>();
    private static final Set<String> warnedUnknown = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Sets a static boolean flag. Evaluated once — good for pack mode values
     * read from a config at startup.
     *
     * <pre>
     * PhoenixQuestFlags.setFlag("expert_mode", true);
     * </pre>
     */
    public static void setFlag(String name, boolean value) {
        staticFlags.put(name, value);
        conditions.remove(name);
    }

    /**
     * Registers a dynamic condition re-evaluated on each flag check. Good for
     * values that may change while the game is running.
     *
     * <pre>
     * PhoenixQuestFlags.registerCondition("has_rs",
     *         () -> ModList.get().isLoaded("refinedstorage"));
     * </pre>
     */
    public static void registerCondition(String name, BooleanSupplier condition) {
        staticFlags.remove(name);
        conditions.put(name, condition);
    }

    /**
     * Removes a registered flag or condition (falls back to default true).
     */
    public static void clearFlag(String name) {
        staticFlags.remove(name);
        conditions.remove(name);
    }

    // ── Evaluation ────────────────────────────────────────────────────────────

    /**
     * Evaluates a comma-separated AND expression without server context.
     * Suitable for client-side display checks and static checks.
     */
    public static boolean evaluate(@Nullable String expression) {
        return evaluate(expression, null);
    }

    /**
     * Evaluates a flag expression with optional server context.
     *
     * Syntax (highest to lowest precedence):
     * !term — NOT
     * a,b — AND (all must be true)
     * a|b — OR (any must be true)
     *
     * Example: "kjs:expert_mode|kjs:hardcore,mod:refinedstorage"
     * → (expert_mode OR hardcore) AND refinedstorage loaded
     *
     * Null or blank always returns true (no condition = always enabled).
     */
    public static boolean evaluate(@Nullable String expression, @Nullable MinecraftServer server) {
        if (expression == null || expression.isBlank()) return true;
        // Split by | first (OR), each OR-clause is itself an AND group
        for (String orClause : expression.split("\\|")) {
            boolean andResult = true;
            for (String part : orClause.split(",")) {
                String term = part.trim();
                if (!term.isEmpty() && !evaluateTerm(term, server)) {
                    andResult = false;
                    break;
                }
            }
            if (andResult) return true; // one OR-clause succeeded
        }
        return false;
    }

    private static boolean evaluateTerm(String term, @Nullable MinecraftServer server) {
        if (term.startsWith("!")) return !evaluateTerm(term.substring(1), server);
        int colon = term.indexOf(':');
        if (colon > 0) {
            String prefix = term.substring(0, colon);
            String rest = term.substring(colon + 1);

            // "flag:" is an explicit alias for the static registry
            if ("flag".equals(prefix)) return evaluateStaticFlag(rest);

            QuestFlagProvider provider = providers.get(prefix);
            if (provider != null) return provider.evaluate(rest, server);

            // Unknown prefix — warn once, return false
            if (warnedUnknown.add("prefix:" + prefix)) {
                System.err.println("[Phoenix Chronicles] Unknown flag prefix '" + prefix + "' in expression '" + term +
                        "'. Register a provider with PhoenixQuestFlags.registerProvider().");
            }
            return false;
        }

        // No prefix — static flag / condition registry
        return evaluateStaticFlag(term);
    }

    private static boolean evaluateStaticFlag(String name) {
        Boolean staticVal = staticFlags.get(name);
        if (staticVal != null) return staticVal;

        BooleanSupplier dyn = conditions.get(name);
        if (dyn != null) return dyn.getAsBoolean();

        // Unknown — warn once and default to true so the quest stays visible
        if (warnedUnknown.add(name)) {
            System.err.println("[Phoenix Chronicles] Unknown quest flag '" + name +
                    "' — defaulting to true. Use PhoenixQuestFlags.setFlag() or" +
                    " registerCondition() to register it, or use a provider prefix" + " (mod:, config:, rule:, kjs:).");
        }
        return true;
    }

    // ── Cache invalidation ────────────────────────────────────────────────────

    /**
     * Clears all file-backed provider caches. Called automatically on world load.
     * Can also be called manually after a KubeJS /reload.
     */
    public static void invalidateCaches() {
        CONFIG.invalidateCache();
        KJS.invalidate();
    }
}
