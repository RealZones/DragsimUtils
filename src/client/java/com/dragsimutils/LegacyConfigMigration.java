package com.dragsimutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** One-time, local-only import from the previous mod's configuration names. */
public final class LegacyConfigMigration {
    private static final long MAX_FILE_BYTES = 32L * 1024 * 1024;
    private static final long MAX_TOTAL_BYTES = 256L * 1024 * 1024;
    private static final int MAX_FILES = 4096;

    private LegacyConfigMigration() { }

    public static void importSettings(Path configDirectory) {
        Path root = configDirectory.toAbsolutePath().normalize();
        Path destination = root.resolve("dragsimutils");
        Path marker = destination.resolve("legacy-import.done");
        if (Files.exists(marker, LinkOption.NOFOLLOW_LINKS)) return;
        try {
            if (Files.isSymbolicLink(destination)) throw new IOException("Configuration directory is a symbolic link");
            Files.createDirectories(destination);
            long[] copied = {0, 0};
            Path previous = root.resolve("endsight");
            if (Files.isDirectory(previous, LinkOption.NOFOLLOW_LINKS)) {
                // Files.walk does not follow symbolic links. The old files remain intact.
                try (var files = Files.walk(previous)) {
                    for (Path source : files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList()) {
                        Path target = destination.resolve(previous.relativize(source)).normalize();
                        if (!target.startsWith(destination)) throw new IOException("Configuration path escaped its directory");
                        copyIfMissing(source, target, destination, copied);
                    }
                }
            }
            if (Files.isDirectory(root)) {
                try (var files = Files.list(root)) {
                    for (Path source : files.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)).toList()) {
                        String name = source.getFileName().toString();
                        if (name.startsWith("endsight.") || name.startsWith("endsight-"))
                            copyIfMissing(source, root.resolve("dragsimutils" + name.substring("endsight".length())), root, copied);
                    }
                }
            }
            Files.writeString(marker, "Local configuration import completed. Original files were retained.\n",
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException | SecurityException error) {
            // An interrupted import retries missing files next launch; it never overwrites new preferences.
            System.err.println("[Dragsim Utils] Could not complete legacy settings import: " + error.getMessage());
        }
    }

    private static void copyIfMissing(Path source, Path target, Path boundary, long[] copied) throws IOException {
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return;
        long size = Files.size(source);
        if (size > MAX_FILE_BYTES || copied[0] >= MAX_FILES || copied[1] + size > MAX_TOTAL_BYTES)
            throw new IOException("Legacy settings exceed the import size limit");
        for (Path parent = target.getParent(); parent != null && parent.startsWith(boundary); parent = parent.getParent())
            if (Files.isSymbolicLink(parent)) throw new IOException("Configuration path contains a symbolic link");
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
        copied[0]++;
        copied[1] += size;
    }
}
