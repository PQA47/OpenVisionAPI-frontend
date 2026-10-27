package com.openvisionapi.app;

import java.util.Locale;

/** Basic client-side validation for image uploads. */
public final class ImageFileValidator {
    public static final long MAX_IMAGE_BYTES = 10L * 1024L * 1024L;
    private ImageFileValidator() {}

    public static boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null) return false;
        String value = mimeType.toLowerCase(Locale.ROOT);
        return value.equals("image/jpeg") || value.equals("image/jpg") || value.equals("image/png") || value.equals("image/webp");
    }

    public static boolean isValidSize(long bytes) {\n        return bytes > 0 && bytes <= MAX_IMAGE_BYTES;\n    }\n\n    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
