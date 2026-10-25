package com.openvisionapi.app;
import org.junit.Test;
import static org.junit.Assert.*;
public class ImageFileValidatorTest {
 @Test public void supportsExpectedImageTypes(){assertTrue(ImageFileValidator.isSupportedMimeType("image/png"));assertTrue(ImageFileValidator.isSupportedMimeType("image/jpeg"));assertTrue(ImageFileValidator.isSupportedMimeType("image/webp"));assertFalse(ImageFileValidator.isSupportedMimeType("image/gif"));assertFalse(ImageFileValidator.isSupportedMimeType(null));}
 @Test public void validatesSizeBoundary(){assertFalse(ImageFileValidator.isValidSize(0));assertTrue(ImageFileValidator.isValidSize(1));assertTrue(ImageFileValidator.isValidSize(ImageFileValidator.MAX_IMAGE_BYTES));assertFalse(ImageFileValidator.isValidSize(ImageFileValidator.MAX_IMAGE_BYTES+1));}
 @Test public void formatsFileSizes(){assertEquals("500 B",ImageFileValidator.formatBytes(500));assertEquals("1.0 KB",ImageFileValidator.formatBytes(1024));}
}
