package com.dragsimutils.profile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

/** Enforces the response byte limit during transfer, before a complete body is allocated. */
final class BoundedJsonBody implements HttpResponse.BodySubscriber<String> {
    private final int maximum;
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final CompletableFuture<String> result = new CompletableFuture<>();
    private Flow.Subscription subscription;

    BoundedJsonBody(int maximum) {
        if (maximum <= 0) throw new IllegalArgumentException("A positive response limit is required");
        this.maximum = maximum;
    }

    static HttpResponse.BodyHandler<String> handler(int maximum) {
        return info -> {
            BoundedJsonBody body = new BoundedJsonBody(maximum);
            try {
                if (info.headers().firstValueAsLong("Content-Length").orElse(0) > maximum)
                    body.reject();
            } catch (NumberFormatException error) {
                body.reject();
            }
            return body;
        };
    }

    @Override public CompletionStage<String> getBody() { return result; }

    @Override public void onSubscribe(Flow.Subscription incoming) {
        if (subscription != null || result.isDone()) { incoming.cancel(); return; }
        subscription = incoming;
        incoming.request(1);
    }

    @Override public void onNext(List<ByteBuffer> chunks) {
        if (result.isDone()) return;
        for (ByteBuffer chunk : chunks) {
            if (chunk.remaining() > maximum - bytes.size()) { reject(); return; }
            byte[] part = new byte[chunk.remaining()];
            chunk.get(part);
            bytes.writeBytes(part);
        }
        subscription.request(1);
    }

    private void reject() {
        if (subscription != null) subscription.cancel();
        result.completeExceptionally(new IOException("Response exceeds the allowed size"));
    }

    @Override public void onError(Throwable error) { result.completeExceptionally(error); }
    @Override public void onComplete() { result.complete(bytes.toString(StandardCharsets.UTF_8)); }
}
