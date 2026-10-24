package com.dragsimutils;

import com.dragsimutils.dragons.BossDrops;
import com.dragsimutils.dragons.DragonTimer;
import com.dragsimutils.dragons.Protector;
import com.dragsimutils.dungeon.DungeonContext;
import com.dragsimutils.dungeon.DungeonMap;
import com.dragsimutils.dungeon.KeyAlert;
import com.dragsimutils.dungeon.BloodRoom;
import com.dragsimutils.dungeon.HigherOrLower;
import com.dragsimutils.dungeon.IcePath;
import com.dragsimutils.dungeon.Quiz;
import com.dragsimutils.dungeon.ThreeWeirdos;
import com.dragsimutils.dungeon.TicTacToe;
import com.dragsimutils.dungeon.TerminalSolver;
import com.dragsimutils.hud.Alert;
import com.dragsimutils.hud.Toast;
import com.dragsimutils.hud.Alerts;
import com.dragsimutils.hud.Area;
import com.dragsimutils.hud.HudPlacementScreen;
import com.dragsimutils.hud.HudLayout;
import com.dragsimutils.hud.ServerStats;
import com.dragsimutils.qol.AbilitySpam;
import com.dragsimutils.qol.CommandBinds;
import com.dragsimutils.qol.DamageNumbers;
import com.dragsimutils.qol.Drops;
import com.dragsimutils.qol.GlobalChat;
import com.dragsimutils.qol.CopyChat;
import com.dragsimutils.qol.ArmorGuard;
import com.dragsimutils.qol.EyeGuard;
import com.dragsimutils.qol.LootAlerts;
import com.dragsimutils.qol.LootFilter;
import com.dragsimutils.qol.LootChat;
import com.dragsimutils.qol.MenuButton;
import com.dragsimutils.qol.LootRoll;
import com.dragsimutils.qol.MathSolver;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.resources.Identifier;
import com.dragsimutils.slayers.Slayer;
import com.dragsimutils.slayers.SlayerSpawnMarker;
import com.dragsimutils.social.DiscordPresence;
import com.dragsimutils.slayers.RevenantSpawnTiles;
import com.dragsimutils.slayers.VoidgloomHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import com.dragsimutils.storage.SnapshotStore;
import com.dragsimutils.storage.ForgeRecipes;
import com.dragsimutils.storage.MiningSession;
import com.dragsimutils.storage.CacheTracker;
import com.dragsimutils.storage.VoidFragments;
import com.dragsimutils.storage.Pets;
import com.dragsimutils.storage.PowderTracker;
import com.dragsimutils.storage.Recipes;
import com.dragsimutils.storage.StoragePreview;
import com.dragsimutils.storage.StorageOverview;
import com.dragsimutils.storage.StorageSearch;
import com.dragsimutils.visual.Beacon;
import com.dragsimutils.visual.ExplosionParticles;
import com.dragsimutils.visual.Cooldowns;
import com.dragsimutils.visual.Etherwarp;
import com.dragsimutils.visual.Fullbright;
import com.dragsimutils.qol.SlotLock;
import com.dragsimutils.visual.InventoryGlass;
import com.dragsimutils.visual.InventoryPreview;
import com.dragsimutils.visual.EquipmentHud;
import com.dragsimutils.visual.NameGradient;
import com.dragsimutils.visual.RarityOutline;
import com.dragsimutils.visual.VisualTweaks;
import com.dragsimutils.visual.ShinyNodes;
import com.dragsimutils.zealots.ZealotTracker;
import com.dragsimutils.ui.Keybinds;
import com.dragsimutils.ui.Module;
import com.dragsimutils.ui.Setting;
import com.dragsimutils.ui.DragsimUtilsScreen;
import com.dragsimutils.ui.SettingsScreen;
import com.dragsimutils.ui.ModuleRegistry;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Initializes client features and owns the module/settings registry.
 *
 * The registry lives here rather than inside the UI package: com.dragsimutils.ui knows
 * how to draw modules and nothing about which ones exist, which is what lets the
 * same package serve this mod and any other. Registering a real module means adding
 * a line to {@link #registry()} - the browser picks it up with no layout changes and
 * a new category needs nothing but a new string.
 */
public class DragsimUtilsClient implements ClientModInitializer {

    /** Right Shift by default - away from anything vanilla binds and easy to hit. */
    /**
     * The menu key, as a real KeyMapping so it sits in Controls like any other mod's
     * and can be rebound there. Still POLLED rather than consumed: a KeyMapping's own
     * click counter is dead while a screen is open, and the menu has to be openable
     * from anywhere. The mapping is only where the choice of key lives.
     */
    private static final KeyMapping OPEN = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.dragsimutils.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("dragsimutils", "dragsimutils"))));

    private static ModuleRegistry registry;
    private boolean keyWasDown = false;
    /** Which bound keys were down last tick, so a held key fires once. */
    private final java.util.Set<Integer> heldBinds = new java.util.HashSet<>();

    /** Built once so a toggle survives closing and reopening the screen. */
    public static ModuleRegistry registry() {
        if (registry == null) {
            registry = new ModuleRegistry();
            registry.replace(StoragePreview.module());
            registry.replace(VoidgloomHelper.module());
            registry.replace(DragonTimer.module());
            registry.replace(Protector.module());
            registry.replace(BossDrops.module());
            registry.replace(BossDrops.slayerProfitModule());
            registry.replace(DiscordPresence.module());
            registry.replace(Slayer.killTimerModule());
            registry.replace(SlayerSpawnMarker.module());
            registry.replace(RevenantSpawnTiles.module());
            registry.replace(ZealotTracker.module());
            registry.insertBefore("dragon.timer", CacheTracker.module());
            registry.replace(DungeonMap.module());
            registry.replace(ThreeWeirdos.module());
            registry.replace(TicTacToe.module());
            registry.replace(Quiz.module());
            registry.replace(HigherOrLower.module());
            registry.replace(BloodRoom.module());
            registry.add(KeyAlert.module());
            registry.replace(IcePath.module());
            registry.replace(TerminalSolver.module());
            registry.replace(Alerts.module());
            registry.replace(EyeGuard.module());
            registry.replace(ArmorGuard.module());
            registry.replace(DamageNumbers.module());
            registry.replace(Beacon.module());
            registry.replace(ExplosionParticles.module());
            registry.replace(RarityOutline.module());
            registry.replace(Fullbright.module());
            registry.replace(SlotLock.module());
            registry.replace(InventoryGlass.module());
            registry.add(ShinyNodes.module());
            registry.replace(InventoryPreview.module());
            registry.replace(EquipmentHud.armorModule());
            registry.replace(EquipmentHud.petModule());
            registry.replace(Etherwarp.module());
            registry.replace(Cooldowns.module());
            registry.replace(LootAlerts.module());
            registry.replace(GlobalChat.module());
            registry.replace(LootFilter.module());
            registry.replace(LootRoll.module());
            registry.replace(AbilitySpam.module());
            registry.replace(CopyChat.module());
            registry.replace(MiningSession.module());
            registry.replace(PowderTracker.module());
            registry.replace(ForgeRecipes.module());
            registry.replace(Recipes.module());
            registry.replace(ServerStats.module());
            registry.replace(MathSolver.module());
            registry.replace(CommandBinds.module());
            registry.replace(MenuButton.module());
            registry.replace(com.dragsimutils.profile.ProfileClient.module());
            // Keep the old modules registered for their config and keybind ids. Their
            // grouped pages change only what the browser shows, not how the features run.
            registry.insertBefore("qol.lootfilter", LootChat.module());
            registry.hide("qol.lootfilter").hide("qol.lootroll");
            registry.insertBefore("visual.fullbright", VisualTweaks.module());
            registry.hide("visual.fullbright").hide("visual.inventoryGlass")
                    .hide("visual.explosionParticles").hide("visual.shinyNodes");
            com.dragsimutils.ui.FeatureCatalog.publicLayout(registry);
            Config.load(registry);
        }
        return registry;
    }

    @Override
    public void onInitializeClient() {
        LegacyConfigMigration.importSettings(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        Drops.init();
        DungeonContext.init();
        KeyAlert.init();
        HudLayout.init();
        com.dragsimutils.hud.ChatHudDrag.init();
        StoragePreview.init();
        SnapshotStore.init();
        StorageSearch.init();
        VoidgloomHelper.init();
        DragonTimer.init();
        MenuButton.init();
        Protector.init();
        BossDrops.init();
        DiscordPresence.init();
        EyeGuard.init();
        ArmorGuard.init();
        DamageNumbers.init();
        GlobalChat.init();
        LootFilter.init();
        LootRoll.init();
        AbilitySpam.init();
        CopyChat.init();
        MiningSession.init();
        CacheTracker.init();
        PowderTracker.init();
        VoidFragments.init();
        Pets.init();
        Recipes.init();
        ForgeRecipes.init();
        ServerStats.init();
        SlotLock.load();
        SlotLock.init();
        InventoryPreview.init();
        EquipmentHud.init();
        NameGradient.init();
        Area.init();
        ShinyNodes.init();
        LootAlerts.init();
        Alerts.init();
        MathSolver.init();
        com.dragsimutils.profile.ProfileClient.init();
        Beacon.init();
        RarityOutline.init();
        Etherwarp.init();
        Cooldowns.init();
        Slayer.init();
        SlayerSpawnMarker.init();
        RevenantSpawnTiles.init();
        ZealotTracker.init();
        DungeonMap.init();
        ThreeWeirdos.init();
        TicTacToe.init();
        Quiz.init();
        HigherOrLower.init();
        BloodRoom.init();
        IcePath.init();
        TerminalSolver.init();
        StorageOverview.init();

        // One popup for the whole mod, drawn last so it sits over every readout. Any
        // module can raise it; only one shows at a time, because two things shouting
        // at once is the same as neither.
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("dragsimutils", "alert"),
                (g, delta) -> Alert.draw(g));

        // Last of all, so a toast sits over every readout including the popup above.
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("dragsimutils", "toast"),
                (g, delta) -> Toast.draw(g));

        // Saved on exit, and again whenever a Dragsim Utils screen closes - quitting the
        // game is not the only way a session ends, and a crash after an hour of tuning
        // should not cost the hour.
        // /dragsimutils or /du opens the browser too - a key can be eaten by another mod or
        // an overlay, and a command cannot. Opened next tick, because the chat screen
        // that ran the command closes itself after this returns.
        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, ctx) -> {
            for (String name : new String[]{"dragsimutils", "du"}) {
                dispatcher.register(net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal(name).executes(c -> {
                    Minecraft mc = Minecraft.getInstance();
                    mc.schedule(() -> open(mc));
                    return 1;
                }));
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Config.save(registry()));
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (screen instanceof DragsimUtilsScreen || screen instanceof SettingsScreen || screen instanceof HudPlacementScreen) {
                ScreenEvents.remove(screen).register(s -> Config.save(registry()));
            }
        });

        // Build the registry now rather than on the first Right Shift, because building
        // it is what loads the config. Left lazy, every saved toggle and slider sat at
        // its default until the menu was first opened - a module saved on stayed off
        // all session and then came alive the moment the menu closed. After the inits
        // above, since restoring HUD positions needs the readouts registered first.
        registry();
    }

    /**
     * Back to wherever it was closed from: the browser as it was scrolled, or the
     * settings page that was open, with the browser behind it for Back.
     */
    private static void open(Minecraft client) {
        DragsimUtilsScreen browser = new DragsimUtilsScreen("Dragsim Utils", registry());
        String open = SettingsScreen.openModule();
        Module page = open == null ? null
                : registry().all().stream().filter(m -> m.id().equals(open)).findFirst().orElse(null);
        client.setScreen(page == null ? browser : new SettingsScreen(browser, "Dragsim Utils", page));
    }

    private void onTick(Minecraft client) {
        if (client.getWindow() == null) return;
        // Polled rather than registered as a KeyMapping on purpose: a KeyMapping only
        // fires while no screen is open, and this has to be openable from the pause
        // menu later. The edge check is what stops one press opening it every tick.
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(OPEN);
        boolean down = !OPEN.isUnbound() && (key.getType() == InputConstants.Type.MOUSE
                ? GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS
                : InputConstants.isKeyDown(client.getWindow(), key.getValue()));
        if (down && !keyWasDown && (client.screen == null && client.level != null)) open(client);
        keyWasDown = down;

        // Bound keys are polled here too, and only with no screen open - otherwise the
        // press that binds a key would also fire whatever it was just bound to.
        if (client.screen == null && client.level != null) fireBinds(client);
    }

    /**
     * Run whatever the bound keys are for, once per press.
     *
     * Walks the registry rather than keeping a second map of id to action: the ids are
     * built from the modules themselves, so a module renamed or removed cannot leave a
     * key pointing at something that is no longer there.
     */
    private void fireBinds(Minecraft client) {
        if (Keybinds.all().isEmpty()) return;
        java.util.Set<Integer> down = new java.util.HashSet<>();
        for (int key : Keybinds.all().values()) {
            if (Keybinds.isDown(client.getWindow(), key)) down.add(key);
        }
        for (Module m : registry().all()) {
            if (pressed(down, Keybinds.get(Keybinds.moduleId(m))) && m.togglable()) {
                m.toggle();
                Toast.toggled(m.title(), m.isEnabled());
            }
        }
        for (Module m : registry().all()) {
            if (!m.implemented() || m.group() || !m.isEnabled() || !m.hasSettings()) continue;
            for (Setting s : m.settings()) {
                if (s instanceof Setting.KeyAction a && pressed(down, Keybinds.get(a.id()))) {
                    a.run().run();
                }
            }
        }
        for (Setting.Command c : CommandBinds.all()) {
            if (pressed(down, Keybinds.get(c.id()))) CommandBinds.fire(c);
        }
        heldBinds.clear();
        heldBinds.addAll(down);
    }

    private boolean pressed(java.util.Set<Integer> down, int key) {
        return key != Keybinds.NONE && down.contains(key) && !heldBinds.contains(key);
    }

}
