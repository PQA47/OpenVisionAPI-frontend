package com.openvisionapi.app;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.not;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI smoke tests that need no network and no photo picker: initial state and form reset.
 * Run with: ./gradlew connectedDebugAndroidTest (emulator or device required).
 */
@RunWith(AndroidJUnit4.class)
public class MainActivitySmokeTest {

    @Test
    public void landingPageShowsHeroContent() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.hero_badge)).check(matches(isDisplayed()));
            onView(withText(R.string.hero_note)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void submitIsDisabledUntilAnImageIsSelected() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText("Process image")).perform(scrollTo()).check(matches(not(isEnabled())));
            onView(withText("No image selected")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText(R.string.image_upload_hint)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void resetWithoutSelectionKeepsInitialState() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            onView(withText(R.string.reset_image)).perform(scrollTo(), click());

            onView(withText("Process image")).perform(scrollTo()).check(matches(not(isEnabled())));
            onView(withText("No image selected")).perform(scrollTo()).check(matches(isDisplayed()));
            onView(withText(R.string.image_placeholder)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
}
