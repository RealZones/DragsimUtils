package com.dragsimutils.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import org.jspecify.annotations.Nullable;

/**
 * Colour lookups for legacy formatting codes. Minecraft 26.2 moved the colour of a
 * ChatFormatting out of the enum and into TextColor; these keep the old call shapes.
 */
public final class LegacyColors {
    private LegacyColors() {}

    public static boolean isColor(ChatFormatting format) {
        return TextColor.fromLegacyFormat(format) != null;
    }

    public static @Nullable Integer color(ChatFormatting format) {
        TextColor color = TextColor.fromLegacyFormat(format);
        return color == null ? null : color.getValue();
    }

    public static char code(ChatFormatting format) {
        return format.toString().charAt(1);
    }
}
