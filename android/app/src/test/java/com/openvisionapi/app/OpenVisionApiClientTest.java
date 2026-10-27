package com.openvisionapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

/**
 * Exercises {@link OpenVisionApiClient} against a real HTTP server on localhost, covering the
 * success path, unsuccessful HTTP responses, malformed bodies, timeouts and connection failures.
 */
public class OpenVisionApiClientTest {
    private static final byte[] IMAGE = "FAKE-IMAGE-BYTES-0123456789".getBytes(StandardCharsets.ISO_8859_1);

    /** A regression that removes a timeout must fail the build, not hang it. */
    @Rule
    public Timeout globalTimeout = Timeout.seconds(30);

    private HttpServer server;
    private final AtomicReference<Captured> captured = new AtomicReference<>();

    /** What the server saw for the last request. */
    private static final class Captured {
        String method;
        String authorization;
        String contentType;
        String accept;
        String body; // ISO-8859-1 so binary bytes survive 1:1
    }

    @Before
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.start();
    }

    @After
    public void stopServer() {
        server.stop(0);
    }

    private String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1/detect";
    }

    private void respond(final int status, final String contentType, final String body) {
        server.createContext("/v1/detect", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                Captured c = new Captured();
                c.method = exchange.getRequestMethod();
                c.authorization = exchange.getRequestHeaders().getFirst("Authorization");
                c.contentType = exchange.getRequestHeaders().getFirst("Content-Type");
                c.accept = exchange.getRequestHeaders().getFirst("Accept");
                c.body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
                captured.set(c);

                if (body == null) {
                    exchange.sendResponseHeaders(status, -1);
                } else {
                    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                    if (contentType != null) exchange.getResponseHeaders().add("Content-Type", contentType);
                    exchange.sendResponseHeaders(status, bytes.length);
                    try (OutputStream out = exchange.getResponseBody()) {
                        out.write(bytes);
                    }
                }
                exchange.close();
            }
        });
    }

    private OpenVisionApiClient.ApiResult send() throws IOException {
        return OpenVisionApiClient.processImage(url(), "test-key", IMAGE, "cat.png", "image/png", 2000, 2000);
    }

    // ---- success -----------------------------------------------------------------------------

    @Test
    public void successfulRequestReturnsStatusAndBody() throws Exception {
        respond(200, "application/json", "{\"success\":true}");

        OpenVisionApiClient.ApiResult result = send();

        assertEquals(200, result.statusCode);
        assertTrue(result.isSuccess());
        assertEquals("{\"success\":true}", result.body);
    }

    @Test
    public void requestIsAuthenticatedMultipartPost() throws Exception {
        respond(200, "application/json", "{}");

        send();

        Captured c = captured.get();
        assertNotNull(c);
        assertEquals("POST", c.method);
        assertEquals("Bearer test-key", c.authorization);
        assertEquals("application/json", c.accept);
        assertTrue(c.contentType, c.contentType.startsWith("multipart/form-data; boundary=----OpenVisionBoundary"));
        assertTrue(c.body.contains("Content-Disposition: form-data; name=\"file\"; filename=\"cat.png\""));
        assertTrue(c.body.contains("Content-Type: image/png"));
        assertTrue("image bytes are sent unmodified", c.body.contains(new String(IMAGE, StandardCharsets.ISO_8859_1)));
        assertTrue(c.body.contains("name=\"services\""));
        assertTrue(c.body.contains("detect-labels,detect-text"));

        String boundary = c.contentType.substring(c.contentType.indexOf("boundary=") + "boundary=".length());
        assertTrue("body is terminated with the closing boundary", c.body.endsWith("--" + boundary + "--\r\n"));
    }

    @Test
    public void apiKeyIsTrimmed() throws Exception {
        respond(200, "application/json", "{}");

        OpenVisionApiClient.processImage(url(), "  spaced-key \n", IMAGE, "a.png", "image/png", 2000, 2000);

        assertEquals("Bearer spaced-key", captured.get().authorization);
    }

    @Test
    public void largestAllowedImageCanBeUploaded() throws Exception {
        respond(200, "application/json", "{\"success\":true}");
        byte[] big = new byte[(int) ImageFileValidator.MAX_IMAGE_BYTES];
        java.util.Arrays.fill(big, (byte) 7);

        OpenVisionApiClient.ApiResult result = OpenVisionApiClient.processImage(
                url(), "k", big, "big.jpg", "image/jpeg", 5000, 20000);

        assertEquals(200, result.statusCode);
        assertTrue(captured.get().body.length() > big.length);
    }

    @Test
    public void emptySuccessBodyIsReturnedAsEmptyString() throws Exception {
        respond(204, null, null);

        OpenVisionApiClient.ApiResult result = send();

        assertEquals(204, result.statusCode);
        assertTrue(result.isSuccess());
        assertEquals("", result.body);
    }

    // ---- filename / mime handling -------------------------------------------------------------

    @Test
    public void filenameCannotInjectHeaders() throws Exception {
        respond(200, "application/json", "{}");

        OpenVisionApiClient.processImage(url(), "k", IMAGE, "a\"b\r\nX-Evil: 1.png", "image/png", 2000, 2000);

        String body = captured.get().body;
        assertTrue(body, body.contains("filename=\"a_b__X-Evil: 1.png\""));
        assertFalse(body.contains("\r\nX-Evil"));
    }

    @Test
    public void missingFilenameFallsBackToDefault() throws Exception {
        respond(200, "application/json", "{}");

        OpenVisionApiClient.processImage(url(), "k", IMAGE, null, "image/png", 2000, 2000);
        assertTrue(captured.get().body.contains("filename=\"image.jpg\""));

        OpenVisionApiClient.processImage(url(), "k", IMAGE, "   ", "image/png", 2000, 2000);
        assertTrue(captured.get().body.contains("filename=\"image.jpg\""));
    }

    @Test
    public void missingMimeTypeFallsBackToOctetStream() throws Exception {
        respond(200, "application/json", "{}");

        OpenVisionApiClient.processImage(url(), "k", IMAGE, "a.png", null, 2000, 2000);

        assertTrue(captured.get().body.contains("Content-Type: application/octet-stream"));
    }

    // ---- unsuccessful HTTP responses -----------------------------------------------------------

    @Test
    public void clientErrorKeepsStatusAndErrorBody() throws Exception {
        respond(401, "application/json", "{\"success\":false,\"message\":\"Invalid API key\"}");

        OpenVisionApiClient.ApiResult result = send();

        assertEquals(401, result.statusCode);
        assertFalse(result.isSuccess());
        assertTrue(result.body.contains("Invalid API key"));
    }

    @Test
    public void rateLimitIsReportedAsFailure() throws Exception {
        respond(429, "application/json", "{\"message\":\"slow down\"}");

        OpenVisionApiClient.ApiResult result = send();

        assertEquals(429, result.statusCode);
        assertFalse(result.isSuccess());
    }

    @Test
    public void serverErrorWithoutBodyDoesNotCrash() throws Exception {
        respond(500, null, null);

        OpenVisionApiClient.ApiResult result = send();

        assertEquals(500, result.statusCode);
        assertFalse(result.isSuccess());
        assertEquals("", result.body);
    }

    @Test
    public void htmlErrorPageIsReturnedVerbatim() throws Exception {
        respond(502, "text/html", "<html><body>Bad Gateway</body></html>");

        OpenVisionApiClient.ApiResult result = send();

        assertEquals(502, result.statusCode);
        assertEquals("<html><body>Bad Gateway</body></html>", result.body);
    }

    // ---- malformed bodies (client must not parse or crash) -------------------------------------

    @Test
    public void malformedJsonWithSuccessStatusIsStillReturned() throws Exception {
        respond(200, "application/json", "{\"success\":tru");

        OpenVisionApiClient.ApiResult result = send();

        assertEquals(200, result.statusCode);
        assertEquals("{\"success\":tru", result.body);
    }

    @Test
    public void utf8BodyIsDecodedCorrectly() throws Exception {
        respond(200, "application/json", "{\"text\":\"Xin chào — 你好\"}");

        assertEquals("{\"text\":\"Xin chào — 你好\"}", send().body);
    }

    // ---- network failures ----------------------------------------------------------------------

    @Test
    public void slowServerCausesReadTimeout() throws Exception {
        server.createContext("/v1/detect", exchange -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ignored) {
                // server is being stopped
            }
            exchange.close();
        });

        long started = System.nanoTime();
        try {
            OpenVisionApiClient.processImage(url(), "k", IMAGE, "a.png", "image/png", 2000, 300);
            fail("expected SocketTimeoutException");
        } catch (SocketTimeoutException expected) {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            assertTrue("timed out promptly, took " + elapsedMs + " ms", elapsedMs < 1400);
        }
    }

    @Test
    public void closedPortCausesConnectionFailure() throws Exception {
        String deadUrl = url();
        server.stop(0);

        try {
            OpenVisionApiClient.processImage(deadUrl, "k", IMAGE, "a.png", "image/png", 2000, 2000);
            fail("expected ConnectException");
        } catch (ConnectException expected) {
            // success
        }
    }

    @Test
    public void serverClosingConnectionWithoutResponseFails() throws Exception {
        server.createContext("/v1/detect", exchange -> exchange.getRequestBody().readAllBytes());
        // The handler returns without sending headers; HttpServer then closes the exchange.

        try {
            OpenVisionApiClient.ApiResult result = send();
            // Some JDKs surface this as an IOException, others as an empty response; both are safe
            // as long as nothing hangs or throws an unchecked exception.
            assertFalse(result.isSuccess() && result.body.length() > 0);
        } catch (IOException expected) {
            // acceptable
        }
    }

    // ---- invalid input -------------------------------------------------------------------------

    @Test
    public void missingEndpointIsRejectedBeforeAnyNetworkCall() {
        assertRejected(null, "k", IMAGE, "endpoint");
        assertRejected("", "k", IMAGE, "endpoint");
        assertRejected("   ", "k", IMAGE, "endpoint");
    }

    @Test
    public void missingApiKeyIsRejected() {
        assertRejected("http://127.0.0.1:1/x", null, IMAGE, "API key");
        assertRejected("http://127.0.0.1:1/x", "", IMAGE, "API key");
        assertRejected("http://127.0.0.1:1/x", "   ", IMAGE, "API key");
    }

    @Test
    public void missingImageIsRejected() {
        assertRejected("http://127.0.0.1:1/x", "k", null, "valid image");
        assertRejected("http://127.0.0.1:1/x", "k", new byte[0], "valid image");
    }

    @Test
    public void malformedEndpointUrlFailsWithIoException() {
        try {
            OpenVisionApiClient.processImage("not a url", "k", IMAGE, "a.png", "image/png", 500, 500);
            fail("expected IOException");
        } catch (IOException expected) {
            // MalformedURLException is an IOException
        }
    }

    private static void assertRejected(String endpoint, String key, byte[] image, String messagePart) {
        try {
            OpenVisionApiClient.processImage(endpoint, key, image, "a.png", "image/png", 500, 500);
            fail("expected IOException mentioning " + messagePart);
        } catch (IOException expected) {
            assertNotNull(expected.getMessage());
            assertTrue(expected.getMessage(), expected.getMessage().contains(messagePart));
        }
    }
}
