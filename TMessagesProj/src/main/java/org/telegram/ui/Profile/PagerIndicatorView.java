package org.telegram.ui.Profile;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.view.View;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.Components.CubicBezierInterpolator;

class PagerIndicatorView extends View {

    private final RectF indicatorRect = new RectF();

    private final TextPaint textPaint;
    private final Paint backgroundPaint;

    private final ValueAnimator animator;
    private final float[] animatorValues = new float[]{0f, 1f};

    private final ProfileActivity parent;

    private final PagerAdapter adapter;

    private boolean isIndicatorVisible;

    public PagerIndicatorView(ProfileActivity parent) {
        super(parent.getContext());
        this.parent = parent;
        adapter = parent.avatarsViewPager.getAdapter();
        setVisibility(GONE);

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(AndroidUtilities.dpf2(15f));
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(0x26000000);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
        animator.addUpdateListener(a -> {
            final float value = AndroidUtilities.lerp(animatorValues, a.getAnimatedFraction());
            if (parent.searchItem != null && !parent.isPulledDown) {
                parent.searchItem.setScaleX(1f - value);
                parent.searchItem.setScaleY(1f - value);
                parent.searchItem.setAlpha(1f - value);
            }
            if (parent.editItemVisible) {
                parent.editItem.setScaleX(1f - value);
                parent.editItem.setScaleY(1f - value);
                parent.editItem.setAlpha(1f - value);
            }
            if (parent.callItemVisible) {
                parent.callItem.setScaleX(1f - value);
                parent.callItem.setScaleY(1f - value);
                parent.callItem.setAlpha(1f - value);
            }
            if (parent.videoCallItemVisible) {
                parent.videoCallItem.setScaleX(1f - value);
                parent.videoCallItem.setScaleY(1f - value);
                parent.videoCallItem.setAlpha(1f - value);
            }
            setScaleX(value);
            setScaleY(value);
            setAlpha(value);
        });
        boolean expanded = parent.expandPhoto;
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isIndicatorVisible) {
                    if (parent.searchItem != null) {
                        parent.searchItem.setClickable(false);
                    }
                    if (parent.editItemVisible) {
                        parent.editItem.setVisibility(GONE);
                    }
                    if (parent.callItemVisible) {
                        parent.callItem.setVisibility(GONE);
                    }
                    if (parent.videoCallItemVisible) {
                        parent.videoCallItem.setVisibility(GONE);
                    }
                } else {
                    setVisibility(GONE);
                }
                parent.updateStoriesViewBounds(false);
            }

            @Override
            public void onAnimationStart(Animator animation) {
                if (parent.searchItem != null && !expanded) {
                    parent.searchItem.setClickable(true);
                }
                if (parent.editItemVisible) {
                    parent.editItem.setVisibility(VISIBLE);
                }
                if (parent.callItemVisible) {
                    parent.callItem.setVisibility(VISIBLE);
                }
                if (parent.videoCallItemVisible) {
                    parent.videoCallItem.setVisibility(VISIBLE);
                }
                setVisibility(VISIBLE);
                parent.updateStoriesViewBounds(false);
            }
        });
        parent.avatarsViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            private int prevPage;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                int realPosition = parent.avatarsViewPager.getRealPosition(position);
                invalidateIndicatorRect(prevPage != realPosition);
                prevPage = realPosition;
                updateAvatarItems();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        adapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                int count = parent.avatarsViewPager.getRealCount();
                if (parent.overlayCountVisible == 0 && count > 1 && count <= 20 && parent.overlaysView.isOverlaysVisible()) {
                    parent.overlayCountVisible = 1;
                }
                invalidateIndicatorRect(false);
                refreshVisibility(1f);
                updateAvatarItems();
            }
        });
    }

    private void updateAvatarItemsInternal() {
        if (parent.otherItem == null || parent.avatarsViewPager == null) {
            return;
        }
        if (parent.isPulledDown) {
            int position = parent.avatarsViewPager.getRealPosition();
            if (position == 0) {
                parent.otherItem.hideSubItem(ProfileActivity.set_as_main);
                parent.otherItem.showSubItem(ProfileActivity.add_photo);
            } else {
                parent.otherItem.showSubItem(ProfileActivity.set_as_main);
                parent.otherItem.hideSubItem(ProfileActivity.add_photo);
            }
        }
    }

    private void updateAvatarItems() {
        if (parent.imageUpdater == null) {
            return;
        }
        if (parent.otherItem.isSubMenuShowing()) {
            AndroidUtilities.runOnUIThread(this::updateAvatarItemsInternal, 500);
        } else {
            updateAvatarItemsInternal();
        }
    }

    public boolean isIndicatorVisible() {
        return isIndicatorVisible;
    }

    public boolean isIndicatorFullyVisible() {
        return isIndicatorVisible && !animator.isRunning();
    }

    public void setIndicatorVisible(boolean indicatorVisible, float durationFactor) {
        if (indicatorVisible != isIndicatorVisible) {
            isIndicatorVisible = indicatorVisible;
            animator.cancel();
            final float value = AndroidUtilities.lerp(animatorValues, animator.getAnimatedFraction());
            if (durationFactor <= 0f) {
                animator.setDuration(0);
            } else if (indicatorVisible) {
                animator.setDuration((long) ((1f - value) * 250f / durationFactor));
            } else {
                animator.setDuration((long) (value * 250f / durationFactor));
            }
            animatorValues[0] = value;
            animatorValues[1] = indicatorVisible ? 1f : 0f;
            animator.start();
        }
    }

    public void refreshVisibility(float durationFactor) {
        setIndicatorVisible(parent.isPulledDown && parent.avatarsViewPager.getRealCount() > 20, durationFactor);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        invalidateIndicatorRect(false);
    }

    private void invalidateIndicatorRect(boolean pageChanged) {
        if (pageChanged) {
            parent.overlaysView.saveCurrentPageProgress();
        }
        parent.overlaysView.invalidate();
        final float textWidth = textPaint.measureText(getCurrentTitle());
        indicatorRect.right = getMeasuredWidth() - AndroidUtilities.dp(54f) - (parent.qrItem != null ? AndroidUtilities.dp(48) : 0);
        indicatorRect.left = indicatorRect.right - (textWidth + AndroidUtilities.dpf2(16f));
        indicatorRect.top = (parent.getActionBar().getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + AndroidUtilities.dp(15f);
        indicatorRect.bottom = indicatorRect.top + AndroidUtilities.dp(26);
        setPivotX(indicatorRect.centerX());
        setPivotY(indicatorRect.centerY());
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final float radius = AndroidUtilities.dpf2(12);
        canvas.drawRoundRect(indicatorRect, radius, radius, backgroundPaint);
        canvas.drawText(getCurrentTitle(), indicatorRect.centerX(), indicatorRect.top + AndroidUtilities.dpf2(18.5f), textPaint);
    }

    private String getCurrentTitle() {
        return adapter.getPageTitle(parent.avatarsViewPager.getCurrentItem()).toString();
    }

    ActionBarMenuItem getSecondaryMenuItem() {
        if (parent.callItemVisible) {
            return parent.callItem;
        } else if (parent.editItemVisible) {
            return parent.editItem;
        } else if (parent.searchItem != null) {
            return parent.searchItem;
        } else {
            return null;
        }
    }
}
