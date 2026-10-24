package com.dragsimutils;

import java.nio.file.Files;
import java.nio.file.Path;

/** Verify preservation and import boundaries using isolated local fixtures. */
public final class LegacyConfigMigrationChecks {
    public static void main(String[] args) throws Exception {
        Path temporary = Path.of("build", "tmp", "migration-checks");
        Files.createDirectories(temporary);
        Path root = Files.createTempDirectory(temporary, "config-");
        Files.createDirectories(root.resolve("endsight"));
        Files.createDirectories(root.resolve("dragsimutils"));
        Files.writeString(root.resolve("endsight.properties"), "browser.view=Mining\n");
        Files.writeString(root.resolve("endsight-storage.nbt"), "old-storage-fixture");
        Files.writeString(root.resolve("endsight/cache-tracker.json"), "old-cache-history");
        Files.writeString(root.resolve("endsight/drops.txt"), "old-order");
        Files.writeString(root.resolve("dragsimutils/drops.txt"), "new-order");
        Files.writeString(root.resolve("other-mod.properties"), "unrelated");
        LegacyConfigMigration.importSettings(root);
        require(Files.readString(root.resolve("dragsimutils.properties")).equals("browser.view=Mining\n"), "main settings imported");
        require(Files.readString(root.resolve("dragsimutils-storage.nbt")).equals("old-storage-fixture"), "storage imported");
        require(Files.readString(root.resolve("dragsimutils/cache-tracker.json")).equals("old-cache-history"), "history imported");
        require(Files.readString(root.resolve("dragsimutils/drops.txt")).equals("new-order"), "existing preferences retained");
        require(Files.exists(root.resolve("endsight/cache-tracker.json")), "old history retained");
        require(Files.readString(root.resolve("other-mod.properties")).equals("unrelated"), "unrelated file retained");
        Files.delete(root.resolve("dragsimutils/cache-tracker.json"));
        LegacyConfigMigration.importSettings(root);
        require(!Files.exists(root.resolve("dragsimutils/cache-tracker.json")), "reset history is not resurrected on later launches");
        require(!Config.ownsSettings(com.dragsimutils.ui.Module.group("qol.profiles", "Profiles", "", "", java.util.List.of())), "action-only profile groups have no persistent controls");
        require(!Config.ownsSettings(com.dragsimutils.ui.Module.group("page.previews", "Previews", "", "", java.util.List.of())), "presentation groups do not duplicate saved settings");
        System.out.println("9 configuration migration and ownership checks passed");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
