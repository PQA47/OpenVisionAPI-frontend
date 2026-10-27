package com.openvisionapi.app;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/** Maps network exceptions and HTTP status codes to short, user-readable messages. */
public final class ApiErrorMessages {
    private ApiErrorMessages() {}

    /** Message for a request that never produced an HTTP response. Never returns null. */
    public static String forException(Throwable error) {
        if (error instanceof SocketTimeoutException) {
            return "The request timed out. Check your connection and try again.";
        }
        if (error instanceof UnknownHostException || error instanceof ConnectException) {
            return "Could not reach the server. Check your internet connection and try again.";
        }
        if (error instanceof SSLException) {
            return "A secure connection could not be established. Try again later.";
        }
        if (error instanceof IOException && error.getMessage() != null
                && !error.getMessage().trim().isEmpty()) {
            // Configuration / validation messages thrown by OpenVisionApiClient are already readable.
            return error.getMessage();
        }
        return "Something went wrong while processing the image. Please try again.";
    }

    /** Message for a non-2xx HTTP response. Never returns null. */
    public static String forHttpStatus(int status) {
        if (status == 400) return "The server rejected the request (HTTP 400). Try a different image.";
        if (status == 401 || status == 403) {
            return "The API key was rejected (HTTP " + status + "). Check PIXICULAR_API_KEY.";
        }
        if (status == 404) return "The API endpoint was not found (HTTP 404). Check IMAGE_API_URL.";
        if (status == 413) return "The server says the image is too large (HTTP 413).";
        if (status == 429) return "Too many requests or no credits left (HTTP 429). Please wait and try again.";
        if (status >= 500) return "The server had a problem (HTTP " + status + "). Please try again later.";
        return "API error (HTTP " + status + "). Please try again.";
    }
}
