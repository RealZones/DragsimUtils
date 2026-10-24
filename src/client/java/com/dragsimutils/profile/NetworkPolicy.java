package com.dragsimutils.profile;

import java.net.URI;

/** The mod's direct web requests are limited to documented HTTPS data endpoints. */
final class NetworkPolicy {
    private NetworkPolicy() { }

    static URI allowedUri(String address) {
        URI uri = URI.create(address);
        if (!"https".equals(uri.getScheme()) || uri.getUserInfo() != null || uri.getFragment() != null
                || uri.getPort() != -1 && uri.getPort() != 443)
            throw new IllegalArgumentException("Only approved HTTPS data endpoints are allowed");
        String host = uri.getHost(), path = uri.getPath();
        if (host == null || path == null) throw new IllegalArgumentException("An absolute HTTPS endpoint is required");
        String query = uri.getRawQuery();
        boolean publicData = query == null && (path.equals("/data/catalog.json") || path.equals("/api/home"));
        boolean player = path.matches("/api/player/[A-Za-z0-9_]{1,16}") && query != null
                && query.matches("mode=main(?:&profile=[0-9a-fA-F-]{36})?");
        if (!"dragsim.co".equals(host) || !path.equals(uri.getRawPath()) || !publicData && !player)
            throw new IllegalArgumentException("Unapproved data endpoint");
        return uri;
    }
}
