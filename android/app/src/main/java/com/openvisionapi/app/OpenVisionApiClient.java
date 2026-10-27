package com.openvisionapi.app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Multipart API client. Verify endpoint, field names, and services against the provider docs. */
public final class OpenVisionApiClient {
    private static final int CONNECT_TIMEOUT_MS = 15000;
    private static final int READ_TIMEOUT_MS = 45000;

    private OpenVisionApiClient() {}

    public static final class ApiResult {
        public final int statusCode;
        public final String body;

        ApiResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    public static ApiResult processImage(
            String endpoint,
            String apiKey,
            byte[] image,
            String filename,
            String mimeType
    ) throws IOException {
        if (endpoint == null || endpoint.trim().isEmpty()) {
            throw new IOException("API endpoint is not configured. Set IMAGE_API_URL in local.properties.");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IOException("API key is not configured. Set PIXICULAR_API_KEY in local.properties.");
        }
        if (image == null || image.length == 0) {
            throw new IOException("Select a valid image first.");
        }

        String boundary = "----OpenVisionBoundary" + UUID.randomUUID().toString().replace("-", "");
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setDoOutput(true);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try {
            try (OutputStream out = connection.getOutputStream()) {
                write(out, "--" + boundary + "\r\n");
                write(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + safe(filename) + "\"\r\n");
                write(out, "Content-Type: " + (mimeType == null ? "application/octet-stream" : mimeType) + "\r\n\r\n");
                out.write(image);
                write(out, "\r\n--" + boundary + "\r\n");
                write(out, "Content-Disposition: form-data; name=\"services\"\r\n\r\n");
                write(out, "detect-labels,detect-text\r\n");
                write(out, "--" + boundary + "--\r\n");
            }

            int status = connection.getResponseCode();
            InputStream stream = status >= 200 && status < 400
                    ? connection.getInputStream() : connection.getErrorStream();
            return new ApiResult(status, stream == null ? "" : readAll(stream));
        } finally {
            connection.disconnect();
        }
    }

    private static void write(OutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String readAll(InputStream stream) throws IOException {
        try (InputStream in = stream; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) return "image.jpg";
        return value.replace("\\", "_").replace("\"", "_")
                .replace("\r", "_").replace("\n", "_");
    }
}
