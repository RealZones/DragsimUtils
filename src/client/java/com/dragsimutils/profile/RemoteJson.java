package com.dragsimutils.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.StringReader;

/** Public web responses are data, with bounded nesting and no lenient JSON extensions. */
final class RemoteJson {
    private RemoteJson() { }

    static JsonObject object(String text) {
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setStrictness(Strictness.STRICT);
            // The documented responses are shallow; a byte limit alone still allows
            // pathological nesting to reach later recursive item-processing code.
            reader.setNestingLimit(64);
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
            if (reader.peek() != JsonToken.END_DOCUMENT)
                throw new IllegalArgumentException("Unexpected data after JSON response");
            return object;
        } catch (IOException | IllegalStateException error) {
            throw new IllegalArgumentException("Invalid JSON response", error);
        }
    }
}
