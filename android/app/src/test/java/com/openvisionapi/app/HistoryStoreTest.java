package com.openvisionapi.app;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HistoryStoreTest {
    @Test public void entryRoundTripsPersistentFields() throws Exception {
        HistoryStore.Entry original = new HistoryStore.Entry(
                "record-1", 123456L, "sample.png", "/private/thumb.jpg",
                "{\"success\":true}", HistoryStore.STATUS_SUCCESS, true,
                "", "Bicycle and 1 more label(s)", 2);

        JSONObject saved = original.toJson();
        HistoryStore.Entry restored = HistoryStore.Entry.fromJson(saved);

        assertEquals(original.id, restored.id);
        assertEquals(original.timestamp, restored.timestamp);
        assertEquals(original.imageName, restored.imageName);
        assertEquals(original.thumbnailPath, restored.thumbnailPath);
        assertEquals(original.responseJson, restored.responseJson);
        assertEquals(original.status, restored.status);
        assertTrue(restored.mock);
        assertEquals(original.summary, restored.summary);
        assertEquals(2, restored.labelCount);
        assertFalse(restored.errorMessage.length() > 0);
    }

    @Test public void incompleteRecordUsesSafeDefaults() throws Exception {
        HistoryStore.Entry entry = HistoryStore.Entry.fromJson(new JSONObject("{}"));
        assertEquals("Image", entry.imageName);
        assertEquals(HistoryStore.STATUS_FAILED, entry.status);
        assertEquals(0, entry.labelCount);
    }
}
