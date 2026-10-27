package com.openvisionapi.app;

import static org.junit.Assert.*;
import org.junit.Test;

public class NativeAppUnitTest {
    @Test public void validImageSizesAreAccepted() {
        assertTrue(ImageFileValidator.isValidSize(1));
        assertTrue(ImageFileValidator.isValidSize(ImageFileValidator.MAX_IMAGE_BYTES));
    }

    @Test public void invalidImageSizesAreRejected() {
        assertFalse(ImageFileValidator.isValidSize(0));
        assertFalse(ImageFileValidator.isValidSize(-1));
        assertFalse(ImageFileValidator.isValidSize(ImageFileValidator.MAX_IMAGE_BYTES + 1));
    }

    @Test public void mimeTypesAreCaseInsensitive() {
        assertTrue(ImageFileValidator.isSupportedMimeType("IMAGE/PNG"));
        assertTrue(ImageFileValidator.isSupportedMimeType("image/jpeg"));
        assertFalse(ImageFileValidator.isSupportedMimeType("image/gif"));
        assertFalse(ImageFileValidator.isSupportedMimeType(null));
    }
}
