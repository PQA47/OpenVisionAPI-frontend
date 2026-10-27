package com.openvisionapi.app;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class NativeAppInstrumentedTest {

    @Test
    public void appUsesExpectedPackage() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.openvisionapi.app", appContext.getPackageName());
    }

    @Test
    public void mockSuccessResponseParsesLabelsAndConfidence() throws Exception {
        ApiResponseParser parsed = ApiResponseParser.parse(
                MockApiResponses.responseFor(MockApiResponses.SUCCESS));

        assertTrue(parsed.hasUsefulResults);
        assertEquals(2, parsed.labels.size());
        assertEquals("Bicycle", parsed.labels.get(0).name);
        assertEquals("97.0% confidence", parsed.labels.get(0).confidence);
    }

    @Test
    public void mockEmptyResponseParsesAsNoResults() throws Exception {
        ApiResponseParser parsed = ApiResponseParser.parse(
                MockApiResponses.responseFor(MockApiResponses.EMPTY));

        assertFalse(parsed.hasUsefulResults);
        assertTrue(parsed.labels.isEmpty());
        assertTrue(parsed.textLines.isEmpty());
    }

    @Test
    public void mockErrorScenarioIsExplicitlyMarked() {
        assertTrue(MockApiResponses.errorMessage().contains("MOCK/DEMO"));
        assertTrue(MockApiResponses.errorMessage().contains("No API request was sent"));
    }
}
