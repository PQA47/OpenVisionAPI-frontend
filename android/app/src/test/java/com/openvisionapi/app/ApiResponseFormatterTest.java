package com.openvisionapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApiResponseFormatterTest {

    private static final String FULL_RESPONSE = "{"
            + "\"success\":true,\"requestId\":\"req-123\","
            + "\"services\":{"
            + "  \"detect-labels\":{\"labels\":["
            + "    {\"name\":\"Cat\",\"confidence\":98.5,\"categories\":[\"Animal\",\"Pet\"]},"
            + "    {\"name\":\"Sofa\",\"confidence\":71.234}"
            + "  ]},"
            + "  \"detect-text\":{\"detections\":["
            + "    {\"detectedText\":\"HELLO\",\"type\":\"LINE\",\"confidence\":99.1}"
            + "  ]}"
            + "},"
            + "\"photo\":{\"status\":\"processed\"},"
            + "\"processingTimeMs\":123,\"creditCost\":2}";

    // ---- happy path ----------------------------------------------------------------------------

    @Test
    public void formatsFullSuccessResponse() {
        String expected = "✓ ANALYSIS COMPLETE\n"
                + "Request ID: req-123\n"
                + "\n"
                + "DETECTED OBJECTS\n"
                + "• Cat — 98.50%\n"
                + "  Category: Animal, Pet\n"
                + "• Sofa — 71.23%\n"
                + "\n"
                + "EXTRACTED TEXT (OCR)\n"
                + "• HELLO (LINE · 99.10% confidence)\n"
                + "\n"
                + "Photo status: processed\n"
                + "Processing time: 123 ms\n"
                + "Credits used: 2";
        assertEquals(expected, ApiResponseFormatter.format(FULL_RESPONSE));
    }

    @Test
    public void noLabelsAndNoTextShowFriendlyPlaceholders() {
        String out = ApiResponseFormatter.format(
                "{\"success\":true,\"services\":{\"detect-labels\":{\"labels\":[]},\"detect-text\":{\"detections\":[]}}}");

        assertTrue(out.contains("No objects detected."));
        assertTrue(out.contains("No text detected."));
    }

    @Test
    public void missingServiceSectionsAreTreatedAsEmpty() {
        String out = ApiResponseFormatter.format("{\"success\":true,\"services\":{}}");

        assertTrue(out.startsWith("✓ ANALYSIS COMPLETE"));
        assertTrue(out.contains("No objects detected."));
        assertTrue(out.contains("No text detected."));
    }

    @Test
    public void blankOcrDetectionsAreReportedAsNoText() {
        String out = ApiResponseFormatter.format(
                "{\"success\":true,\"services\":{\"detect-text\":{\"detections\":[{\"detectedText\":\"   \"},{}]}}}");

        assertTrue(out, out.contains("No text detected."));
        assertFalse(out, out.contains("• \n"));
    }

    @Test
    public void textDetectionWithOnlyConfidenceOrOnlyType() {
        String out = ApiResponseFormatter.format("{\"success\":true,\"services\":{\"detect-text\":{\"detections\":["
                + "{\"detectedText\":\"A\",\"confidence\":50},"
                + "{\"detectedText\":\"B\",\"type\":\"WORD\"},"
                + "{\"detectedText\":\"C\"}]}}}");

        assertTrue(out, out.contains("• A (50.00% confidence)"));
        assertTrue(out, out.contains("• B (WORD)"));
        assertTrue(out, out.contains("• C\n") || out.endsWith("• C"));
    }

    @Test
    public void labelWithoutNameOrConfidenceIsStillListed() {
        String out = ApiResponseFormatter.format(
                "{\"success\":true,\"services\":{\"detect-labels\":{\"labels\":[{}]}}}");

        assertTrue(out, out.contains("• Unknown object"));
        assertFalse(out, out.contains("%"));
    }

    @Test
    public void jsonNullValuesAreNotPrintedAsTheWordNull() {
        String out = ApiResponseFormatter.format("{\"success\":true,\"requestId\":null,\"services\":{"
                + "\"detect-labels\":{\"labels\":[{\"name\":null,\"categories\":[null,\"Animal\"]}]}}}");

        assertFalse(out, out.contains("null"));
        assertFalse(out, out.contains("Request ID"));
        assertTrue(out, out.contains("• Unknown object"));
        assertTrue(out, out.contains("Category: Animal"));
    }

    @Test
    public void nonObjectLabelEntriesAreSkipped() {
        String out = ApiResponseFormatter.format(
                "{\"success\":true,\"services\":{\"detect-labels\":{\"labels\":[1,\"x\",null,{\"name\":\"Dog\"}]}}}");

        assertTrue(out, out.contains("• Dog"));
    }

    @Test
    public void unicodeTextIsPreserved() {
        String out = ApiResponseFormatter.format(
                "{\"success\":true,\"services\":{\"detect-text\":{\"detections\":[{\"detectedText\":\"Xin chào 你好\"}]}}}");

        assertTrue(out, out.contains("Xin chào 你好"));
    }

    // ---- failure / unexpected shapes -----------------------------------------------------------

    @Test
    public void failureResponseWithMessageShowsTheMessage() {
        String out = ApiResponseFormatter.format("{\"success\":false,\"message\":\"Invalid API key\"}");

        assertEquals("⚠ ANALYSIS FAILED\n\nInvalid API key", out);
    }

    @Test
    public void emptyObjectShowsDefaultMessage() {
        assertEquals("⚠ ANALYSIS FAILED\n\nNo service results were returned.", ApiResponseFormatter.format("{}"));
    }

    @Test
    public void emptyOrNullBodyShowsNoResults() {
        assertTrue(ApiResponseFormatter.format(null).startsWith("No results"));
        assertTrue(ApiResponseFormatter.format("").startsWith("No results"));
        assertTrue(ApiResponseFormatter.format("  \n ").startsWith("No results"));
    }

    @Test
    public void malformedJsonIsShownAsRawResponse() {
        String out = ApiResponseFormatter.format("{\"success\":tru");

        assertEquals("API RESPONSE\n\n{\"success\":tru", out);
    }

    @Test
    public void plainTextAndJsonArraysAreShownAsRawResponse() {
        assertEquals("API RESPONSE\n\nInternal Server Error", ApiResponseFormatter.format("Internal Server Error"));
        assertEquals("API RESPONSE\n\n[1,2,3]", ApiResponseFormatter.format("[1,2,3]"));
    }

    @Test
    public void veryLongRawBodiesAreTruncated() {
        StringBuilder html = new StringBuilder("<html>");
        for (int i = 0; i < 5000; i++) html.append('x');
        String out = ApiResponseFormatter.format(html.toString());

        assertTrue(out.length() < 1600);
        assertTrue(out.endsWith("…"));
    }

    @Test
    public void wrongTypedFieldsDoNotCrash() {
        // services is a string, labels is an object, confidence is text, creditCost is text.
        assertTrue(ApiResponseFormatter.format("{\"success\":true,\"services\":\"oops\"}")
                .contains("No service results were returned."));
        assertTrue(ApiResponseFormatter.format(
                "{\"success\":true,\"services\":{\"detect-labels\":{\"labels\":{}}}}").contains("No objects detected."));
        String out = ApiResponseFormatter.format(
                "{\"success\":true,\"services\":{\"detect-labels\":{\"labels\":[{\"name\":\"A\",\"confidence\":\"high\"}]}},"
                        + "\"creditCost\":\"many\"}");
        assertTrue(out, out.contains("• A"));
    }

    // ---- isAnalysisSuccessful ------------------------------------------------------------------

    @Test
    public void successFlagIsReadFromJsonObjectOnly() {
        assertTrue(ApiResponseFormatter.isAnalysisSuccessful("{\"success\":true}"));
        assertFalse(ApiResponseFormatter.isAnalysisSuccessful("{\"success\":false}"));
        assertFalse(ApiResponseFormatter.isAnalysisSuccessful("{}"));
        assertFalse(ApiResponseFormatter.isAnalysisSuccessful("[true]"));
        assertFalse(ApiResponseFormatter.isAnalysisSuccessful("success"));
        assertFalse(ApiResponseFormatter.isAnalysisSuccessful("{\"success\":tru"));
        assertFalse(ApiResponseFormatter.isAnalysisSuccessful(""));
        assertFalse(ApiResponseFormatter.isAnalysisSuccessful(null));
    }
}
