package com.openvisionapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ImageFileValidatorTest {

    @Test
    public void acceptsSupportedImageTypes() {
        assertTrue(ImageFileValidator.isSupportedMimeType("image/png"));
        assertTrue(ImageFileValidator.isSupportedMimeType("image/jpeg"));
        assertTrue(ImageFileValidator.isSupportedMimeType("image/jpg"));
        assertTrue(ImageFileValidator.isSupportedMimeType("image/webp"));
    }

    @Test
    public void mimeTypeCheckIsCaseInsensitive() {
        assertTrue(ImageFileValidator.isSupportedMimeType("IMAGE/PNG"));
        assertTrue(ImageFileValidator.isSupportedMimeType("Image/Jpeg"));
    }

    @Test
    public void rejectsUnsupportedOrMissingTypes() {
        assertFalse(ImageFileValidator.isSupportedMimeType(null));
        assertFalse(ImageFileValidator.isSupportedMimeType(""));
        assertFalse(ImageFileValidator.isSupportedMimeType("image/gif"));
        assertFalse(ImageFileValidator.isSupportedMimeType("image/svg+xml"));
        assertFalse(ImageFileValidator.isSupportedMimeType("image/heic"));
        assertFalse(ImageFileValidator.isSupportedMimeType("application/pdf"));
        assertFalse(ImageFileValidator.isSupportedMimeType("text/plain"));
        assertFalse(ImageFileValidator.isSupportedMimeType("image/*"));
        assertFalse(ImageFileValidator.isSupportedMimeType(" image/png"));
    }

    @Test
    public void sizeLimitIsTenMegabytes() {
        assertEquals(10L * 1024L * 1024L, ImageFileValidator.MAX_IMAGE_BYTES);
    }

    @Test
    public void validatesSizeBoundaries() {
        assertFalse("empty file", ImageFileValidator.isValidSize(0));
        assertFalse("negative", ImageFileValidator.isValidSize(-1));
        assertTrue(ImageFileValidator.isValidSize(1));
        assertTrue("exactly the limit", ImageFileValidator.isValidSize(ImageFileValidator.MAX_IMAGE_BYTES));
        assertFalse("one byte over", ImageFileValidator.isValidSize(ImageFileValidator.MAX_IMAGE_BYTES + 1));
    }

    @Test
    public void formatsFileSizes() {
        assertEquals("0 B", ImageFileValidator.formatBytes(0));
        assertEquals("500 B", ImageFileValidator.formatBytes(500));
        assertEquals("1023 B", ImageFileValidator.formatBytes(1023));
        assertEquals("1.0 KB", ImageFileValidator.formatBytes(1024));
        assertEquals("1.5 KB", ImageFileValidator.formatBytes(1536));
        assertEquals("1.0 MB", ImageFileValidator.formatBytes(1024 * 1024));
        assertEquals("10.0 MB", ImageFileValidator.formatBytes(ImageFileValidator.MAX_IMAGE_BYTES));
    }
}
