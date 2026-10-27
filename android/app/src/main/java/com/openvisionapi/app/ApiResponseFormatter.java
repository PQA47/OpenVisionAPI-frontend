package com.openvisionapi.app;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Converts the Pixicular JSON response into readable object-detection and OCR results. */
public final class ApiResponseFormatter {
    /** Raw non-JSON bodies (e.g. an HTML 502 page) are shown truncated to this many characters. */
    static final int MAX_RAW_CHARS = 1500;

    private ApiResponseFormatter() {}

    // Android's org.json turns a JSON null into the text "null" for optString(); the reference
    // library returns the fallback. isNull() behaves the same in both, so check it first.
    private static String str(JSONObject object, String key, String fallback) {
        return object.isNull(key) ? fallback : object.optString(key, fallback);
    }

    /**
     * True only when the body is a JSON object whose {@code success} field is {@code true}.
     * Used so that "HTTP 200 + success:false" is not reported to the user as a success.
     */
    public static boolean isAnalysisSuccessful(String response) {
        if (response == null || response.trim().isEmpty()) return false;
        try {
            return new JSONObject(response).optBoolean("success", false);
        } catch (JSONException exception) {
            return false;
        }
    }

    public static String format(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "No results\n\nThe API returned an empty response.";
        }

        try {
            JSONObject root = new JSONObject(response);
            boolean success = root.optBoolean("success", false);
            StringBuilder output = new StringBuilder();
            output.append(success ? "✓ ANALYSIS COMPLETE" : "⚠ ANALYSIS FAILED");

            String requestId = str(root, "requestId", "");
            if (!requestId.isEmpty()) {
                output.append("\nRequest ID: ").append(requestId);
            }

            JSONObject services = root.optJSONObject("services");
            if (services == null) {
                String message = str(root, "message", "No service results were returned.");
                output.append("\n\n").append(message);
                return output.toString();
            }

            output.append("\n\nDETECTED OBJECTS");
            JSONObject labelService = services.optJSONObject("detect-labels");
            JSONArray labels = labelService == null ? null : labelService.optJSONArray("labels");
            if (labels == null || labels.length() == 0) {
                output.append("\nNo objects detected.");
            } else {
                for (int i = 0; i < labels.length(); i++) {
                    JSONObject label = labels.optJSONObject(i);
                    if (label == null) continue;

                    String name = str(label, "name", "Unknown object");
                    double confidence = label.optDouble("confidence", -1);
                    output.append("\n• ").append(name);
                    if (confidence >= 0) {
                        output.append(" — ").append(String.format(Locale.US, "%.2f%%", confidence));
                    }

                    JSONArray categories = label.optJSONArray("categories");
                    if (categories != null && categories.length() > 0) {
                        output.append("\n  Category: ");
                        boolean firstCategory = true;
                        for (int j = 0; j < categories.length(); j++) {
                            if (categories.isNull(j)) continue;
                            if (!firstCategory) output.append(", ");
                            output.append(categories.optString(j));
                            firstCategory = false;
                        }
                    }
                }
            }

            output.append("\n\nEXTRACTED TEXT (OCR)");
            JSONObject textService = services.optJSONObject("detect-text");
            JSONArray detections = textService == null ? null : textService.optJSONArray("detections");
            boolean anyText = false;
            if (detections != null) {
                for (int i = 0; i < detections.length(); i++) {
                    JSONObject detection = detections.optJSONObject(i);
                    if (detection == null) continue;

                    String detectedText = str(detection, "detectedText", "").trim();
                    if (detectedText.isEmpty()) continue;
                    anyText = true;
                    output.append("\n• ").append(detectedText);

                    String type = str(detection, "type", "");
                    double confidence = detection.optDouble("confidence", -1);
                    if (!type.isEmpty() || confidence >= 0) {
                        output.append(" (");
                        if (!type.isEmpty()) {
                            output.append(type);
                            if (confidence >= 0) output.append(" · ");
                        }
                        if (confidence >= 0) {
                            output.append(String.format(Locale.US, "%.2f%% confidence", confidence));
                        }
                        output.append(")");
                    }
                }
            }
            // Also covers the case where every detection was blank (previously printed nothing).
            if (!anyText) {
                output.append("\nNo text detected.");
            }

            JSONObject photo = root.optJSONObject("photo");
            if (photo != null) {
                String status = str(photo, "status", "");
                if (!status.isEmpty()) {
                    output.append("\n\nPhoto status: ").append(status);
                }
            }

            if (root.has("processingTimeMs")) {
                output.append("\nProcessing time: ").append(root.optLong("processingTimeMs")).append(" ms");
            }
            if (root.has("creditCost")) {
                output.append("\nCredits used: ").append(root.optInt("creditCost"));
            }

            return output.toString();
        } catch (JSONException exception) {
            // If the server returns plain text or an unexpected JSON shape, still show its response.
            String raw = response.length() > MAX_RAW_CHARS
                    ? response.substring(0, MAX_RAW_CHARS) + "…" : response;
            return "API RESPONSE\n\n" + raw;
        }
    }
}
