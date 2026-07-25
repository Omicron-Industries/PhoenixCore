package net.phoenix.core.integration.phoenix_chronicles.flag;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nullable;

public class GameRuleFlagProvider implements QuestFlagProvider {

    @Override
    public String prefix() {
        return "rule";
    }

    @Override
    public boolean evaluate(String expression, @Nullable MinecraftServer server) {
        if (server == null) return true;

        FlagExpression expr = FlagExpression.parse(expression);
        GameRules.Key<?> key = getRuleKey(expr.key);
        if (key == null) {
            warnUnknown(expr.key);
            return false;
        }

        GameRules.Value<?> val = server.getGameRules().getRule(key);
        String actualStr;
        if (val instanceof GameRules.BooleanValue bv) {
            actualStr = String.valueOf(bv.get());
        } else if (val instanceof GameRules.IntegerValue iv) {
            actualStr = String.valueOf(iv.get());
        } else {
            actualStr = val.toString();
        }

        return expr.test(actualStr);
    }

    private static volatile Map<String, GameRules.Key<?>> ruleKeys = null;
    private static final java.util.Set<String> warnedRules = java.util.Collections
            .newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    @Nullable
    private static GameRules.Key<?> getRuleKey(String name) {
        return getKnownRules().get(name);
    }

    private static Map<String, GameRules.Key<?>> getKnownRules() {
        if (ruleKeys != null) return ruleKeys;
        synchronized (GameRuleFlagProvider.class) {
            if (ruleKeys != null) return ruleKeys;
            Map<String, GameRules.Key<?>> map = new LinkedHashMap<>();

            Field idField = null;
            for (Field f : GameRules.Key.class.getDeclaredFields()) {
                if (f.getType() == String.class) {
                    idField = f;
                    idField.setAccessible(true);
                    break;
                }
            }
            if (idField != null) {
                for (Field f : GameRules.class.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    if (!GameRules.Key.class.isAssignableFrom(f.getType())) continue;
                    try {
                        GameRules.Key<?> k = (GameRules.Key<?>) f.get(null);
                        String id = (String) idField.get(k);
                        if (id != null) map.put(id, k);
                    } catch (Exception ignored) {}
                }
            }
            ruleKeys = map;
            return ruleKeys;
        }
    }

    private static void warnUnknown(String name) {
        if (warnedRules.add(name)) {
            System.err.println("[Phoenix Chronicles] Unknown game rule in flag expression: '" + name + "'");
        }
    }
}
