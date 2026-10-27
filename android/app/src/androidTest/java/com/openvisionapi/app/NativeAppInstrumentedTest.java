package com.openvisionapi.app;

import static org.junit.Assert.*;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import android.app.Activity;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class NativeAppInstrumentedTest {
    @Test public void appUsesExpectedPackage() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.openvisionapi.app", appContext.getPackageName());
    }

    @Test public void landingAndUploadWorkflowCanLaunch() {
        android.content.Intent intent = new android.content.Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), MainActivity.class);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        try {
            assertNotNull(activity.findViewById(R.id.app_root));
            assertNotNull(activity.findViewById(R.id.app_header));
            assertNotNull(activity.findViewById(R.id.app_scroll));
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);
        }
    }
}
