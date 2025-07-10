package org.telegram.ui.Profile;


import android.app.Activity;
import android.os.Bundle;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.ProfileActivity;

public class ProfileScreenFeatureConfigs {
    // Change this value to switch between ProfileActivity versions,
    // 1 for the legacy ProfileActivity and 2 for the new UI ProfileActivityV2.
    private static final int DEFAULT_PROFILE_SCREEN_VERSION = 2;

    private static int featureVersion() {
        // This method can be used to dynamically determine the version via remote configs.
        // For now, we return the default version.
        return DEFAULT_PROFILE_SCREEN_VERSION;
    }

    static ProfileActivityV2Configs profileActivityV2Configs = new ProfileActivityV2Configs();

    public static BaseFragment getProfileActivity(Bundle args) {
        BaseFragment instance;

        if (featureVersion() == 2) {
            instance = new ProfileActivityV2(args);
        } else {
            instance = new ProfileActivity(args);
        }

        return instance;
    }
    
    public static BaseFragment getProfileActivity(Bundle args, SharedMediaLayout.SharedMediaPreloader preloader) {
        BaseFragment instance;

        if (featureVersion() == 2) {
            instance = new ProfileActivityV2(args, preloader);
        } else {
            instance = new ProfileActivity(args, preloader);
        }

        return instance;
    }    

    public static BaseFragment getProfileActivityOf(long dialogId) {
        BaseFragment instance;
        
        if (featureVersion() == 2) {
            instance = ProfileActivityV2.of(dialogId);
        } else {
            instance = ProfileActivity.of(dialogId);
        }
        
        return instance;
    }

    public static void sendLogs(Activity activity, boolean last) {
        if (featureVersion() == 2) {
            ProfileActivityV2.sendLogs(activity, last);
        } else {
            ProfileActivity.sendLogs(activity, last);
        }
    }

    static class ProfileActivityV2Configs {
        int UI_PROFILE_SCROLL_ANIMATION_MIDDLE_STATE_AVATAR_SIZE_DP = 144;
    }
}
