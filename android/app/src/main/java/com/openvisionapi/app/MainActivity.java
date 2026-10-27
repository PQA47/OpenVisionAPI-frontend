package com.openvisionapi.app;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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

public class MainActivity extends AppCompatActivity {
    private static final int MAX_IMAGE_BYTES = (int) ImageFileValidator.MAX_IMAGE_BYTES;
    private static final int COLOR_BACKGROUND = Color.rgb(8, 11, 23);
    private static final int COLOR_SURFACE = Color.rgb(16, 21, 41);
    private static final int COLOR_TEXT = Color.rgb(244, 245, 255);
    private static final int COLOR_MUTED = Color.rgb(161, 169, 197);
    private static final int COLOR_PURPLE = Color.rgb(177, 154, 255);
    private static final int COLOR_BORDER = Color.rgb(54, 59, 89);

    // Separate workers so a slow upload never blocks loading/previewing another image.
    private final ExecutorService imageWorker = Executors.newSingleThreadExecutor();
    private final ExecutorService uploadWorker = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> imagePicker;
    private LinearLayout pageContent;
    private LinearLayout uploadCard;
    private ScrollView appScrollView;
    private ImageView imagePreview;
    private TextView imagePlaceholder;
    private TextView imageStatus;
    private TextView imageFileInfo;
    private Button submitButton;
    private byte[] selectedImageBytes;
    private Uri selectedImageUri;
    private String selectedImageName;
    private String selectedImageMime;
    private boolean processing;
    private long imageLoadGeneration;
    // Incremented whenever the selection changes or a new request starts, so late responses are ignored.
    private long requestGeneration;
    private boolean lastRequestFailed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(COLOR_BACKGROUND);
        getWindow().setNavigationBarColor(COLOR_BACKGROUND);
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
    }

    private void buildScreen() {
        LinearLayout header = findViewById(R.id.app_header);
        header.addView(makeBrandIcon());
        header.addView(makeBrandName(), new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        ));
        header.addView(makeHeaderButton());

        appScrollView = findViewById(R.id.app_scroll);
        pageContent = new LinearLayout(this);
        pageContent.setOrientation(LinearLayout.VERTICAL);
        pageContent.setPadding(dp(22), dp(26), dp(22), dp(36));
        appScrollView.addView(pageContent);

        buildHeroSection();
        buildFeaturesSection();
        buildWorkflowSection();
        buildImageSection();
        buildClosingSection();
    }

    private void buildHeroSection() {
        TextView badge = text(getString(R.string.hero_badge), 11, COLOR_PURPLE, true);
        badge.setLetterSpacing(0.12f);
        badge.setPadding(dp(13), dp(9), dp(13), dp(9));
        badge.setBackground(rounded(Color.rgb(22, 20, 49), COLOR_BORDER, 24));
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
        pageContent.addView(description, margin(0, 0, 0, 22));

        Button explore = primaryButton(getString(R.string.choose_image));
        explore.setOnClickListener(view -> imagePicker.launch("image/*"));
        pageContent.addView(explore, margin(0, 0, 0, 14));

        TextView note = text(getString(R.string.hero_note), 12, COLOR_MUTED, false);
        pageContent.addView(note, margin(0, 0, 0, 6));

        ImageView illustration = new ImageView(this);
        illustration.setImageResource(R.drawable.hero_illustration);
        illustration.setAdjustViewBounds(true);
        illustration.setScaleType(ImageView.ScaleType.FIT_CENTER);
        pageContent.addView(illustration, margin(0, 16, 0, 28));
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
        uploadCard.setBackground(rounded(COLOR_SURFACE, COLOR_BORDER, 22));
        pageContent.addView(uploadCard, margin(0, 4, 0, 26));

        FrameLayout previewFrame = new FrameLayout(this);
        previewFrame.setMinimumHeight(dp(170));
        previewFrame.setBackground(rounded(Color.rgb(19, 22, 43), COLOR_BORDER, 16));
        imagePreview = new ImageView(this);
        imagePreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imagePreview.setAdjustViewBounds(true);
        imagePreview.setVisibility(View.GONE);
        previewFrame.addView(imagePreview, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        imagePlaceholder = text(getString(R.string.image_placeholder), 14, COLOR_MUTED, false);
        imagePlaceholder.setGravity(Gravity.CENTER);
        imagePlaceholder.setPadding(dp(24), dp(20), dp(24), dp(20));
        previewFrame.addView(imagePlaceholder, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        uploadCard.addView(previewFrame, margin(0, 0, 0, 14));

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
        submitButton = primaryButton("Process image");
        submitButton.setEnabled(false);
        submitButton.setAlpha(0.55f);
        submitButton.setOnClickListener(view -> submitSelectedImage());
        uploadCard.addView(submitButton, margin(0, 12, 0, 0));
    }

    private void buildClosingSection() {
        TextView title = text(getString(R.string.closing_title), 25, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        title.setLineSpacing(dp(2), 1f);
        pageContent.addView(title, margin(0, 8, 0, 10));

        TextView description = text(getString(R.string.closing_description), 14, COLOR_MUTED, false);
        description.setGravity(Gravity.CENTER);
        description.setLineSpacing(dp(5), 1f);
        pageContent.addView(description, margin(0, 0, 0, 12));
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
        iconView.setBackground(rounded(Color.rgb(32, 27, 63), COLOR_BORDER, 14));
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
        numberView.setBackground(rounded(Color.rgb(32, 27, 63), COLOR_BORDER, 12));
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
        card.setBackground(rounded(COLOR_SURFACE, COLOR_BORDER, 18));
        return card;
    }

    private void loadSelectedImage(Uri uri) {
        if (uri == null) {
            return;
        }

        ContentResolver resolver = getContentResolver();
        String mimeType = resolver.getType(uri);
        if (!ImageFileValidator.isSupportedMimeType(mimeType)) {
            imageLoadGeneration++;
            invalidateRequestAndResult();
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
        invalidateRequestAndResult();
        scrollToView(uploadCard);
        selectedImageUri = uri;
        selectedImageMime = mimeType;
        selectedImageBytes = null;
        selectedImageName = queryDisplayName(uri);
        imageFileInfo.setText(selectedImageName + " · checking file size…");
        updateSubmitButton();
        imageStatus.setText(R.string.image_loading);
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
            } catch (RuntimeException | OutOfMemoryError exception) {
                // Never leave the UI stuck on "Loading image preview…".
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
        imageLoadGeneration++;
        invalidateRequestAndResult();
        imagePreview.setImageDrawable(null);
        imagePreview.setVisibility(View.GONE);
        imagePlaceholder.setVisibility(View.VISIBLE);
        imageStatus.setText(R.string.image_upload_hint);
        selectedImageBytes = null;
        selectedImageUri = null;
        selectedImageName = null;
        selectedImageMime = null;
        imageFileInfo.setText("No image selected");
        updateSubmitButton();
    }

    /**
     * Drops any in-flight request (its response will be ignored), removes the previous result card
     * and clears the retry state. Called whenever the selection changes or the form is reset.
     */
    private void invalidateRequestAndResult() {
        requestGeneration++;
        processing = false;
        lastRequestFailed = false;
        clearResult();
    }

    private void clearResult() {
        if (uploadCard == null) return;
        View previousResult = uploadCard.findViewWithTag("api_result_card");
        if (previousResult != null) {
            uploadCard.removeView(previousResult);
        }
    }

    private void scrollToView(View target) {
        appScrollView.post(() -> {
            int top = target.getTop();
            if (target.getParent() == uploadCard) top += uploadCard.getTop();
            appScrollView.smoothScrollTo(0, Math.max(0, top - dp(12)));
        });
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
        boolean enabled = selectedImageBytes != null && selectedImageBytes.length > 0 && !processing;
        submitButton.setEnabled(enabled);
        submitButton.setAlpha(enabled ? 1f : 0.55f);
        submitButton.setText(processing ? "Processing…"
                : (lastRequestFailed ? "Try again" : "Process image"));
    }

    private void submitSelectedImage() {
        if (processing || selectedImageBytes == null || selectedImageBytes.length == 0) {
            imageStatus.setText("Choose a valid image before processing.");
            return;
        }
        clearResult();
        lastRequestFailed = false;
        processing = true;
        final long request = ++requestGeneration;
        updateSubmitButton();
        imageStatus.setText("Sending image to OpenVisionAPI…");
        byte[] payload = selectedImageBytes;
        String filename = selectedImageName;
        String mime = selectedImageMime;
        uploadWorker.execute(() -> {
            try {
                OpenVisionApiClient.ApiResult result = OpenVisionApiClient.processImage(
                        BuildConfig.IMAGE_API_URL,
                        BuildConfig.PIXICULAR_API_KEY,
                        payload,
                        filename,
                        mime);
                runOnUiThread(() -> {
                    // Ignore responses for a request the user has already reset or replaced.
                    if (request != requestGeneration || isFinishing() || isDestroyed()) return;
                    processing = false;
                    if (!result.isSuccess()) {
                        lastRequestFailed = true;
                        imageStatus.setText(ApiErrorMessages.forHttpStatus(result.statusCode));
                    } else if (!ApiResponseFormatter.isAnalysisSuccessful(result.body)) {
                        lastRequestFailed = true;
                        imageStatus.setText("The API did not return a successful analysis. "
                                + "Details are shown below; you can try again.");
                    } else {
                        imageStatus.setText("Processing completed (HTTP " + result.statusCode + ").");
                    }
                    updateSubmitButton();
                    showApiResult(result.body);
                });
            } catch (Exception exception) {
                runOnUiThread(() -> {
                    if (request != requestGeneration || isFinishing() || isDestroyed()) return;
                    processing = false;
                    lastRequestFailed = true;
                    updateSubmitButton();
                    imageStatus.setText(ApiErrorMessages.forException(exception));
                });
            }
        });
    }

    private void showApiResult(String response) {
        String formatted = ApiResponseFormatter.format(response);

        TextView result = text(formatted, 14, COLOR_TEXT, false);
        result.setTextIsSelectable(true);
        result.setLineSpacing(dp(5), 1f);
        result.setPadding(dp(14), dp(14), dp(14), dp(14));
        result.setBackground(rounded(Color.rgb(10, 14, 30), COLOR_BORDER, 12));

        // Replace the previous result rather than adding a new card every time.
        clearResult();
        result.setTag("api_result_card");

        int index = uploadCard.indexOfChild(imageFileInfo);
        uploadCard.addView(result,
                index >= 0 ? index + 1 : uploadCard.getChildCount(),
                margin(0, 0, 0, 12));
        scrollToView(result);
    }

    private View makeBrandIcon() {
        TextView icon = text("◈", 24, COLOR_PURPLE, true);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(rounded(Color.rgb(25, 21, 52), COLOR_BORDER, 12));
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

    private Button makeHeaderButton() {
        Button button = secondaryButton(getString(R.string.about_button));
        button.setOnClickListener(view -> appScrollView.smoothScrollTo(
                0, pageContent.getChildAt(pageContent.getChildCount() - 1).getTop()
        ));
        return button;
    }

    private Button primaryButton(String label) {
        Button button = button(label, Color.WHITE, Color.rgb(112, 91, 226));
        button.setBackground(rounded(Color.rgb(112, 91, 226), Color.TRANSPARENT, 14));
        return button;
    }

    private Button secondaryButton(String label) {
        Button button = button(label, COLOR_TEXT, COLOR_SURFACE);
        button.setBackground(rounded(COLOR_SURFACE, COLOR_BORDER, 13));
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
        button.setBackground(rounded(backgroundColor, COLOR_BORDER, 13));
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
        uploadWorker.shutdownNow();
        super.onDestroy();
    }

    private static final class ImageTooLargeException extends IOException {}
}