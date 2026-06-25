package net.phoenix.core.integration.phoenix_chronicles.flag;

import net.minecraft.server.MinecraftServer;

import javax.annotation.Nullable;

/**
 * A named provider that evaluates flag expressions for PhoenixQuestFlags.
 *
 * Register custom providers with
 * {@link net.phoenix.core.integration.phoenix_chronicles.PhoenixQuestFlags#registerProvider}.
 *
 * Expression format in quest SNBT:
 * enable_if: "[prefix:]expression"
 *
 * Example (custom provider with prefix "myplugin"):
 * enable_if: "myplugin:some_condition>=3"
 *
 * The expression string passed to {@link #evaluate} is everything AFTER the colon.
 *
 * Providers that require server context (game rules, world data) should return
 * {@code true} when {@code server} is null — the client uses flags for display
 * only; the server enforces completion. Returning {@code false} client-side would
 * hide quests from players even when the server would consider them active.
 */
public interface QuestFlagProvider {

    /** The prefix that routes to this provider, e.g. {@code "mod"}, {@code "rule"}. */
    String prefix();

    /**
     * Evaluates an expression within this provider's domain.
     *
     * @param expression Everything after the prefix colon, e.g. {@code "refinedstorage"}
     *                   or {@code "randomTickSpeed>=3"}.
     * @param server     Nullable. Null on client-side display checks and during early
     *                   world load. Providers that need server state should return
     *                   {@code true} when this is null.
     */
    boolean evaluate(String expression, @Nullable MinecraftServer server);
}
