package com.openvisionapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.Test;

public class ApiErrorMessagesTest {

    @Test
    public void timeoutMentionsTimeout() {
        String message = ApiErrorMessages.forException(new SocketTimeoutException("Read timed out"));
        assertTrue(message, message.contains("timed out"));
    }

    @Test
    public void offlineErrorsDoNotLeakHostnameOnly() {
        // UnknownHostException's getMessage() is just the host name; it must not be shown verbatim.
        String message = ApiErrorMessages.forException(new UnknownHostException("api.pixicular.com"));
        assertFalse(message, message.equals("api.pixicular.com"));
        assertTrue(message, message.contains("internet connection"));
        assertTrue(ApiErrorMessages.forException(new ConnectException("refused")).contains("internet connection"));
    }

    @Test
    public void tlsFailureHasOwnMessage() {
        String message = ApiErrorMessages.forException(new SSLHandshakeException("bad cert"));
        assertTrue(message, message.contains("secure connection"));
    }

    @Test
    public void readableIoMessagesArePassedThrough() {
        assertEquals("API key is not configured.",
                ApiErrorMessages.forException(new IOException("API key is not configured.")));
    }

    @Test
    public void nullOrBlankMessagesNeverProduceNullText() {
        assertNotNull(ApiErrorMessages.forException(new IOException()));
        assertFalse(ApiErrorMessages.forException(new IOException()).contains("null"));
        assertFalse(ApiErrorMessages.forException(new IOException("  ")).contains("null"));
        assertFalse(ApiErrorMessages.forException(new RuntimeException()).contains("null"));
        assertNotNull(ApiErrorMessages.forException(null));
    }

    @Test
    public void httpStatusMessages() {
        assertTrue(ApiErrorMessages.forHttpStatus(400).contains("400"));
        assertTrue(ApiErrorMessages.forHttpStatus(401).contains("API key"));
        assertTrue(ApiErrorMessages.forHttpStatus(403).contains("API key"));
        assertTrue(ApiErrorMessages.forHttpStatus(404).contains("IMAGE_API_URL"));
        assertTrue(ApiErrorMessages.forHttpStatus(413).contains("too large"));
        assertTrue(ApiErrorMessages.forHttpStatus(429).contains("429"));
        assertTrue(ApiErrorMessages.forHttpStatus(500).contains("500"));
        assertTrue(ApiErrorMessages.forHttpStatus(503).contains("503"));
        assertTrue(ApiErrorMessages.forHttpStatus(418).contains("418"));
    }
}
