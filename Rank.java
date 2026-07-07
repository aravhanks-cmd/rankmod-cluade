package com.yourserver.rankmod;

import net.minecraft.ChatFormatting;

import java.util.Locale;

/**
 * The available ranks, lowest to highest. The ordinal (declaration order)
 * doubles as the "power level" - higher ranks automatically satisfy any
 * check that a lower rank would pass. Each rank also carries a chat color
 * used for its [TAG] prefix.
 */
public enum Rank {
    MEMBER(ChatFormatting.GRAY),
    VIP(ChatFormatting.YELLOW),
    MVP(ChatFormatting.GOLD),
    OG(ChatFormatting.DARK_PURPLE),
    BUILDER(ChatFormatting.DARK_AQUA),
    MOD(ChatFormatting.BLUE),
    ADMIN(ChatFormatting.RED),
    COOWNER(ChatFormatting.LIGHT_PURPLE, "CO-OWNER"),
    OWNER(ChatFormatting.DARK_RED);

    private final ChatFormatting color;
    private final String displayName;

    Rank(ChatFormatting color) {
        this(color, null);
    }

    Rank(ChatFormatting color, String displayName) {
        this.color = color;
        this.displayName = displayName != null ? displayName : name();
    }

    public ChatFormatting color() {
        return color;
    }

    /** Text shown in chat/tab-list tags - usually the same as the enum name, but nicer for CO-OWNER. */
    public String displayName() {
        return displayName;
    }

    /** True if this rank is the given rank or higher. */
    public boolean atLeast(Rank other) {
        return this.ordinal() >= other.ordinal();
    }

    public static Rank fromString(String name) {
        try {
            return Rank.valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

