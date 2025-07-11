package org.telegram.ui.Profile;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ProfileChannelCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.ProfileBirthdayEffect;
import org.telegram.ui.Stars.ProfileGiftsView;

public interface IProfileActivity {
    int getBirthdayRow();

    RecyclerListView getListView();

    int getCurrentAccount();

    long getDialogId();

    boolean isMyProfile();

    void setPlayProfileAnimation(int type);

    void setUserInfo(
            TLRPC.UserFull value,
            ProfileChannelCell.ChannelMessageFetcher channelMessageFetcher,
            ProfileBirthdayEffect.BirthdayEffectFetcher birthdayAssetsFetcher
    );

    void setChatInfo(TLRPC.ChatFull value);

    UndoView getUndoView();

    boolean isSettings();

    boolean isChat();

    SharedMediaLayout getSharedMediaLayout();

    void scrollToSharedMedia();

    void scrollToSharedMedia(boolean animated);

    long getTopicId();

    boolean isTopic();

    ProfileGiftsView getGiftsView();
}
