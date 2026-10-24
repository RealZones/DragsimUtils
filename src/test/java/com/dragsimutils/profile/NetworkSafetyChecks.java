package com.dragsimutils.profile;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;

/** Exercise hostile inputs without contacting a network service. */
public final class NetworkSafetyChecks {
    private static int checks;
    public static void main(String[] args) {
        NetworkPolicy.allowedUri("https://dragsim.co/api/player/Imfear?mode=main"); checks++;
        NetworkPolicy.allowedUri("https://dragsim.co/data/catalog.json"); checks++;
        for (String bad : List.of("http://dragsim.co/api/home", "https://example.com/data/catalog.json", "https://dragsim.co/api/leaderboards/networth",
                "https://dragsim.co.evil.test/api/home", "https://dragsim.co@evil.test/api/home",
                "https://dragsim.co/api/player/../../home", "https://192.0.2.1/data/catalog.json", "https://dragsim.co/api/home?url=evil",
                "https://dragsim.co/api/player/Imfear?mode=main&url=evil", "https://dragsim.co/api/home#fragment")) {
            try { NetworkPolicy.allowedUri(bad); throw new AssertionError("Accepted " + bad); }
            catch (IllegalArgumentException expected) { checks++; }
        }
        BoundedJsonBody good = new BoundedJsonBody(8);
        Subscription first = new Subscription(); good.onSubscribe(first);
        good.onNext(List.of(ByteBuffer.wrap("{\"x\":1}".getBytes(StandardCharsets.UTF_8)))); good.onComplete();
        require(good.getBody().toCompletableFuture().join().equals("{\"x\":1}"), "valid response retained");
        BoundedJsonBody hostile = new BoundedJsonBody(8);
        Subscription second = new Subscription(); hostile.onSubscribe(second);
        hostile.onNext(List.of(ByteBuffer.wrap(new byte[5])));
        hostile.onNext(List.of(ByteBuffer.wrap(new byte[5])));
        require(second.cancelled && hostile.getBody().toCompletableFuture().isCompletedExceptionally(), "chunked overflow rejected during transfer");
        BoundedJsonBody exact = new BoundedJsonBody(8); exact.onSubscribe(new Subscription());
        exact.onNext(List.of(ByteBuffer.wrap(new byte[8]))); exact.onComplete();
        require(exact.getBody().toCompletableFuture().join().length() == 8, "exact boundary accepted");
        require(RemoteJson.object("{\"message\":\"[braces] and \\\"quotes\\\"\"}").has("message"), "quoted text accepted");
        for (String bad : List.of("{\"x\":1} {\"x\":2}", "{x:1}", "{\"x\":NaN}", "{\"x\":"+"[".repeat(70)+"0"+"]".repeat(70)+"}")) {
            try { RemoteJson.object(bad); throw new AssertionError("Accepted invalid or deeply nested JSON"); }
            catch (RuntimeException expected) { checks++; }
        }
        System.out.println(checks + " network safety checks passed");
    }

    private static void require(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        checks++;
    }
    private static final class Subscription implements Flow.Subscription {
        boolean cancelled;
        @Override public void request(long count) { }
        @Override public void cancel() { cancelled = true; }
    }
}
