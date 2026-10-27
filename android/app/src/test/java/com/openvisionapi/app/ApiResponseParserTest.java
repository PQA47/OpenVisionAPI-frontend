package com.openvisionapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.json.JSONException;
import org.junit.Test;

/**
 * Parser contract tests using locally authored fixtures shaped for the current parser.
 * These fixtures are not an official Pixicular schema or provider-verified responses.
 */
public class ApiResponseParserTest {

    @Test
    public void parsesSuccessfulLabelsAndOcr() throws JSONException {
        ApiResponseParser parsed = ApiResponseParser.parse(response(
                "\"success\":true,"
                        + "\"requestId\":\"req-123\",\"processingTimeMs\":42,\"creditCost\":1,"
                        + "\"services\":{" + labels(label("cat", 0.95, "[\"animal\",\"pet\"]", box(0.1, 0.2, 0.3, 0.4)))
                        + ",\"detect-text\":{\"detections\":[{\"detectedText\":\"Hello\",\"type\":\"LINE\",\"confidence\":99.1}]}}"));

        assertEquals(1, parsed.labels.size());
        ApiResponseParser.Label label = parsed.labels.get(0);
        assertEquals("cat", label.name);
        assertEquals("95.0% confidence", label.confidence);
        assertEquals("animal, pet", label.categories);
        assertTrue(label.hasBox);
        assertEquals(0.1f, label.x, 0.0001f);
        assertEquals(0.2f, label.y, 0.0001f);
        assertEquals(0.3f, label.width, 0.0001f);
        assertEquals(0.4f, label.height, 0.0001f);
        assertEquals(1, parsed.textLines.size());
        assertEquals("Hello · LINE · 99.1% confidence", parsed.textLines.get(0));
        assertEquals("req-123", parsed.requestId);
        assertEquals("42 ms", parsed.processingTime);
        assertEquals("1", parsed.credits);
        assertTrue(parsed.hasUsefulResults);
    }

    @Test
    public void missingOptionalFieldsUseEmptyDefaults() throws JSONException {
        ApiResponseParser parsed = ApiResponseParser.parse(response(
                "\"success\":true,\"services\":{" + labels(label("bird", null, null, null))
                        + ",\"detect-text\":{\"detections\":[{\"detectedText\":\"Bird\"}]}}"));

        assertEquals("", parsed.requestId);
        assertEquals("", parsed.processingTime);
        assertEquals("", parsed.credits);
        assertEquals("", parsed.labels.get(0).confidence);
        assertEquals("", parsed.labels.get(0).categories);
        assertFalse(parsed.labels.get(0).hasBox);
        assertEquals("Bird", parsed.textLines.get(0));
    }

    @Test
    public void nullAndWrongTypesAreIgnoredSafely() throws JSONException {
        ApiResponseParser parsed = ApiResponseParser.parse(
                "{\"success\":true,\"requestId\":null,\"processingTimeMs\":\"fast\",\"creditCost\":{},"
                        + "\"services\":{\"detect-labels\":{\"labels\":[null,7,{\"name\":null,\"confidence\":\"high\",\"categories\":{},\"boundingBox\":[]}]},"
                        + "\"detect-text\":{\"detections\":[null,false,{\"detectedText\":null,\"confidence\":[]}]}}}");

        assertTrue(parsed.labels.isEmpty());
        assertTrue(parsed.textLines.isEmpty());
        assertEquals("", parsed.requestId);
        assertEquals("", parsed.processingTime);
        assertEquals("", parsed.credits);
        assertFalse(parsed.hasUsefulResults);
    }

    @Test
    public void emptyArraysProduceNoResults() throws JSONException {
        ApiResponseParser parsed = ApiResponseParser.parse(
                "{\"success\":true,\"services\":{\"detect-labels\":{\"labels\":[]},\"detect-text\":{\"detections\":[]}}}");
        assertTrue(parsed.labels.isEmpty());
        assertTrue(parsed.textLines.isEmpty());
        assertFalse(parsed.hasUsefulResults);
    }

    @Test
    public void emptyOrMalformedJsonIsRejected() {
        assertThrows(JSONException.class, () -> ApiResponseParser.parse(""));
        assertThrows(JSONException.class, () -> ApiResponseParser.parse("   "));
        assertThrows(JSONException.class, () -> ApiResponseParser.parse("{}"));
        assertThrows(JSONException.class, () -> ApiResponseParser.parse("{not-json}"));
        assertThrows(JSONException.class, () -> ApiResponseParser.parse(null));
    }

