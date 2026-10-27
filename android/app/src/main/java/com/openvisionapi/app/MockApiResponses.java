package com.openvisionapi.app;

/** Static DEMO responses for exercising the result UI without calling the API. */
final class MockApiResponses {
    static final int SUCCESS = 0;
    static final int EMPTY = 1;
    static final int ERROR = 2;

    private MockApiResponses() {}

    static String responseFor(int scenario) {
        if (scenario == EMPTY) {
            return "{\"success\":true,\"services\":{\"detect-labels\":{\"labels\":[]},\"detect-text\":{\"detections\":[]}}}";
        }
        return "{\"success\":true,\"services\":{\"detect-labels\":{\"labels\":["
                + "{\"name\":\"Bicycle\",\"confidence\":0.97,\"categories\":[\"Vehicle\",\"Transport\"]},"
                + "{\"name\":\"Person\",\"confidence\":0.84,\"categories\":[\"People\"]}]},"
                + "\"detect-text\":{\"detections\":[]}}}";
    }

    static String errorMessage() {
        return "MOCK/DEMO — Simulated request failure. No API request was sent.";
    }
}
