package net.phoenix.core.integration.phoenix_guilds;

public enum GuildRank {

    MEMBER,
    OFFICER,
    OWNER;

    public boolean isAtLeast(GuildRank required) {
        return this.ordinal() >= required.ordinal();
    }

    public String display() {
        return switch (this) {
            case OWNER -> "★";
            case OFFICER -> "⚑";
            case MEMBER -> "•";
        };
    }

    public String label() {
        return switch (this) {
            case OWNER -> "Owner";
            case OFFICER -> "Officer";
            case MEMBER -> "Member";
        };
    }
}
