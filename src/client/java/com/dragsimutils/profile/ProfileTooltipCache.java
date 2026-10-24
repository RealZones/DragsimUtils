package com.dragsimutils.profile;

import net.minecraft.network.chat.Component;
import java.util.*;

/** Cache content only; Minecraft still owns tooltip placement, font layout and rendering. */
public final class ProfileTooltipCache {
    private ProfileData.Item item;
    private ProfileItems source;
    private boolean catalog;
    private int muted;
    private List<Component> lines=List.of();

    public List<Component> get(ProfileItems source,ProfileData.Item item,boolean catalog,int muted) {
        if(this.item==item && this.source==source && this.catalog==catalog && this.muted==muted) return lines;
        List<Component> built=new ArrayList<>();built.add(source.nameLine(item));built.addAll(source.lore(item));
        if(catalog) built.add(Component.literal("Not owned · Catalog entry").withColor(muted & 0xFFFFFF));
        built.add(Component.literal("Click to inspect").withColor(muted & 0xFFFFFF));
        this.item=item;this.source=source;this.catalog=catalog;this.muted=muted;
        lines=List.copyOf(built);return lines;
    }
}
