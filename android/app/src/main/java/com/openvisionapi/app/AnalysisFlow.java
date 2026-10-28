package com.openvisionapi.app;

import java.io.IOException;

/** Keeps mock execution isolated from the live network request path. */
final class AnalysisFlow {
    interface LiveRequest {
        OpenVisionApiClient.ApiResult execute() throws IOException;
    }

    private AnalysisFlow() {}

    static boolean canSubmit(boolean mockMode, boolean hasValidImage, boolean processing) {
        return !processing && (mockMode || hasValidImage);
    }

    static OpenVisionApiClient.ApiResult execute(boolean mockMode, int scenario, LiveRequest liveRequest)
            throws IOException {
        if (!mockMode) {
            if (liveRequest == null) throw new IOException("Live API request is unavailable.");
            return liveRequest.execute();
        }
        if (scenario == MockApiResponses.ERROR) throw new IOException(MockApiResponses.errorMessage());
        if (scenario != MockApiResponses.SUCCESS && scenario != MockApiResponses.EMPTY) {
            throw new IOException("Unknown MOCK/DEMO scenario.");
        }
        return new OpenVisionApiClient.ApiResult(200, MockApiResponses.responseFor(scenario));
    }
}
