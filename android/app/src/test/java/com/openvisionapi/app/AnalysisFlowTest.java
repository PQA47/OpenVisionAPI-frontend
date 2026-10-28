package com.openvisionapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class AnalysisFlowTest {
    @Test public void mockCanRunWithoutImageAndBlocksWhileProcessing() {
        assertTrue(AnalysisFlow.canSubmit(true, false, false));
        assertFalse(AnalysisFlow.canSubmit(true, false, true));
    }

    @Test public void liveModeStillRequiresImage() {
        assertFalse(AnalysisFlow.canSubmit(false, false, false));
        assertTrue(AnalysisFlow.canSubmit(false, true, false));
    }

    @Test public void mockSuccessAndEmptyProduceParseableExpectedResultsWithoutCallingLiveApi() throws Exception {
        AtomicInteger liveCalls = new AtomicInteger();
        AnalysisFlow.LiveRequest forbiddenLiveCall = () -> {
            liveCalls.incrementAndGet();
            throw new AssertionError("MOCK/DEMO must not execute the live request");
        };

        ApiResponseParser success = ApiResponseParser.parse(
                AnalysisFlow.execute(true, MockApiResponses.SUCCESS, forbiddenLiveCall).body);
        assertTrue(success.hasUsefulResults);
        assertEquals("Bicycle", success.labels.get(0).name);

        ApiResponseParser empty = ApiResponseParser.parse(
                AnalysisFlow.execute(true, MockApiResponses.EMPTY, forbiddenLiveCall).body);
        assertFalse(empty.hasUsefulResults);
        assertEquals(0, empty.labels.size());
        assertEquals(0, liveCalls.get());
    }

    @Test public void mockErrorIsRecoverableAndNeverCallsLiveApi() throws Exception {
        AtomicInteger liveCalls = new AtomicInteger();
        try {
            AnalysisFlow.execute(true, MockApiResponses.ERROR, () -> {
                liveCalls.incrementAndGet();
                return null;
            });
            fail("Expected simulated error");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("No API request was sent"));
        }
        assertEquals(0, liveCalls.get());
        assertTrue(AnalysisFlow.canSubmit(true, false, false));
    }

    @Test public void liveModeDelegatesOnlyAfterCallerHasValidImage() throws Exception {
        AtomicInteger liveCalls = new AtomicInteger();
        assertFalse(AnalysisFlow.canSubmit(false, false, false));
        OpenVisionApiClient.ApiResult result = AnalysisFlow.execute(false, MockApiResponses.SUCCESS, () -> {
            liveCalls.incrementAndGet();
            return new OpenVisionApiClient.ApiResult(200, "live");
        });
        assertEquals(200, result.statusCode);
        assertEquals(1, liveCalls.get());
    }
}
