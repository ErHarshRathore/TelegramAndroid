package org.telegram.ui.Profile;


import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

public class ProfileActivityLayout extends ViewGroup {
    public ProfileActivityLayout(Context context) {
        super(context);
    }

    public ProfileActivityLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ProfileActivityLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean hasChanged, int left, int top, int right, int bottom) {
        setBackgroundColor(Color.CYAN);
    }


    // Other methods to handle UI interactions can be added here
}
