package org.telegram.ui.Profile;


import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.Objects;

public class ProfileActivityLayout extends FrameLayout {
    
    static String TAG = ProfileActivityLayout.class.getSimpleName();

    static String INITIAL_TEXT = "Initial";

    private Boolean areChildrenInitialized = false;

    private Float verticalScrollOffset = 0f;

    private FrameLayout topViewContainer;
    private FrameLayout avatarImageViewFrame;

    private SimpleTextView progressTextView;

    public ProfileActivityLayout(Context context) {
        super(context);
        initializeChildren();
        bindChildren();
    }

    public ProfileActivityLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initializeChildren();
        bindChildren();
    }

    public ProfileActivityLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeChildren();
        bindChildren();
    }

    private void initializeChildren() {
        // Initialize views here if needed
        initializeProgressTextView();
        initializeTopViewContainer();
        initializeAvatarImageViewFrame();

        areChildrenInitialized = true;
    }

    private void bindChildren() {
        if (!areChildrenInitialized) initializeChildren();
        addChild(
                this,
                topViewContainer,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START),
                this::initializeTopViewContainer
        );

        addChild(
                topViewContainer,
                avatarImageViewFrame,
                LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.CENTER_HORIZONTAL),
                this::initializeAvatarImageViewFrame
        );

        addChild(
                this,
                progressTextView,
                LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER),
                this::initializeProgressTextView
        );
    }

    public void setVerticalScrollOffset(Float verticalScrollOffset) {
        if (Objects.equals(this.verticalScrollOffset, verticalScrollOffset)) {
            return; // No change in scroll offset
        }

        if (this.verticalScrollOffset == 0f) {
            if (verticalScrollOffset < 0f) onStartScrollAnimation(ScrollAnimationType.MIDDLE_TO_TOP);
            else onStartScrollAnimation(ScrollAnimationType.MIDDLE_TO_BOTTOM);
        } else if (this.verticalScrollOffset == 1f) {
            onStartScrollAnimation(ScrollAnimationType.BOTTOM_TO_MIDDLE);
        } else if (this.verticalScrollOffset == -1f) {
            onStartScrollAnimation(ScrollAnimationType.TOP_TO_MIDDLE);
        }

        if (verticalScrollOffset <= 0f) {
            ScrollAnimationType type = this.verticalScrollOffset > verticalScrollOffset?
                    ScrollAnimationType.MIDDLE_TO_TOP : ScrollAnimationType.TOP_TO_MIDDLE;
            onScrollAnimation(type, verticalScrollOffset);
        } else if (verticalScrollOffset > 0f) {
            ScrollAnimationType type = this.verticalScrollOffset < verticalScrollOffset?
                    ScrollAnimationType.MIDDLE_TO_BOTTOM : ScrollAnimationType.BOTTOM_TO_MIDDLE;
            onScrollAnimation(type, verticalScrollOffset);
        }

        if (verticalScrollOffset == 0f) {
            if (this.verticalScrollOffset < 0f) onFinishScrollAnimation(ScrollAnimationType.TOP_TO_MIDDLE);
            else onStartScrollAnimation(ScrollAnimationType.BOTTOM_TO_MIDDLE);
        } else if (verticalScrollOffset == 1f) {
            onFinishScrollAnimation(ScrollAnimationType.MIDDLE_TO_BOTTOM);
        } else if (verticalScrollOffset == -1f) {
            onFinishScrollAnimation(ScrollAnimationType.MIDDLE_TO_TOP);
        }

        this.verticalScrollOffset = verticalScrollOffset;
        requestRecomposition();
    }

    @SuppressLint("DefaultLocale")
    void requestRecomposition() {
        if (verticalScrollOffset < 0) {
            Log.i(TAG, "requestRecomposition - Scrolling up");
        }

        progressTextView.setText(String.format("%.4f", verticalScrollOffset));
    }

    private void onStartScrollAnimation(ScrollAnimationType type) {
        Log.i(TAG, "onStartScrollAnimation \t - " + type);
    }

    private void onScrollAnimation(ScrollAnimationType type, Float scrollOffset) {
        Log.d(TAG, "onScrollAnimation      \t - " + type + ", " + scrollOffset);


        float avatarTopPadding = AndroidUtilities.lerp(AndroidUtilities.statusBarHeight, 0f, Math.abs(scrollOffset));
        avatarImageViewFrame.setPadding(0, (int) avatarTopPadding, 0, 0);

        if (type.isShrinkAnimation()) {
            // Handle specific logic for scrolling to top

//            Float transition = AndroidUtilities.lerp(AndroidUtilities.statusBarHeight, 0f, -scrollOffset);
//            avatarImageViewFrame.setTranslationY(transition);
//            avatarImageViewFrame.setScaleX(1f + scrollOffset);
//            avatarImageViewFrame.setScaleY(1f + scrollOffset);
        } else if (type == ScrollAnimationType.MIDDLE_TO_BOTTOM || type == ScrollAnimationType.BOTTOM_TO_MIDDLE) {
            // Handle specific logic for scrolling to bottom
            Log.i(TAG, "Scrolling to bottom with offset: " + scrollOffset);
        }
    }

    private void onFinishScrollAnimation(ScrollAnimationType type) {
        Log.i(TAG, "onFinishScrollAnimation\t - " + type);

        if (type == ScrollAnimationType.TOP_TO_MIDDLE || type == ScrollAnimationType.BOTTOM_TO_MIDDLE) {
            avatarImageViewFrame.setPadding(0, AndroidUtilities.statusBarHeight, 0, 0);
        } else {
            avatarImageViewFrame.setPadding(0, 0, 0, 0);
        }
    }

    // Other methods to handle UI interactions can be added here

    private boolean addChild(ViewGroup parent, View child, LayoutParams layoutParams) {
        if (parent == null || child == null) {
            return false; // Cannot add null parent or child
        }
        if (child.getParent() != null) {
            ((ViewGroup) child.getParent()).removeView(child); // Remove from previous parent if exists
        }

        Log.i(TAG, "addChild - " + child);
        parent.addView(child, layoutParams);
        return true;
    }

    private void addChild(
            ViewGroup parent,
            View child,
            LayoutParams layoutParams,
            Integer retryCount,
            Runnable initializer
    ) {
        if (retryCount <= 0) {
            return; // Stop trying to add child after max retries
        }

        if (!addChild(parent, child, layoutParams)) {
            initializer.run(); // Initialize if not added
            addChild(parent, child, layoutParams, retryCount - 1, initializer);
        }
    }

    private void addChild(ViewGroup parent, View child, LayoutParams layoutParams, Runnable initializer) {
        addChild(parent, child, layoutParams, 2, initializer);
    }

    /** progressTextView initialization */
    private void initializeProgressTextView() {
        progressTextView = new SimpleTextView(getContext());
        progressTextView.setWidthWrapContent(true);
        progressTextView.setText(INITIAL_TEXT);
        progressTextView.setTextSize(18);
        progressTextView.setPadding(4, 4, 4, 4);
        progressTextView.setBackgroundColor(0x77ffffff);

        Log.i(TAG, "initializeProgressTextView");
    }

    /** topViewContainer initialization */
    private void initializeTopViewContainer() {
        topViewContainer = new FrameLayout(getContext());
        topViewContainer.setBackgroundColor(0x44ff0f0f);
        topViewContainer.setPadding(0, 0, 0, 100);

        Log.i(TAG, "initializeTopViewContainer");
    }

    /** avatarImageViewFrame initialization */
    private void initializeAvatarImageViewFrame() {
        avatarImageViewFrame = new FrameLayout(getContext());
        avatarImageViewFrame.setBackgroundColor(0x77ff0000);

        FrameLayout avatarImageView = new FrameLayout(getContext());
        avatarImageView.setBackgroundColor(0xff0000ff);
        avatarImageView.setLayoutParams(LayoutHelper.createFrame(144, 144));
        avatarImageViewFrame.addView(avatarImageView);

        Log.i(TAG, "initializeAvatarImageViewFrame");
    }
}
