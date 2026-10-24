package com.dragsimutils.profile;

import com.google.gson.*;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import com.dragsimutils.ui.ProfileScreen;
import net.minecraft.client.Minecraft;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

/** Asynchronous, size-bounded reads of public Dragsim profiles. */
public final class ProfileClient {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private static CompletableFuture<JsonObject> catalog;
    private static final ProfileNames NAMES = new ProfileNames(FabricLoader.getInstance().getConfigDir().resolve("dragsimutils/profile-players.txt"));
    private static long catalogAt;
    private record Cached(long at, JsonObject value) { }
    private static final Map<String, Cached> profiles = new LinkedHashMap<>(16, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Cached> entry) { return size() > 16; }
    };
    private ProfileClient() { }
    public static com.dragsimutils.ui.Module module() {
        return com.dragsimutils.ui.Module.group("qol.profiles","Player profiles","Browse Dragsim players with /pv.","General",List.of(
                new com.dragsimutils.ui.Setting.Action("Your profile","Open your saved Dragsim profile.","Open",() -> open(Minecraft.getInstance().getUser().getName()))));
    }

    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> ProfileCommands.register(
                dispatcher, source -> source.getClient().getUser().getName(),
                source -> NAMES.withOnline(source.getClient().getConnection().getSuggestionsProvider().getOnlinePlayerNames()),
                ProfileClient::open, ProfileClient::toServer));
    }

    /**
     * Sends a command to the server as the plain command packet. Not sendCommand: Fabric runs
     * client commands inside it first, so "/pv 1" came straight back to this /pv, which sent it
     * again, until the stack ran out and the game crashed (2026-10-24, profile select screen).
     */
    private static void toServer(String command) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection != null) connection.send(new net.minecraft.network.protocol.game.ServerboundChatCommandPacket(command));
    }

    public static int open(String player) {
        Minecraft mc = Minecraft.getInstance();
        mc.schedule(() -> mc.setScreen(new ProfileScreen(mc.screen, player)));
        return 1;
    }

    public static synchronized CompletableFuture<JsonObject> catalog() {
        if (catalog == null || catalog.isCompletedExceptionally() || System.currentTimeMillis()-catalogAt > 6*3600_000) {
            catalogAt = System.currentTimeMillis();
            catalog = get("https://dragsim.co/data/catalog.json", 8_000_000);
        }
        return catalog;
    }

    private static CompletableFuture<JsonObject> boards;
    private static long boardsAt;

    /** Public rankings for the profile viewer, cached for five minutes. */
    public static synchronized CompletableFuture<JsonObject> boards() {
        if (boards == null || boards.isCompletedExceptionally() || System.currentTimeMillis()-boardsAt > 300_000) {
            boardsAt = System.currentTimeMillis();
            boards = get("https://dragsim.co/api/home", 2_000_000).thenApply(home -> {
                JsonObject all = ProfileData.object(home, "boards").deepCopy();
                all.remove("networth");
                for (JsonElement board : all.asMap().values()) if (board.isJsonArray()) {
                    for (JsonElement row : board.getAsJsonArray()) if (row.isJsonObject())
                        NAMES.include(ProfileData.text(row.getAsJsonObject(), "username", ""));
                }
                return all;
            });
        }
        return boards;
    }

    /** A player's position on one board, or -1. A board ranks each player's best profile, so
     *  a row counts for the profile on screen only when it is that profile: by id where the
     *  board names one (networth), otherwise by the board's value being this profile's own
     *  number to within 5% (the board refreshes less often than the profile). */
    public static int position(JsonObject boards, String board, String player, String profileId, double value) {
        if (boards == null || player == null || !boards.has(board) || !boards.get(board).isJsonArray()) return -1;
        for (JsonElement element : boards.getAsJsonArray(board)) {
            if (!element.isJsonObject()) continue;
            JsonObject row = element.getAsJsonObject();
            if (!ProfileData.text(row, "username", "").equalsIgnoreCase(player)) continue;
            if (row.has("profile_id") && profileId != null && !profileId.isEmpty()
                    && !ProfileData.text(row, "profile_id", "").equalsIgnoreCase(profileId)) return -1;
            double ranked = ProfileData.number(row, "value", Double.NaN);
            if (value >= 0 && Double.isFinite(ranked) && Math.abs(ranked-value) > Math.max(1, Math.abs(value)*.05)) return -1;
            return ProfileData.integer(row, "position", -1);
        }
        return -1;
    }

    public static CompletableFuture<ProfileData> profile(String player, String profile, boolean force) {
        if (!player.matches("[A-Za-z0-9_]{1,16}")) return CompletableFuture.failedFuture(new IllegalArgumentException("Enter a Minecraft username."));
        if (!profile.isEmpty() && !profile.matches("[0-9a-fA-F-]{36}")) return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid profile."));
        // Main only: the beta server's profiles are test data nobody asks to see.
        String path = "https://dragsim.co/api/player/"+player+"?mode=main"
                +(profile.isEmpty() ? "" : "&profile="+profile);
        synchronized (profiles) {
            Cached cached = profiles.get(path);
            if (!force && cached != null && System.currentTimeMillis()-cached.at() < 30_000)
                return CompletableFuture.completedFuture(new ProfileData(cached.value()));
        }
        return get(path, 6_000_000).thenApply(json -> {
            ProfileData result = new ProfileData(json);
            NAMES.remember(result.player);
            synchronized (profiles) { profiles.put(path, new Cached(System.currentTimeMillis(), json)); }
            return result;
        });
    }

    private static CompletableFuture<JsonObject> get(String url, int maximum) {
        HttpRequest request = HttpRequest.newBuilder(NetworkPolicy.allowedUri(url)).timeout(Duration.ofSeconds(18))
                .header("User-Agent", "Dragsim Utils-Profile/1").header("Accept", "application/json").GET().build();
        return HTTP.sendAsync(request, BoundedJsonBody.handler(maximum)).thenApply(response -> {
            if (response.body().length() > maximum) throw new CompletionException(new IllegalArgumentException("Response was too large."));
            JsonObject body;
            try { body = RemoteJson.object(response.body()); }
            catch (RuntimeException error) { throw new CompletionException(new IllegalArgumentException("Profile service returned an invalid response.")); }
            // A null error field is not a failure message.
            if (response.statusCode() != 200 || body.has("error") && !body.get("error").isJsonNull())
                throw new CompletionException(new IllegalArgumentException(ProfileData.text(body, "error", "Profile service unavailable.")));
            return body;
        });
    }

    public static String error(Throwable error) {
        while (error.getCause() != null) error = error.getCause();
        if (error instanceof java.net.http.HttpTimeoutException || error instanceof java.net.ConnectException)
            return "Could not reach Dragsim. Try again.";
        return error.getMessage() == null ? "Could not load this profile." : error.getMessage();
    }
}
