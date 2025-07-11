package org.telegram.ui.Profile;


import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Stories.StoriesListPlaceProvider;

class ClippedListView extends RecyclerListView implements StoriesListPlaceProvider.ClippedView {
    private final ProfileActivityV2 parent;

    public ClippedListView(ProfileActivityV2 parent) {
        super(parent.getContext());
        this.parent = parent;
    }

    @Override
    public void updateClip(int[] clip) {
        clip[0] = parent.getActionBar().getMeasuredHeight();
        clip[1] = getMeasuredHeight() - getPaddingBottom();
    }
}
