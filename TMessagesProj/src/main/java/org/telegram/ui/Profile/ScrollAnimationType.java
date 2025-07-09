package org.telegram.ui.Profile;


public enum ScrollAnimationType {
    NONE,
    MIDDLE_TO_TOP,
    MIDDLE_TO_BOTTOM,
    TOP_TO_MIDDLE,
    BOTTOM_TO_MIDDLE;

    Boolean isShrinkAnimation() {
        return this == MIDDLE_TO_TOP || this == TOP_TO_MIDDLE;
    }
}
