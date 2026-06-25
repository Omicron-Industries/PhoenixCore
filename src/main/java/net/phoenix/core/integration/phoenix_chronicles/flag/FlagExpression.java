package net.phoenix.core.integration.phoenix_chronicles.flag;

import javax.annotation.Nullable;

/**
 * Parses and evaluates comparison expressions of the form:
 * key (EXISTS — truthy: non-null, non-empty, not "false", not "0")
 * key=value (equality)
 * key!=value (inequality)
 * key>value (numeric greater-than)
 * key>=value (numeric greater-or-equal)
 * key<value (numeric less-than)
 * key<=value (numeric less-or-equal)
 *
 * Used by flag providers to avoid duplicating parsing logic.
 */
public final class FlagExpression {

    public enum Op {
        EXISTS,
        EQ,
        NEQ,
        GT,
        GTE,
        LT,
        LTE
    }

    public final String key;
    public final Op op;
    @Nullable
    public final String value;

    private FlagExpression(String key, Op op, @Nullable String value) {
        this.key = key;
        this.op = op;
        this.value = value;
    }

    /** Longer operators must come before their prefixes to avoid partial matches. */
    private static final String[] OPS = { ">=", "<=", "!=", ">", "<", "=" };
    private static final Op[] OP_ENUMS = { Op.GTE, Op.LTE, Op.NEQ, Op.GT, Op.LT, Op.EQ };

    public static FlagExpression parse(String expr) {
        if (expr == null) return new FlagExpression("", Op.EXISTS, null);
        for (int i = 0; i < OPS.length; i++) {
            int idx = expr.indexOf(OPS[i]);
            if (idx > 0) {
                return new FlagExpression(
                        expr.substring(0, idx).trim(),
                        OP_ENUMS[i],
                        expr.substring(idx + OPS[i].length()).trim());
            }
        }
        return new FlagExpression(expr.trim(), Op.EXISTS, null);
    }

    /**
     * Tests {@code actual} against the parsed expression.
     * Returns {@code false} if {@code actual} is null and op is not EXISTS.
     */
    public boolean test(@Nullable String actual) {
        if (op == Op.EXISTS) {
            return actual != null && !actual.isBlank() && !actual.equalsIgnoreCase("false") && !actual.equals("0") &&
                    !actual.equalsIgnoreCase("null");
        }
        if (actual == null) return false;

        // Numeric comparison
        if (op != Op.EQ && op != Op.NEQ) {
            try {
                double a = Double.parseDouble(actual.trim());
                double b = Double.parseDouble(value);
                return switch (op) {
                    case GT -> a > b;
                    case GTE -> a >= b;
                    case LT -> a < b;
                    case LTE -> a <= b;
                    default -> false;
                };
            } catch (NumberFormatException ignored) {
                return false; // non-numeric values don't support >, <
            }
        }

        // String / boolean equality
        boolean eq = actual.trim().equalsIgnoreCase(value != null ? value.trim() : "");
        return op == Op.EQ ? eq : !eq;
    }
}
