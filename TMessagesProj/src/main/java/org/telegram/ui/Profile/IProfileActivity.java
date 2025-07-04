package org.telegram.ui.Profile;


import android.content.Context;

import org.telegram.ui.Components.RecyclerListView;

public interface IProfileActivity {
    int getBirthdayRow();

    RecyclerListView getListView();

    Context getContext();

    int getCurrentAccount();

    long getDialogId();

    boolean isMyProfile();
}
