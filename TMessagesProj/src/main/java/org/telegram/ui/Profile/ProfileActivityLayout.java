package org.telegram.ui.Profile;


import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.telegram.ui.Profile.ProfileScreenFeatureConfigs.ProfileActivityV2Configs.AvatarImageContainerAnimationConfigs;

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
    private FrameLayout avatarImageView;

    private SimpleTextView progressTextView;

    private final AvatarImageContainerAnimationConfigs avatarAnimationConfigs = new AvatarImageContainerAnimationConfigs();

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
        progressTextView.setText(String.format("%.4f", verticalScrollOffset));
    }

    private void onStartScrollAnimation(ScrollAnimationType type) {
        Log.i(TAG, "onStartScrollAnimation \t - " + type);

        avatarAnimationConfigs.initialTopPadding = ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarTopMarginDP;
        avatarAnimationConfigs.targetTopPadding = 0f;
        if (type == ScrollAnimationType.MIDDLE_TO_TOP) {
            avatarAnimationConfigs.initialSize = ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarSizeDP;
            avatarAnimationConfigs.targetSize = 0;
        } else if (type == ScrollAnimationType.MIDDLE_TO_BOTTOM || type == ScrollAnimationType.BOTTOM_TO_MIDDLE) {
            avatarAnimationConfigs.initialSize = ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarSizeDP;
            avatarAnimationConfigs.targetSize = super.getContext().getResources().getDisplayMetrics().widthPixels;
        }
    }

    private void onScrollAnimation(ScrollAnimationType type, Float scrollOffset) {
        Log.d(TAG, "onScrollAnimation      \t - " + type + ", " + scrollOffset);

        float avatarTopPadding = AndroidUtilities.lerp(
                ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarTopMarginDP,
                0f,
                Math.abs(scrollOffset)
        );
        avatarImageViewFrame.setPadding(0, (int) avatarTopPadding, 0, 0);

        if (type.isShrinkAnimation()) {
            // Handle specific logic for scrolling to top
            avatarAnimationBetweenTopAndMiddle(scrollOffset);
        } else if (type != ScrollAnimationType.NONE) {
            // Handle specific logic for scrolling to bottom
            avatarAnimationBetweenMiddleAndBottom(scrollOffset);
        }
    }

    private void onFinishScrollAnimation(ScrollAnimationType type) {
        Log.i(TAG, "onFinishScrollAnimation\t - " + type);

        if (type == ScrollAnimationType.TOP_TO_MIDDLE || type == ScrollAnimationType.BOTTOM_TO_MIDDLE) {
            avatarImageViewFrame.setPadding(0, ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarTopMarginDP, 0, 0);
        } else {
            avatarImageViewFrame.setPadding(0, 0, 0, 0);
        }
    }

    private void avatarAnimationBetweenTopAndMiddle(Float scrollOffset) {
        int avatarSize = (int) AndroidUtilities.lerp(
                avatarAnimationConfigs.initialSize,
                avatarAnimationConfigs.targetSize,
                -scrollOffset
        );
        avatarImageView.setLayoutParams(new LayoutParams(avatarSize, avatarSize));
    }

    private void avatarAnimationBetweenMiddleAndBottom(Float scrollOffset) {
        int avatarSize = (int) AndroidUtilities.lerp(
                avatarAnimationConfigs.initialSize,
                avatarAnimationConfigs.targetSize,
                scrollOffset
        );
        avatarImageView.setLayoutParams(new LayoutParams(avatarSize, avatarSize));
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
        progressTextView.setPadding(8, 8, 8, 8);
        progressTextView.setBackgroundColor(0xaaffffff);
    }

    /** topViewContainer initialization */
    private void initializeTopViewContainer() {
        topViewContainer = new FrameLayout(getContext());
        topViewContainer.setBackgroundColor(0x22ffffff);
        topViewContainer.setPadding(0, 0, 0, 100);
    }

    /** avatarImageViewFrame initialization */
    private void initializeAvatarImageViewFrame() {
        avatarImageViewFrame = new FrameLayout(getContext());
        avatarImageViewFrame.setBackgroundColor(0x22ff0000);

        if (verticalScrollOffset == 0f)
            avatarImageViewFrame.setPadding(0, ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarTopMarginDP, 0, 0);

        avatarImageView = new FrameLayout(getContext());
        avatarImageView.setBackgroundColor(0x440000ff);

        int size = ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarSizeDP;
        avatarImageView.setLayoutParams(new LayoutParams(size, size));
        avatarImageViewFrame.addView(avatarImageView);
    }
}
