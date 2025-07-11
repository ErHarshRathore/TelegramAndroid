package org.telegram.ui.Profile;


import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import org.telegram.ui.Profile.ProfileScreenFeatureConfigs.ProfileActivityV2Configs.AvatarImageContainerAnimationConfigs;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ProfileActivity;

import java.util.ArrayList;
import java.util.Objects;

public class ProfileActivityLayout extends FrameLayout {
    
    static String TAG = ProfileActivityLayout.class.getSimpleName();

    static String INITIAL_TEXT = "Initial";

    private Boolean areChildrenInitialized = false;

    private Float verticalScrollOffset = -1f;
    private ArrayList<Runnable> onVerticalScrollOffsetChangeListeners = new ArrayList<>();

    private FrameLayout topViewContainer;
    private FrameLayout topViewStickerOverlay;
    private FrameLayout profileGiftsOverlay;
    private RelativeLayout profileMetaOverlay;
    private FrameLayout avatarImageAnimatedContainer;
    private FrameLayout avatarImageFrame;
    private LinearLayout metadataContainer;
    private FrameLayout profileNameContainer;
    private FrameLayout smallTextContainer;
    private FrameLayout profileActionButtonsContainer;

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

        areChildrenInitialized = true;
    }

    private void bindChildren() {
        if (!areChildrenInitialized) initializeChildren();

        // TODO @Harsh - remove this UI
        addChild(
                this,
                progressTextView,
                LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER),
                this::initializeProgressTextView
        );
        setVerticalScrollOffset(-0.5f);
        setVerticalScrollOffset(0f);
    }

    private void addOnVerticalScrollOffsetChangeListener(Runnable listener) {
        onVerticalScrollOffsetChangeListeners.add(listener);
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
        for (Runnable listener : onVerticalScrollOffsetChangeListeners) {
            AndroidUtilities.runOnUIThread(listener);
        }

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
        if (type == ScrollAnimationType.MIDDLE_TO_TOP || type == ScrollAnimationType.TOP_TO_MIDDLE) {
            avatarAnimationConfigs.initialSize = ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarSizeDP;
            avatarAnimationConfigs.targetSize = 0;

            avatarAnimationConfigs.initialBottomPadding = metadataContainer.getHeight();
            avatarAnimationConfigs.targetBottomPadding = metadataContainer.getHeight() + AndroidUtilities.statusBarHeight;
        } else if (type == ScrollAnimationType.MIDDLE_TO_BOTTOM || type == ScrollAnimationType.BOTTOM_TO_MIDDLE) {
            avatarAnimationConfigs.initialSize = ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarSizeDP;
            avatarAnimationConfigs.targetSize = super.getContext().getResources().getDisplayMetrics().widthPixels;

            avatarAnimationConfigs.initialBottomPadding = metadataContainer.getHeight();
            avatarAnimationConfigs.targetBottomPadding = 0;
        }
    }

    private void onScrollAnimation(ScrollAnimationType type, Float scrollOffset) {
        Log.d(TAG, "onScrollAnimation      \t - " + type + ", " + scrollOffset);

        int avatarTopPadding = (int) AndroidUtilities.lerp(
                avatarAnimationConfigs.initialTopPadding,
                avatarAnimationConfigs.targetTopPadding,
                Math.abs(scrollOffset)
        );
        int avatarBottomPadding = (int) AndroidUtilities.lerp(
                avatarAnimationConfigs.initialBottomPadding,
                avatarAnimationConfigs.targetBottomPadding,
                Math.abs(scrollOffset)
        );
        avatarImageAnimatedContainer.setPadding(0, avatarTopPadding, 0, avatarBottomPadding);

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
            avatarImageAnimatedContainer.setPadding(0, (int) avatarAnimationConfigs.initialTopPadding, 0, (int) avatarAnimationConfigs.initialBottomPadding);
//            metadataContainer.setTranslationY(metadataContainerAnimationConfigs.initialTranslationY);
        } else {
            avatarImageAnimatedContainer.setPadding(0, (int) avatarAnimationConfigs.targetTopPadding, 0, (int) avatarAnimationConfigs.targetBottomPadding);
        }
    }

    private void avatarAnimationBetweenTopAndMiddle(Float scrollOffset) {
        int avatarSize = (int) AndroidUtilities.lerp(
                avatarAnimationConfigs.initialSize,
                avatarAnimationConfigs.targetSize,
                -scrollOffset
        );
        avatarImageFrame.setLayoutParams(new LayoutParams(avatarSize, avatarSize));
    }

    private void avatarAnimationBetweenMiddleAndBottom(Float scrollOffset) {
        int avatarSize = (int) AndroidUtilities.lerp(
                avatarAnimationConfigs.initialSize,
                avatarAnimationConfigs.targetSize,
                scrollOffset
        );
        avatarImageFrame.setLayoutParams(new LayoutParams(avatarSize, avatarSize));
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
        topViewContainer.setPadding(0, 0, 0, 0);

        addChild(
                this,
                topViewContainer,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.START),
                () -> {}
        );

        initializeTopViewStickerOverlay();
        initializeProfileGiftsOverlay();
        initializeProfileMetaOverlay();
    }

    /** topViewStickerOverlay initialization */
    private void initializeTopViewStickerOverlay() {
        topViewStickerOverlay = new FrameLayout(getContext());
        topViewStickerOverlay.setId(View.generateViewId());
//        topViewStickerOverlay.setBackgroundColor(0x77000000);
        topViewStickerOverlay.setPadding(0, 0, 0, 0);

        topViewContainer.addView(
                topViewStickerOverlay,
                LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        );
    }

    /** profileGiftsOverlay initialization */
    private void initializeProfileGiftsOverlay() {
        profileGiftsOverlay = new FrameLayout(getContext());
        profileGiftsOverlay.setId(View.generateViewId());
//        profileGiftsOverlay.setBackgroundColor(0x22ffffff);
        profileGiftsOverlay.setPadding(0, 0, 0, 100);

        topViewContainer.addView(
                profileGiftsOverlay,
                LayoutHelper.createRelative(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        );
    }

    /** profileMetaOverlay initialization */
    private void initializeProfileMetaOverlay() {
        profileMetaOverlay = new RelativeLayout(getContext());
        profileMetaOverlay.setId(View.generateViewId());
//        profileMetaOverlay.setBackgroundColor(0x55448822);
        profileMetaOverlay.setPadding(0, 0, 0, 0);

        topViewContainer.addView(
                profileMetaOverlay,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT)
        );

        initializeAvatarImageAnimatedContainer();
        initializeMetadataContainer();

        avatarImageAnimatedContainer.setPadding(
                0,
                ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarTopMarginDP,
                0,
                metadataContainer.getHeight()
        );
    }

    /** avatarImageAnimatedContainer initialization */
    private void initializeAvatarImageAnimatedContainer() {
        avatarImageAnimatedContainer = new FrameLayout(getContext());
        avatarImageAnimatedContainer.setId(View.generateViewId());

        initializeAvatarImageFrame();
        profileMetaOverlay.addView(
                avatarImageAnimatedContainer,
                LayoutHelper.createRelative(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, RelativeLayout.CENTER_HORIZONTAL | RelativeLayout.ALIGN_PARENT_TOP)
        );

    }

    /** avatarImageViewFrame initialization */
    private void initializeAvatarImageFrame() {
        avatarImageFrame = new FrameLayout(getContext());
        int size = ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarSizeDP;
        avatarImageFrame.setLayoutParams(new LayoutParams(size, size));
        addChild(avatarImageAnimatedContainer, avatarImageFrame, LayoutHelper.createFrameWrapContent(), this::initializeAvatarImageAnimatedContainer);
    }


    /** metadataContainer initialization */
    private void initializeMetadataContainer() {
        metadataContainer = new LinearLayout(getContext());
        metadataContainer.setId(View.generateViewId());
        metadataContainer.setOrientation(LinearLayout.VERTICAL);
        metadataContainer.setBackgroundColor(0xaa000000);
        int sidePadding = AndroidUtilities.dp(16);
        metadataContainer.setPadding(sidePadding, avatarImageAnimatedContainer.getHeight() + sidePadding, sidePadding, sidePadding);

        initializeProfileNameContainer();
        initializeSmallTextContainer();
        initializeProfileActionButtonsContainer();

        RelativeLayout.LayoutParams params = LayoutHelper.createRelative(
                LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                RelativeLayout.CENTER_HORIZONTAL
        );
        params.addRule(RelativeLayout.ALIGN_BOTTOM, avatarImageAnimatedContainer.getId());

        profileMetaOverlay.addView(metadataContainer, params);
    }

    /** profileNameContainer initialization */
    private void initializeProfileNameContainer() {
        profileNameContainer = new FrameLayout(getContext());
        profileNameContainer.setBackgroundColor(0xaa00ffff);
        profileNameContainer.setPadding(0, 0, 0, 0);

        addChild(
                metadataContainer,
                profileNameContainer,
//                LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL),
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 16, Gravity.CENTER_HORIZONTAL),
                this::initializeMetadataContainer
        );
    }

    /** smallTextContainer initialization */
    private void initializeSmallTextContainer() {
        smallTextContainer = new FrameLayout(getContext());
        smallTextContainer.setBackgroundColor(0xaaff00ff);
        smallTextContainer.setPadding(0, AndroidUtilities.dp(16), 0, 0);

        addChild(
                metadataContainer,
                smallTextContainer,
//                LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL),
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 16, Gravity.CENTER_HORIZONTAL),
                this::initializeMetadataContainer
        );
    }

    /** profileActionButtonsContainer initialization */
    private void initializeProfileActionButtonsContainer() {
        profileActionButtonsContainer = new FrameLayout(getContext());
        profileActionButtonsContainer.setBackgroundColor(0xaaffff00);
        profileActionButtonsContainer.setPadding(0, AndroidUtilities.dp(16), 0, 0);

        addChild(
                metadataContainer,
                profileActionButtonsContainer,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL),
                this::initializeMetadataContainer
        );
    }


    // ================================= View item placing methods =================================

    void bindAvatarContainer(
            FrameLayout avatarImageContainer,
            ProfileActivity.AvatarImageView avatarView
    ) {
        addChild(
                avatarImageFrame,
                avatarImageContainer,
                LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER),
                () -> {}
        );

        int radius = ProfileScreenFeatureConfigs.profileActivityV2Configs.uiScrollStateMiddleAvatarSizeDP/2;
        addOnVerticalScrollOffsetChangeListener (() -> {
            avatarView.setRoundRadius((int) AndroidUtilities.lerp(radius, 0f, verticalScrollOffset));

            avatarView.setVisibility(verticalScrollOffset < 1f? View.VISIBLE : View.GONE);
        });
    }
}