    @Test
    public void successFalseIsRejectedWithMessage() {
        JSONException error = assertThrows(JSONException.class,
                () -> ApiResponseParser.parse("{\"success\":false,\"message\":\"No image found\"}"));
        assertEquals("No image found", error.getMessage());
    }

    @Test
    public void structurallyDifferentErrorResponseIsRejected() {
        assertThrows(JSONException.class, () -> ApiResponseParser.parse(
                "{\"error\":{\"code\":\"RATE_LIMIT\",\"message\":\"Try later\"}}"));
    }

    @Test
    public void confidenceSupportsZeroOneAndOneHundredBoundaries() throws JSONException {
        ApiResponseParser parsed = ApiResponseParser.parse(response(
                "\"success\":true,\"services\":{\"detect-labels\":{\"labels\":["
                        + label("zero", 0, null, null) + ","
                        + label("fraction-boundary", 1, null, null) + ","
                        + label("percent-boundary", 100, null, null) + "]}}"));
        assertEquals("0.0% confidence", parsed.labels.get(0).confidence);
        assertEquals("100.0% confidence", parsed.labels.get(1).confidence);
        assertEquals("100.0% confidence", parsed.labels.get(2).confidence);
    }

    @Test
    public void confidenceOutsideSupportedRangeAndWrongTypeAreOmitted() throws JSONException {
        ApiResponseParser parsed = ApiResponseParser.parse(response(
                "\"success\":true,\"services\":{\"detect-labels\":{\"labels\":["
                        + label("negative", -0.1, null, null) + ","
                        + label("too-large", 100.1, null, null) + ","
                        + "{\"name\":\"text\",\"confidence\":\"0.8\"}]}}"));
        assertEquals("", parsed.labels.get(0).confidence);
        assertEquals("", parsed.labels.get(1).confidence);
        assertEquals("", parsed.labels.get(2).confidence);
    }

    @Test
    public void validBoundingBoxIsAcceptedAtImageBoundary() throws JSONException {
        ApiResponseParser parsed = parseSingleBox(box(0, 0, 1, 1));
        assertTrue(parsed.labels.get(0).hasBox);
    }

    @Test
    public void missingIncompleteWrongTypeAndOutOfRangeBoundingBoxesAreRejected() throws JSONException {
        ApiResponseParser.Label missing = parseSingleBox(null).labels.get(0);
        ApiResponseParser.Label incomplete = parseSingleBox("{\"x\":0,\"y\":0,\"width\":0.5}").labels.get(0);
        ApiResponseParser.Label wrongType = parseSingleBox("{\"x\":\"0\",\"y\":0,\"width\":0.5,\"height\":0.5}").labels.get(0);
        ApiResponseParser.Label negative = parseSingleBox(box(-0.1, 0, 0.2, 0.2)).labels.get(0);
        ApiResponseParser.Label exceedsRight = parseSingleBox(box(0.8, 0, 0.3, 0.2)).labels.get(0);
        ApiResponseParser.Label exceedsBottom = parseSingleBox(box(0, 0.8, 0.2, 0.3)).labels.get(0);
        ApiResponseParser.Label zeroArea = parseSingleBox(box(0.1, 0.1, 0, 0.2)).labels.get(0);

        assertFalse(missing.hasBox);
        assertFalse(incomplete.hasBox);
        assertFalse(wrongType.hasBox);
        assertFalse(negative.hasBox);
        assertFalse(exceedsRight.hasBox);
        assertFalse(exceedsBottom.hasBox);
        assertFalse(zeroArea.hasBox);
    }

    private static ApiResponseParser parseSingleBox(String box) throws JSONException {
        return ApiResponseParser.parse(response("\"success\":true,\"services\":{" + labels(label("object", null, null, box)) + "}"));
    }

    private static String response(String members) {
        return "{" + members + "}";
    }

    private static String labels(String label) {
        return "\"detect-labels\":{\"labels\":[" + label + "]}";
    }

    private static String label(String name, Number confidence, String categories, String boundingBox) {
        StringBuilder json = new StringBuilder("{\"name\":\"").append(name).append("\"");
        if (confidence != null) json.append(",\"confidence\":").append(confidence);
        if (categories != null) json.append(",\"categories\":").append(categories);
        if (boundingBox != null) json.append(",\"boundingBox\":").append(boundingBox);
        return json.append('}').toString();
    }

    private static String box(double x, double y, double width, double height) {
        return "{\"x\":" + x + ",\"y\":" + y + ",\"width\":" + width + ",\"height\":" + height + "}";
    }
}
