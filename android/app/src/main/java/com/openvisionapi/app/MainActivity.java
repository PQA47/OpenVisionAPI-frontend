package com.openvisionapi.app;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;

public class MainActivity extends AppCompatActivity {
    public static final String EXTRA_HISTORY_ID = "com.openvisionapi.app.EXTRA_HISTORY_ID";
    private static final int MAX_IMAGE_BYTES = (int) ImageFileValidator.MAX_IMAGE_BYTES;
    private int COLOR_BACKGROUND, COLOR_SURFACE, COLOR_TEXT, COLOR_MUTED, COLOR_PURPLE, COLOR_BORDER;
    private int COLOR_SURFACE_VARIANT, COLOR_ACCENT_SURFACE, COLOR_PRIMARY, COLOR_ON_PRIMARY, COLOR_SUCCESS, COLOR_ERROR;

    private final ExecutorService imageWorker = Executors.newSingleThreadExecutor();
    private final ExecutorService historyWorker = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> imagePicker;
    private LinearLayout pageContent;
    private LinearLayout uploadCard;
    private ScrollView appScrollView;
    private ImageView imagePreview;
    private TextView imagePlaceholder;
    private TextView imageStatus;
    private TextView imageFileInfo;
    private Button submitButton;
    private LinearLayout resultContainer;
    private Button retryButton;
    private ProgressBar loadingIndicator;
    private DetectionOverlay detectionOverlay;
    private CheckBox mockModeToggle;
    private Spinner mockScenarioSpinner;
    private byte[] selectedImageBytes;
    private Uri selectedImageUri;
    private String selectedImageName;
    private String selectedImageMime;
    private boolean processing;
    private boolean viewingHistory;
    private long imageLoadGeneration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedMode(this);
        super.onCreate(savedInstanceState);
        loadThemeColors();
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(COLOR_BACKGROUND);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(!isDarkTheme());
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(!isDarkTheme());
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.app_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(0, systemBars.top, 0, systemBars.bottom);
            return windowInsets;
        });

        imagePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                this::loadSelectedImage
        );
        buildScreen();
        String historyId = getIntent().getStringExtra(EXTRA_HISTORY_ID);
        if (historyId != null) restoreHistoryEntry(historyId);
    }

    private void loadThemeColors() {
        COLOR_BACKGROUND = getColor(R.color.app_background); COLOR_SURFACE = getColor(R.color.surface);
        COLOR_TEXT = getColor(R.color.text_primary); COLOR_MUTED = getColor(R.color.text_secondary);
        COLOR_PURPLE = getColor(R.color.colorPrimary); COLOR_BORDER = getColor(R.color.border);
        COLOR_SURFACE_VARIANT = getColor(R.color.surface_variant); COLOR_ACCENT_SURFACE = getColor(R.color.accent_surface);
        COLOR_PRIMARY = getColor(R.color.colorPrimary); COLOR_ON_PRIMARY = getColor(R.color.on_primary);
        COLOR_SUCCESS = getColor(R.color.success); COLOR_ERROR = getColor(R.color.error);
    }

    private boolean isDarkTheme() {
        return (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void buildScreen() {
        LinearLayout header = findViewById(R.id.app_header);
        header.addView(makeBrandIcon());
        header.addView(makeBrandName(), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));
        Button appearanceButton = secondaryButton(ThemeManager.buttonLabel(this));
        appearanceButton.setContentDescription("Change application appearance");
        appearanceButton.setOnClickListener(view -> ThemeManager.showPicker(this));
        header.addView(appearanceButton);
        Button historyButton = secondaryButton("History");
        historyButton.setOnClickListener(view -> startActivity(new Intent(this, HistoryActivity.class)));
        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(-2, -2);
        historyParams.leftMargin = dp(6);
        header.addView(historyButton, historyParams);

        appScrollView = findViewById(R.id.app_scroll);
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(dp(22), dp(26), dp(22), dp(36));
        appScrollView.addView(pageContent);

        buildImageSection();
    }

    private void buildHeroSection() {
        TextView badge = text(getString(R.string.hero_badge), 11, COLOR_PURPLE, true);
        badge.setLetterSpacing(0.12f);
        badge.setPadding(dp(13), dp(9), dp(13), dp(9));
        badge.setBackground(rounded(COLOR_ACCENT_SURFACE, COLOR_BORDER, 6));
        pageContent.addView(badge, margin(0, 4, 0, 20));

        String headingText = getString(R.string.hero_title);
        SpannableString headingContent = new SpannableString(headingText);
        int secondLineStart = headingText.indexOf('\n') + 1;
        headingContent.setSpan(new ForegroundColorSpan(COLOR_PURPLE),
                secondLineStart, headingText.length(), 0);
        TextView heading = text(headingContent, 39, COLOR_TEXT, true);
        heading.setLineSpacing(dp(1), 1f);
        heading.setLetterSpacing(-0.035f);
        pageContent.addView(heading, margin(0, 0, 0, 14));

        TextView description = text(getString(R.string.hero_description), 15, COLOR_MUTED, false);
        description.setLineSpacing(dp(6), 1f);
        pageContent.addView(description, margin(0, 0, 0, 0));
    }

    private void buildFeaturesSection() {
        addSectionHeading(getString(R.string.features_kicker), getString(R.string.features_title),
                getString(R.string.features_description));
        addFeatureCard("⌕", getString(R.string.feature_recognition_title),
                getString(R.string.feature_recognition_description), "01");
        addFeatureCard("✳", getString(R.string.feature_analysis_title),
                getString(R.string.feature_analysis_description), "02");
        addFeatureCard("⌘", getString(R.string.feature_developer_title),
                getString(R.string.feature_developer_description), "03");
    }

    private void buildWorkflowSection() {
        addSectionHeading(getString(R.string.workflow_kicker), getString(R.string.workflow_title),
                getString(R.string.workflow_description));
        addWorkflowStep("01", getString(R.string.step_choose_title),
                getString(R.string.step_choose_description));
        addWorkflowStep("02", getString(R.string.step_analyze_title),
                getString(R.string.step_analyze_description));
        addWorkflowStep("03", getString(R.string.step_results_title),
                getString(R.string.step_results_description));
    }

    private void buildImageSection() {
        addSectionHeading(getString(R.string.workspace_kicker), getString(R.string.workspace_title),
                getString(R.string.workspace_description));

        uploadCard = new LinearLayout(this);
        uploadCard.setOrientation(LinearLayout.VERTICAL);
        uploadCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        uploadCard.setBackground(rounded(COLOR_SURFACE, COLOR_BORDER, 8));
        pageContent.addView(uploadCard, margin(0, 4, 0, 26));

        FrameLayout previewFrame = new FrameLayout(this);
        previewFrame.setMinimumHeight(dp(170));
        previewFrame.setBackground(rounded(COLOR_SURFACE_VARIANT, COLOR_BORDER, 6));
        imagePreview = new ImageView(this);
        imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagePreview.setAdjustViewBounds(true);
        imagePreview.setVisibility(View.GONE);
        detectionOverlay = new DetectionOverlay();
        detectionOverlay.setVisibility(View.GONE);
        previewFrame.addView(imagePreview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        previewFrame.addView(detectionOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        imagePlaceholder = text(getString(R.string.image_placeholder), 14, COLOR_MUTED, false);
        imagePlaceholder.setGravity(Gravity.CENTER);
        imagePlaceholder.setPadding(dp(24), dp(20), dp(24), dp(20));
        previewFrame.addView(imagePlaceholder, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        LinearLayout.LayoutParams previewParams = margin(0, 0, 0, 14);
        previewParams.height = dp(230);
        uploadCard.addView(previewFrame, previewParams);

        imageStatus = text(getString(R.string.image_upload_hint), 13, COLOR_MUTED, false);
        imageStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        uploadCard.addView(imageStatus, margin(0, 0, 0, 6));
        imageFileInfo = text("No image selected", 12, COLOR_MUTED, false);
        uploadCard.addView(imageFileInfo, margin(0, 0, 0, 14));

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        Button chooseButton = primaryButton(getString(R.string.choose_image));
        chooseButton.setOnClickListener(view -> imagePicker.launch("image/*"));
        buttons.addView(chooseButton, new LinearLayout.LayoutParams(
                0, dp(48), 1
        ));

        Button resetButton = secondaryButton(getString(R.string.reset_image));
        resetButton.setOnClickListener(view -> resetImage());
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                0, dp(48), 1
        );
        resetParams.leftMargin = dp(10);
        buttons.addView(resetButton, resetParams);
        uploadCard.addView(buttons);

        mockModeToggle = new CheckBox(this);
        mockModeToggle.setText("Use MOCK/DEMO mode (no real API request)");
        mockModeToggle.setTextColor(COLOR_TEXT);
        uploadCard.addView(mockModeToggle, margin(0, 12, 0, 0));
        mockScenarioSpinner = new Spinner(this);
        ArrayAdapter<String> scenarios = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Success — sample labels", "Empty — no results", "Error — simulated failure"}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                return scenarioText(getItem(position), false);
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                return scenarioText(getItem(position), true);
            }
        };
        scenarios.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mockScenarioSpinner.setAdapter(scenarios);
        mockScenarioSpinner.setVisibility(View.GONE);
        uploadCard.addView(mockScenarioSpinner, margin(0, 0, 0, 4));
        mockModeToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mockScenarioSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updateSubmitButton();
            if (isChecked) imageStatus.setText("MOCK/DEMO mode enabled. No API request will be sent.");
            else if (!processing) imageStatus.setText(R.string.image_ready);
        });

        submitButton = primaryButton("Process image");
        submitButton.setEnabled(false);
        submitButton.setAlpha(0.55f);
        submitButton.setOnClickListener(view -> submitSelectedImage());
        uploadCard.addView(submitButton, margin(0, 12, 0, 0));

        loadingIndicator = new ProgressBar(this);
        loadingIndicator.setVisibility(View.GONE);
        uploadCard.addView(loadingIndicator, margin(0, 12, 0, 0));
        resultContainer = new LinearLayout(this);
        resultContainer.setOrientation(LinearLayout.VERTICAL);
        resultContainer.setVisibility(View.GONE);
        uploadCard.addView(resultContainer, margin(0, 12, 0, 0));
        retryButton = secondaryButton("Try again");
        retryButton.setVisibility(View.GONE);
        retryButton.setOnClickListener(view -> submitSelectedImage());
        uploadCard.addView(retryButton, margin(0, 10, 0, 0));
    }

    private void addSectionHeading(String kicker, String title, String description) {
        TextView kickerView = text(kicker, 11, COLOR_PURPLE, true);
        kickerView.setLetterSpacing(0.12f);
        pageContent.addView(kickerView, margin(0, 28, 0, 9));

        TextView titleView = text(title, 26, COLOR_TEXT, true);
        titleView.setLetterSpacing(-0.02f);
        pageContent.addView(titleView, margin(0, 0, 0, 9));

        TextView descriptionView = text(description, 14, COLOR_MUTED, false);
        descriptionView.setLineSpacing(dp(5), 1f);
        pageContent.addView(descriptionView, margin(0, 0, 0, 14));
    }

    private void addFeatureCard(String icon, String title, String description, String number) {
        LinearLayout card = horizontalCard();

        TextView iconView = text(icon, 23, COLOR_PURPLE, true);
        iconView.setGravity(Gravity.CENTER);
        iconView.setBackground(rounded(COLOR_ACCENT_SURFACE, COLOR_BORDER, 6));
        card.addView(iconView, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = text(title, 16, COLOR_TEXT, true);
        TextView descriptionView = text(description, 13, COLOR_MUTED, false);
        descriptionView.setLineSpacing(dp(3), 1f);
        textColumn.addView(titleView, margin(0, 0, 0, 5));
        textColumn.addView(descriptionView);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        );
        textParams.leftMargin = dp(14);
        card.addView(textColumn, textParams);

        TextView numberView = text(number, 11, COLOR_MUTED, false);
        LinearLayout.LayoutParams numberParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        numberParams.leftMargin = dp(8);
        card.addView(numberView, numberParams);
        pageContent.addView(card, margin(0, 0, 0, 10));
    }

    private void addWorkflowStep(String number, String title, String description) {
        LinearLayout row = horizontalCard();
        TextView numberView = text(number, 12, COLOR_PURPLE, true);
        numberView.setGravity(Gravity.CENTER);
        numberView.setBackground(rounded(COLOR_ACCENT_SURFACE, COLOR_BORDER, 5));
        row.addView(numberView, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout textColumn = new LinearLayout(this);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.addView(text(title, 15, COLOR_TEXT, true), margin(0, 0, 0, 4));
        TextView descriptionView = text(description, 13, COLOR_MUTED, false);
        descriptionView.setLineSpacing(dp(3), 1f);
        textColumn.addView(descriptionView);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        );
        textParams.leftMargin = dp(14);
        row.addView(textColumn, textParams);
        pageContent.addView(row, margin(0, 0, 0, 10));
    }

    private LinearLayout horizontalCard() {
        LinearLayout card = new LinearLayout(this);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackground(rounded(COLOR_SURFACE, COLOR_BORDER, 6));
        return card;
    }

    private void loadSelectedImage(Uri uri) {
        if (uri == null) {
            return;
        }
        viewingHistory = false;

        ContentResolver resolver = getContentResolver();
        String mimeType = resolver.getType(uri);
        if (!ImageFileValidator.isSupportedMimeType(mimeType)) {
            imageLoadGeneration++;
            processing = false;
            clearResult();
            imagePreview.setImageDrawable(null);
            selectedImageBytes = null;
            selectedImageUri = null;
            imageFileInfo.setText("No valid image selected");
            updateSubmitButton();
            imageStatus.setText(R.string.image_invalid_type);
            imagePreview.setVisibility(View.GONE);
            imagePlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        long generation = ++imageLoadGeneration;
        clearResult();
        processing = false;
        updateSubmitButton();
        selectedImageUri = uri;
        selectedImageMime = mimeType;
        selectedImageBytes = null;
        selectedImageName = queryDisplayName(uri);
        imageFileInfo.setText(selectedImageName + " · checking file size…");
        updateSubmitButton();
        imageStatus.setText(R.string.image_loading);
        imagePlaceholder.setText(R.string.image_placeholder);
        imagePreview.setVisibility(View.GONE);
        imagePlaceholder.setVisibility(View.GONE);
        imageWorker.execute(() -> {
            try {
                byte[] imageBytes = readImageBytes(resolver, uri);
                Bitmap bitmap = decodeImage(imageBytes);
                runOnUiThread(() -> {
                    if (generation == imageLoadGeneration && !isFinishing()) {
                        selectedImageBytes = imageBytes;
                        imagePreview.setImageBitmap(bitmap);
                        detectionOverlay.setImageSize(bitmap.getWidth(), bitmap.getHeight());
                        imagePreview.setVisibility(View.VISIBLE);
                        imagePlaceholder.setVisibility(View.GONE);
                        imageFileInfo.setText(selectedImageName + " · " + ImageFileValidator.formatBytes(imageBytes.length));
                        imageStatus.setText("Image ready. You can submit it for processing.");
                        updateSubmitButton();
                    }
                });
            } catch (ImageTooLargeException exception) {
                showImageError(generation, R.string.image_too_large);
            } catch (IOException | SecurityException exception) {
                showImageError(generation, R.string.image_read_error);
            }
        });
    }

    private byte[] readImageBytes(ContentResolver resolver, Uri uri) throws IOException {
        try (InputStream stream = resolver.openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (stream == null) {
                throw new IOException("The selected image could not be opened.");
            }
            byte[] buffer = new byte[16 * 1024];
            int total = 0;
            int count;
            while ((count = stream.read(buffer)) != -1) {
                total += count;
                if (total > MAX_IMAGE_BYTES) {
                    throw new ImageTooLargeException();
                }
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private Bitmap decodeImage(byte[] bytes) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("The selected image could not be decoded.");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (bounds.outWidth / options.inSampleSize > 1200
                || bounds.outHeight / options.inSampleSize > 1600) {
            options.inSampleSize *= 2;
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (bitmap == null) {
            throw new IOException("The selected image could not be decoded.");
        }
        return bitmap;
    }

    private void showImageError(long generation, int messageResource) {
        runOnUiThread(() -> {
            if (generation == imageLoadGeneration && !isFinishing()) {
                clearResult();
                processing = false;
                imagePreview.setImageDrawable(null);
                imagePreview.setVisibility(View.GONE);
                imagePlaceholder.setVisibility(View.VISIBLE);
                imageStatus.setText(messageResource);
                selectedImageBytes = null;
                imageFileInfo.setText("No valid image selected");
                updateSubmitButton();
            }
        });
    }

    private void resetImage() {
        viewingHistory = false;
        imageLoadGeneration++;
        imagePreview.setImageDrawable(null);
        imagePreview.setVisibility(View.GONE);
        imagePlaceholder.setVisibility(View.VISIBLE);
        imageStatus.setText(R.string.image_upload_hint);
        imagePlaceholder.setText(R.string.image_placeholder);
        selectedImageBytes = null;
        selectedImageUri = null;
        selectedImageName = null;
        selectedImageMime = null;
        imageFileInfo.setText("No image selected");
        processing = false;
        clearResult();
        updateSubmitButton();
    }

    private String queryDisplayName(Uri uri) {
        try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) return cursor.getString(index);
            }
        } catch (Exception ignored) { }
        return "selected-image";
    }

    private void updateSubmitButton() {
        if (submitButton == null) return;
        boolean enabled = !viewingHistory && AnalysisFlow.canSubmit(mockModeToggle != null && mockModeToggle.isChecked(),
                selectedImageBytes != null && selectedImageBytes.length > 0, processing);
        submitButton.setEnabled(enabled);
        submitButton.setAlpha(enabled ? 1f : 0.55f);
        submitButton.setText(processing ? "Processing…" : "Process image");
    }

    private void submitSelectedImage() {
        boolean useMock = mockModeToggle.isChecked();
        if (!AnalysisFlow.canSubmit(useMock, selectedImageBytes != null && selectedImageBytes.length > 0, processing)) {
            imageStatus.setText("Choose a valid image before processing live analysis, or enable MOCK/DEMO mode.");
            return;
        }
        processing = true;
        final long requestGeneration = imageLoadGeneration;
        int mockScenario = mockScenarioSpinner.getSelectedItemPosition();
        clearResult();
        updateSubmitButton();
        imageStatus.setText(useMock ? "MOCK/DEMO running — no API request will be sent."
                : "Sending image to OpenVisionAPI…");
        loadingIndicator.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
        resultContainer.setVisibility(View.GONE);
        byte[] payload = selectedImageBytes;
        String filename = selectedImageName;
        String mime = selectedImageMime;
        imageWorker.execute(() -> {
            try {
                OpenVisionApiClient.ApiResult result = AnalysisFlow.execute(useMock, mockScenario,
                        () -> OpenVisionApiClient.processImage(BuildConfig.IMAGE_API_URL,
                                BuildConfig.PIXICULAR_API_KEY, payload, filename, mime));
                runOnUiThread(() -> {
                    if (requestGeneration != imageLoadGeneration || isFinishing()) return;
                    processing = false;
                    updateSubmitButton();
                    loadingIndicator.setVisibility(View.GONE);
                    if (!result.isSuccess()) {
                        String error = "The server could not process this image (HTTP " + result.statusCode + ").";
                        imageStatus.setText("API request failed (HTTP " + result.statusCode + "). Please try again.");
                        saveHistory(payload, filename, result.body, HistoryStore.STATUS_FAILED, useMock,
                                error, "Request failed (HTTP " + result.statusCode + ")", 0);
                        showErrorResult(error, useMock);
                        return;
                    }
                    try {
                        ApiResponseParser parsed = ApiResponseParser.parse(result.body);
                        imageStatus.setText(useMock ? "MOCK/DEMO complete — sample data, no API request sent."
                                : "Processing completed (HTTP " + result.statusCode + ").");
                        saveHistory(payload, filename, result.body, HistoryStore.STATUS_SUCCESS, useMock,
                                "", buildHistorySummary(parsed), parsed.labels.size());
                        showApiResult(parsed, useMock);
                    } catch (JSONException exception) {
                        String error = useMock ? "MOCK/DEMO fixture does not match the current parser."
                                : "The API returned an empty, failed, or unsupported response.";
                        imageStatus.setText(useMock ? "MOCK/DEMO sample response could not be parsed."
                                : "The API response could not be read. Please try again.");
                        saveHistory(payload, filename, result.body, HistoryStore.STATUS_FAILED, useMock,
                                error, "Response could not be parsed", 0);
                        showErrorResult(error, useMock);
                    }
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (requestGeneration != imageLoadGeneration || isFinishing()) return;
                    processing = false;
                    updateSubmitButton();
                    loadingIndicator.setVisibility(View.GONE);
                    String error = useMock ? MockApiResponses.errorMessage()
                            : "Request failed: " + safeErrorMessage(exception);
                    imageStatus.setText(useMock ? "MOCK/DEMO simulated request error. No API request was sent."
                            : "Could not process image. Check your connection and try again.");
                    saveHistory(payload, filename, "", HistoryStore.STATUS_FAILED, useMock,
                            error, useMock ? "Simulated demo failure" : "Network or request failure", 0);
                    showErrorResult(error, useMock);
                });
            }
        });
    }

    private String buildHistorySummary(ApiResponseParser parsed) {
        if (!parsed.labels.isEmpty()) {
            String summary = parsed.labels.get(0).name;
            if (parsed.labels.size() > 1) summary += " and " + (parsed.labels.size() - 1) + " more label(s)";
            return summary;
        }
        if (!parsed.textLines.isEmpty()) return "Text: " + parsed.textLines.get(0);
        return "No labels or text detected";
    }

    private void saveHistory(byte[] imageBytes, String imageName, String responseJson,
                             String status, boolean isMock, String error, String summary, int labelCount) {
        final byte[] imageCopy = imageBytes;
        historyWorker.execute(() -> {
            try {
                HistoryStore.save(getApplicationContext(), imageCopy, imageName, responseJson,
                        status, isMock, error, summary, labelCount);
            } catch (IOException ignored) {
                // The analysis result remains visible even if local history storage fails.
            }
        });
    }

    private void restoreHistoryEntry(String historyId) {
        HistoryStore.Entry entry = HistoryStore.get(this, historyId);
        if (entry == null) {
            imageStatus.setText("This saved history item is no longer available.");
            return;
        }
        viewingHistory = true;
        selectedImageBytes = null;
        selectedImageUri = null;
        selectedImageName = entry.imageName;
        selectedImageMime = null;
        processing = false;
        mockModeToggle.setChecked(entry.mock);
        imageFileInfo.setText(entry.imageName);
        imageStatus.setText("Viewing saved result · " + new java.text.SimpleDateFormat(
                "MMM d, yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(entry.timestamp)));
        android.graphics.Bitmap thumbnail = HistoryStore.loadThumbnail(entry);
        if (thumbnail != null) {
            imagePreview.setImageBitmap(thumbnail);
            imagePreview.setVisibility(View.VISIBLE);
            imagePlaceholder.setVisibility(View.GONE);
            detectionOverlay.setImageSize(thumbnail.getWidth(), thumbnail.getHeight());
        } else {
            imagePreview.setImageDrawable(null);
            imagePreview.setVisibility(View.GONE);
            imagePlaceholder.setVisibility(View.VISIBLE);
            imagePlaceholder.setText("Original image thumbnail is unavailable. The saved result is still accessible.");
        }
        updateSubmitButton();
        retryButton.setVisibility(View.GONE);
        if (HistoryStore.STATUS_SUCCESS.equals(entry.status)) {
            try {
                ApiResponseParser parsed = ApiResponseParser.parse(entry.responseJson);
                showApiResult(parsed, entry.mock);
            } catch (JSONException exception) {
                showErrorResult("The saved response is incomplete or corrupted. " + entry.summary, entry.mock);
            }
        } else {
            showErrorResult(entry.errorMessage.isEmpty() ? "This analysis did not complete successfully." : entry.errorMessage,
                    entry.mock);
        }
    }

    private void showApiResult(ApiResponseParser parsed, boolean isMock) {
        resultContainer.removeAllViews();
        if (isMock) addResultHeading("MOCK / DEMO — SAMPLE DATA, NOT A REAL ANALYSIS");
        addResultHeading(parsed.hasUsefulResults ? "Analysis results" : "No results");
        if (!parsed.hasUsefulResults) {
            addResultText("The API completed successfully but returned no usable labels or extracted text.");
        }
        if (!parsed.labels.isEmpty()) {
            addResultHeading("Detected labels");
            for (ApiResponseParser.Label label : parsed.labels) {
                String detail = label.name + (label.confidence.isEmpty() ? "" : "\n" + label.confidence)
                        + (label.categories.isEmpty() ? "" : "\nCategory: " + label.categories);
                addResultText(detail);
            }
        }
        if (!parsed.textLines.isEmpty()) {
            addResultHeading("Extracted text");
            for (String line : parsed.textLines) addResultText(line);
        }
        StringBuilder metadata = new StringBuilder();
        if (!parsed.requestId.isEmpty()) metadata.append("Request ID: ").append(parsed.requestId);
        if (!parsed.processingTime.isEmpty()) appendMetadata(metadata, "Processing time: " + parsed.processingTime);
        if (!parsed.credits.isEmpty()) appendMetadata(metadata, "Credits used: " + parsed.credits);
        if (metadata.length() > 0) { addResultHeading("Request details"); addResultText(metadata.toString()); }
        detectionOverlay.setLabels(parsed.labels);
        detectionOverlay.setVisibility(hasBoxes(parsed.labels) ? View.VISIBLE : View.GONE);
        resultContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(View.GONE);
    }

    private boolean hasBoxes(java.util.List<ApiResponseParser.Label> labels) {
        for (ApiResponseParser.Label label : labels) if (label.hasBox) return true;
        return false;
    }
    private void appendMetadata(StringBuilder value, String item) { if (value.length() > 0) value.append('\n'); value.append(item); }
    private void addResultHeading(String value) {
        boolean errorHeading = value.toLowerCase(java.util.Locale.ROOT).contains("could not")
                || value.toLowerCase(java.util.Locale.ROOT).contains("error");
        TextView heading = text(value, 16, errorHeading ? COLOR_ERROR : COLOR_PURPLE, true);
        resultContainer.addView(heading, margin(0, 8, 0, 8));
    }
    private void addResultText(String value) {
        TextView body = text(value, 14, COLOR_TEXT, false);
        body.setLineSpacing(dp(4), 1f);
        body.setPadding(dp(12), dp(11), dp(12), dp(11));
        body.setBackground(rounded(COLOR_SURFACE_VARIANT, COLOR_BORDER, 5));
        resultContainer.addView(body, margin(0, 0, 0, 8));
    }
    private void showErrorResult(String message, boolean isMock) {
        resultContainer.removeAllViews();
        addResultHeading(isMock ? "MOCK / DEMO — SIMULATED ERROR" : "Could not display results");
        addResultText(message);
        resultContainer.setVisibility(View.VISIBLE);
        retryButton.setVisibility(!viewingHistory && AnalysisFlow.canSubmit(isMock,
                selectedImageBytes != null && selectedImageBytes.length > 0, processing)
                ? View.VISIBLE : View.GONE);
        detectionOverlay.setVisibility(View.GONE);
    }
    private String safeErrorMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.trim().isEmpty()) return "network or response error";
        return message.length() > 180 ? message.substring(0, 180) : message;
    }
    private void clearResult() {
        if (resultContainer != null) { resultContainer.removeAllViews(); resultContainer.setVisibility(View.GONE); }
        if (retryButton != null) retryButton.setVisibility(View.GONE);
        if (loadingIndicator != null) loadingIndicator.setVisibility(View.GONE);
        if (detectionOverlay != null) detectionOverlay.setVisibility(View.GONE);
    }

    private View makeBrandIcon() {
        TextView icon = text("◈", 24, COLOR_PURPLE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(COLOR_ACCENT_SURFACE, COLOR_BORDER, 5));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(38), dp(38));
        params.rightMargin = dp(10);
        icon.setLayoutParams(params);
        return icon;
    }

    private TextView makeBrandName() {
        TextView brand = text(getString(R.string.app_brand), 18, COLOR_TEXT, true);
        brand.setLetterSpacing(-0.02f);
        return brand;
    }

    private Button primaryButton(String label) {
        Button button = button(label, COLOR_ON_PRIMARY, COLOR_PRIMARY);
        button.setBackground(rounded(COLOR_PRIMARY, Color.TRANSPARENT, 6));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = button(label, COLOR_TEXT, COLOR_SURFACE);
        button.setBackground(rounded(COLOR_SURFACE, COLOR_BORDER, 6));
        return button;
    }

    private Button button(String label, int textColor, int backgroundColor) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(textColor);
        button.setTextSize(14);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setMinHeight(dp(48));
        button.setPadding(dp(12), 0, dp(12), 0);
        button.setBackground(rounded(backgroundColor, COLOR_BORDER, 6));
        return button;
    }

    private TextView text(CharSequence value, int size, int color, boolean medium) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setTypeface(Typeface.create(
                medium ? "sans-serif-medium" : "sans-serif",
                Typeface.NORMAL
        ));
        text.setIncludeFontPadding(false);
        return text;
    }

    private TextView scenarioText(String value, boolean dropdown) {
        TextView item = text(value, 14, COLOR_TEXT, false);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(12), dp(10), dp(12), dp(10));
        item.setBackgroundColor(dropdown ? COLOR_SURFACE : COLOR_SURFACE_VARIANT);
        return item;
    }

    private GradientDrawable rounded(int fillColor, int strokeColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dp(radius));
        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }
        return drawable;
    }

    private LinearLayout.LayoutParams margin(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        imageLoadGeneration++;
        imageWorker.shutdownNow();
        historyWorker.shutdown();
        super.onDestroy();
    }

    private static final class ImageTooLargeException extends IOException {}

    private final class DetectionOverlay extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private java.util.List<ApiResponseParser.Label> labels = java.util.Collections.emptyList();
        private int imageWidth, imageHeight;
        DetectionOverlay() { super(MainActivity.this); paint.setColor(COLOR_PURPLE); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); }
        void setImageSize(int width, int height) { imageWidth = width; imageHeight = height; invalidate(); }
        void setLabels(java.util.List<ApiResponseParser.Label> value) { labels = value; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            if (imageWidth <= 0 || imageHeight <= 0) return;
            float scale = Math.min(getWidth() / (float) imageWidth, getHeight() / (float) imageHeight);
            float left = (getWidth() - imageWidth * scale) / 2f;
            float top = (getHeight() - imageHeight * scale) / 2f;
            for (ApiResponseParser.Label label : labels) if (label.hasBox) {
                float x1 = left + label.x * imageWidth * scale;
                float y1 = top + label.y * imageHeight * scale;
                float x2 = left + (label.x + label.width) * imageWidth * scale;
                float y2 = top + (label.y + label.height) * imageHeight * scale;
                canvas.drawRect(x1, y1, x2, y2, paint);
            }
        }
    }
}
