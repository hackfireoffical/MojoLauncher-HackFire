package com.kdt.mcgui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.constraintlayout.widget.ConstraintLayout;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.progresskeeper.ProgressListener;
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener;

import java.util.ArrayList;

/** Compact, expandable task center. It observes existing launcher progress events. */
public class ProgressLayout extends ConstraintLayout implements View.OnClickListener, TaskCountListener {
    public static final String UNPACK_RUNTIME = "unpack_runtime";
    public static final String DOWNLOAD_MINECRAFT = "download_minecraft";
    public static final String DOWNLOAD_VERSION_LIST = "download_verlist";
    public static final String AUTHENTICATE = "authenticate";
    public static final String INSTALL_MODPACK = "install_modpack";
    public static final String EXTRACT_COMPONENTS = "extract_components";
    public static final String EXTRACT_SINGLE_FILES = "extract_single_files";
    public static final String INSTANCE_INSTALL = "instance_install";

    private final ArrayList<LayoutProgressListener> mMap = new ArrayList<>();
    private LinearLayout mLinearLayout;
    private TextView mTaskNumberDisplayer;
    private TextView mFlipArrow;
    private ScrollView mTaskScroll;
    private boolean mExpanded;

    public ProgressLayout(@NonNull Context context) { super(context); init(); }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(); }
    public ProgressLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) { super(context, attrs, defStyleAttr, defStyleRes); init(); }

    public void observe(String progressKey) { mMap.add(new LayoutProgressListener(progressKey)); }
    public void cleanUpObservers() {
        for (LayoutProgressListener listener : mMap) ProgressKeeper.removeListener(listener.progressKey, listener);
        mMap.clear();
    }
    public boolean hasProcesses() { return ProgressKeeper.getTaskCount() > 0; }

    private void init() {
        inflate(getContext(), R.layout.view_progress, this);
        mLinearLayout = findViewById(R.id.progress_linear_layout);
        mTaskNumberDisplayer = findViewById(R.id.progress_textview);
        mFlipArrow = findViewById(R.id.progress_flip_arrow);
        mTaskScroll = findViewById(R.id.progress_scroll);
        findViewById(R.id.progress_header).setOnClickListener(this);
        setVisibility(GONE);
    }

    public static void setProgress(String key, int progress) { ProgressKeeper.submitProgress(key, progress, -1, (Object) null); }
    public static void setProgress(String key, int progress, @StringRes int resource, Object... message) { ProgressKeeper.submitProgress(key, progress, resource, message); }
    public static void setProgress(String key, int progress, String message) { setProgress(key, progress, -1, message); }
    public static void clearProgress(String key) { setProgress(key, -1, -1); }

    public static boolean ensureNoOngoingTasks(Context context) {
        if (ProgressKeeper.hasOngoingTasks()) { Toast.makeText(context, R.string.tasks_ongoing, Toast.LENGTH_LONG).show(); return false; }
        return true;
    }
    public static boolean ensureNoOngoingTasks(Context context, String key) {
        if (ProgressKeeper.hasProgressKey(key)) { Toast.makeText(context, R.string.tasks_ongoing, Toast.LENGTH_LONG).show(); return false; }
        return true;
    }

    @Override public void onClick(View view) {
        mExpanded = !mExpanded;
        mTaskScroll.setVisibility(mExpanded ? VISIBLE : GONE);
        mFlipArrow.setText(mExpanded ? "⌄" : "⌃");
        animate().translationY(0).setDuration(160).start();
    }

    @Override public boolean onUpdateTaskCount(int count) {
        post(() -> {
            if (count > 0) {
                mTaskNumberDisplayer.setText(getContext().getString(R.string.progresslayout_tasks_in_progress, count));
                setVisibility(VISIBLE);
            } else {
                setVisibility(GONE);
                mExpanded = false;
                mTaskScroll.setVisibility(GONE);
                mFlipArrow.setText("⌃");
            }
        });
        return false;
    }

    private int dp(float value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private String taskTitle(String key) {
        if (DOWNLOAD_MINECRAFT.equals(key)) return "Downloading Minecraft";
        if (INSTALL_MODPACK.equals(key)) return "Installing mod or modpack";
        if (UNPACK_RUNTIME.equals(key)) return "Installing Java runtime";
        if (DOWNLOAD_VERSION_LIST.equals(key)) return "Refreshing game versions";
        if (AUTHENTICATE.equals(key)) return "Account authentication";
        if (EXTRACT_COMPONENTS.equals(key)) return "Extracting game components";
        if (EXTRACT_SINGLE_FILES.equals(key)) return "Extracting files";
        if (INSTANCE_INSTALL.equals(key)) return "Installing instance";
        return "Background task";
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(12));
        drawable.setStroke(dp(1), Color.rgb(65, 83, 86));
        return drawable;
    }

    class LayoutProgressListener implements ProgressListener {
        final String progressKey;
        final LinearLayout card;
        final TextView titleView;
        final TextView statusView;
        final TextView percentView;
        final ProgressBar progressBar;
        final LinearLayout.LayoutParams cardParams;

        LayoutProgressListener(String key) {
            progressKey = key;
            Context context = getContext();
            card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            card.setBackground(rounded(Color.rgb(42, 49, 50)));
            cardParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = dp(8);

            LinearLayout heading = new LinearLayout(context);
            heading.setOrientation(LinearLayout.HORIZONTAL);
            heading.setGravity(android.view.Gravity.CENTER_VERTICAL);
            titleView = new TextView(context);
            titleView.setText(taskTitle(key));
            titleView.setTextColor(Color.WHITE);
            titleView.setTextSize(13);
            titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            heading.addView(titleView, new LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1));
            percentView = new TextView(context);
            percentView.setTextColor(Color.rgb(132, 220, 226));
            percentView.setTextSize(12);
            percentView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            percentView.setText("…");
            heading.addView(percentView);
            card.addView(heading);

            statusView = new TextView(context);
            statusView.setTextColor(Color.rgb(190, 201, 202));
            statusView.setTextSize(11);
            statusView.setMaxLines(2);
            statusView.setPadding(0, dp(4), 0, dp(8));
            statusView.setText("Preparing…");
            card.addView(statusView);

            progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
            progressBar.setMax(100);
            progressBar.setProgressTintList(ColorStateList.valueOf(Color.rgb(119, 218, 222)));
            progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(67, 78, 80)));
            card.addView(progressBar, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(5)));
            ProgressKeeper.addListener(key, this);
        }

        @Override public void onProgressStarted() {
            post(() -> {
                if (card.getParent() == null) mLinearLayout.addView(card, cardParams);
                card.setAlpha(0f);
                card.animate().alpha(1f).setDuration(180).start();
            });
        }

        @Override public void onProgressUpdated(int progress, int resid, Object... args) {
            post(() -> {
                if (progress < 0) {
                    progressBar.setIndeterminate(true);
                    percentView.setText("Working");
                } else {
                    progressBar.setIndeterminate(false);
                    progressBar.setProgress(Math.min(100, Math.max(0, progress)));
                    percentView.setText(Math.min(100, Math.max(0, progress)) + "%");
                }
                String status = "";
                if (resid != -1) status = getContext().getString(resid, args);
                else if (args.length > 0 && args[0] != null) status = String.valueOf(args[0]);
                statusView.setText(status.isEmpty() ? "Working…" : status);
            });
        }

        @Override public void onProgressEnded() {
            post(() -> {
                if (card.getParent() == mLinearLayout) {
                    card.animate().alpha(0f).setDuration(120).withEndAction(() -> mLinearLayout.removeView(card)).start();
                }
            });
        }
    }
}
