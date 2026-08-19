package com.sonnesen;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Opaque cursor for {@code ListCustomers} pagination: just the last customer
 * id seen on the previous page, base64-encoded so clients treat it as an
 * opaque string rather than relying on it being a raw id.
 */
public final class PageToken {

    private PageToken() {
    }

    /** @return the id to resume after, or 0 (start from the beginning) for a blank token. */
    public static long decode(String token) {
        if (token == null || token.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new InvalidPageTokenException(token, e);
        }
    }

    public static String encode(long lastIdOnPage) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(Long.toString(lastIdOnPage).getBytes(StandardCharsets.UTF_8));
    }
}
