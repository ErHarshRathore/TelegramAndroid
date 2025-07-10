package org.telegram.ui.Profile;


import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionIntroActivity;
import org.telegram.ui.ArchivedStickersActivity;
import org.telegram.ui.ArticleViewer;
import org.telegram.ui.AutoDeleteMessagesActivity;
import org.telegram.ui.CacheControlActivity;
import org.telegram.ui.Cells.GraySectionCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.SettingsSearchCell;
import org.telegram.ui.ChangeBioActivity;
import org.telegram.ui.ChangeNameActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.Premium.PremiumFeatureBottomSheet;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.DataAutoDownloadActivity;
import org.telegram.ui.DataSettingsActivity;
import org.telegram.ui.DataUsage2Activity;
import org.telegram.ui.FiltersSetupActivity;
import org.telegram.ui.LanguageSelectActivity;
import org.telegram.ui.LiteModeSettingsActivity;
import org.telegram.ui.LoginActivity;
import org.telegram.ui.NotificationsCustomSettingsActivity;
import org.telegram.ui.NotificationsSettingsActivity;
import org.telegram.ui.PasscodeActivity;
import org.telegram.ui.PremiumPreviewFragment;
import org.telegram.ui.PrivacyControlActivity;
import org.telegram.ui.PrivacySettingsActivity;
import org.telegram.ui.PrivacyUsersActivity;
import org.telegram.ui.ProxyListActivity;
import org.telegram.ui.ReactionsDoubleTapManageActivity;
import org.telegram.ui.SessionsActivity;
import org.telegram.ui.StickersActivity;
import org.telegram.ui.ThemeActivity;
import org.telegram.ui.TwoStepVerificationActivity;
import org.telegram.ui.WallpapersListActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

class SearchAdapter extends RecyclerListView.SelectionAdapter {

    private final ProfileActivityV2 parent;
    private final Context mContext;
    SearchAdapter.SearchResult[] searchArray;
    ArrayList<MessagesController.FaqSearchResult> faqSearchArray = new ArrayList<>();
    ArrayList<SearchAdapter.SearchResult> searchResults = new ArrayList<>();
    ArrayList<MessagesController.FaqSearchResult> faqSearchResults = new ArrayList<>();
    ArrayList<Object> recentSearches = new ArrayList<>();
    String lastSearchString;
    TLRPC.WebPage faqWebPage;
    private ArrayList<CharSequence> resultNames = new ArrayList<>();
    private boolean searchWas;
    private Runnable searchRunnable;
    private boolean loadingFaqPage;

    public SearchAdapter(ProfileActivityV2 parent) {
        this.parent = parent;
        mContext = parent.getContext();
        searchArray = onCreateSearchArray();

        updateSearchArray();
    }

    void updateSearchArray() {
        Log.d("TagHarsh", "SearchAdapter::updateSearchArray...");
        HashMap<Integer, SearchAdapter.SearchResult> resultHashMap = new HashMap<>();
        for (int a = 0; a < searchArray.length; a++) {
            if (searchArray[a] == null) {
                continue;
            }
            resultHashMap.put(searchArray[a].guid, searchArray[a]);
        }
        Set<String> set = MessagesController.getGlobalMainSettings().getStringSet("settingsSearchRecent2", null);
        if (set != null) {
            for (String value : set) {
                try {
                    SerializedData data = new SerializedData(Utilities.hexToBytes(value));
                    int num = data.readInt32(false);
                    int type = data.readInt32(false);
                    if (type == 0) {
                        String title = data.readString(false);
                        int count = data.readInt32(false);
                        String[] path = null;
                        if (count > 0) {
                            path = new String[count];
                            for (int a = 0; a < count; a++) {
                                path[a] = data.readString(false);
                            }
                        }
                        String url = data.readString(false);
                        MessagesController.FaqSearchResult result = new MessagesController.FaqSearchResult(title, path, url);
                        result.num = num;
                        recentSearches.add(result);
                    } else if (type == 1) {
                        SearchAdapter.SearchResult result = resultHashMap.get(data.readInt32(false));
                        if (result != null) {
                            result.num = num;
                            recentSearches.add(result);
                        }
                    }
                } catch (Exception ignore) {

                }
            }
        }
        Collections.sort(recentSearches, (o1, o2) -> {
            int n1 = getNum(o1);
            int n2 = getNum(o2);
            if (n1 < n2) {
                return -1;
            } else if (n1 > n2) {
                return 1;
            }
            return 0;
        });
    }

    SearchAdapter.SearchResult[] onCreateSearchArray() {
        Log.d("TagHarsh", "SearchAdapter::onCreateSearchArray...");
        return new SearchAdapter.SearchResult[]{
                new SearchAdapter.SearchResult(500, getString(R.string.EditName), 0, () -> parent.presentFragment(new ChangeNameActivity(parent.resourcesProvider))),
                new SearchAdapter.SearchResult(501, getString(R.string.ChangePhoneNumber), 0, () -> parent.presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER))),
                new SearchAdapter.SearchResult(502, getString(R.string.AddAnotherAccount), 0, () -> {
                    int freeAccount = -1;
                    for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                        if (!UserConfig.getInstance(a).isClientActivated()) {
                            freeAccount = a;
                            break;
                        }
                    }
                    if (freeAccount >= 0) {
                        parent.presentFragment(new LoginActivity(freeAccount));
                    }
                }),
                new SearchAdapter.SearchResult(503, getString(R.string.UserBio), 0, () -> {
                    if (parent.getUserInfo() != null) {
                        parent.presentFragment(new ChangeBioActivity());
                    }
                }),
                new SearchAdapter.SearchResult(504, getString(R.string.AddPhoto), 0, parent::onWriteButtonClick),

                new SearchAdapter.SearchResult(1, getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(2, getString(R.string.NotificationsPrivateChats), getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsCustomSettingsActivity(NotificationsController.TYPE_PRIVATE, new ArrayList<>(), null, true))),
                new SearchAdapter.SearchResult(3, getString(R.string.NotificationsGroups), getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsCustomSettingsActivity(NotificationsController.TYPE_GROUP, new ArrayList<>(), null, true))),
                new SearchAdapter.SearchResult(4, getString(R.string.NotificationsChannels), getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsCustomSettingsActivity(NotificationsController.TYPE_CHANNEL, new ArrayList<>(), null, true))),
                new SearchAdapter.SearchResult(5, getString(R.string.VoipNotificationSettings), "callsSectionRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(6, getString(R.string.BadgeNumber), "badgeNumberSection", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(7, getString(R.string.InAppNotifications), "inappSectionRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(8, getString(R.string.ContactJoined), "contactJoinedRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(9, getString(R.string.PinnedMessages), "pinnedMessageRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(10, getString(R.string.ResetAllNotifications), "resetNotificationsRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(11, getString(R.string.NotificationsService), "notificationsServiceRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(12, getString(R.string.NotificationsServiceConnection), "notificationsServiceConnectionRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),
                new SearchAdapter.SearchResult(13, getString(R.string.RepeatNotifications), "repeatRow", getString(R.string.NotificationsAndSounds), R.drawable.msg_notifications, () -> parent.presentFragment(new NotificationsSettingsActivity())),

                new SearchAdapter.SearchResult(100, getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacySettingsActivity())),
                new SearchAdapter.SearchResult(109, getString(R.string.TwoStepVerification), getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new TwoStepVerificationActivity())),
                new SearchAdapter.SearchResult(124, getString(R.string.AutoDeleteMessages), getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> {
                    if (parent.getUserConfig().getGlobalTTl() >= 0) {
                        parent.presentFragment(new AutoDeleteMessagesActivity());
                    }
                }),
                new SearchAdapter.SearchResult(108, getString(R.string.Passcode), getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(PasscodeActivity.determineOpenFragment())),
                SharedConfig.hasEmailLogin ? new SearchAdapter.SearchResult(125, getString(R.string.EmailLogin), "emailLoginRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())) : null,
                new SearchAdapter.SearchResult(101, getString(R.string.BlockedUsers), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacyUsersActivity().loadBlocked())),
                new SearchAdapter.SearchResult(110, getString(R.string.SessionsTitle), R.drawable.msg2_secret, () -> parent.presentFragment(new SessionsActivity(0))),
                new SearchAdapter.SearchResult(105, getString(R.string.PrivacyPhone), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_PHONE, true))),
                new SearchAdapter.SearchResult(102, getString(R.string.PrivacyLastSeen), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_LASTSEEN, true))),
                new SearchAdapter.SearchResult(103, getString(R.string.PrivacyProfilePhoto), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_PHOTO, true))),
                new SearchAdapter.SearchResult(104, getString(R.string.PrivacyForwards), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_FORWARDS, true))),
                new SearchAdapter.SearchResult(122, getString(R.string.PrivacyP2P), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_P2P, true))),
                new SearchAdapter.SearchResult(106, getString(R.string.Calls), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_CALLS, true))),
                new SearchAdapter.SearchResult(107, getString(R.string.PrivacyInvites), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> parent.presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_INVITE, true))),
                new SearchAdapter.SearchResult(123, getString(R.string.PrivacyVoiceMessages), getString(R.string.PrivacySettings), R.drawable.msg_secret, () -> {
                    if (!parent.getUserConfig().isPremium()) {
                        try {
                            parent.fragmentView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                        } catch (Exception ignored) {
                        }
                        BulletinFactory.of(parent).createRestrictVoiceMessagesPremiumBulletin().show();
                        return;
                    }
                    parent.presentFragment(new PrivacyControlActivity(ContactsController.PRIVACY_RULES_TYPE_VOICE_MESSAGES, true));
                }),
                parent.getMessagesController().autoarchiveAvailable ? new SearchAdapter.SearchResult(121, getString(R.string.ArchiveAndMute), "newChatsRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())) : null,
                new SearchAdapter.SearchResult(112, getString(R.string.DeleteAccountIfAwayFor2), "deleteAccountRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())),
                new SearchAdapter.SearchResult(113, getString(R.string.PrivacyPaymentsClear), "paymentsClearRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())),
                new SearchAdapter.SearchResult(114, getString(R.string.WebSessionsTitle), getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new SessionsActivity(1))),
                new SearchAdapter.SearchResult(115, getString(R.string.SyncContactsDelete), "contactsDeleteRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())),
                new SearchAdapter.SearchResult(116, getString(R.string.SyncContacts), "contactsSyncRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())),
                new SearchAdapter.SearchResult(117, getString(R.string.SuggestContacts), "contactsSuggestRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())),
                new SearchAdapter.SearchResult(118, getString(R.string.MapPreviewProvider), "secretMapRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())),
                new SearchAdapter.SearchResult(119, getString(R.string.SecretWebPage), "secretWebpageRow", getString(R.string.PrivacySettings), R.drawable.msg2_secret, () -> parent.presentFragment(new PrivacySettingsActivity())),

                new SearchAdapter.SearchResult(120, getString(R.string.Devices), R.drawable.msg2_devices, () -> parent.presentFragment(new SessionsActivity(0))),
                new SearchAdapter.SearchResult(121, getString(R.string.TerminateAllSessions), "terminateAllSessionsRow", getString(R.string.Devices), R.drawable.msg2_devices, () -> parent.presentFragment(new SessionsActivity(0))),
                new SearchAdapter.SearchResult(122, getString(R.string.LinkDesktopDevice), getString(R.string.Devices), R.drawable.msg2_devices, () -> parent.presentFragment(new SessionsActivity(0).setHighlightLinkDesktopDevice())),

                new SearchAdapter.SearchResult(200, getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(201, getString(R.string.DataUsage), "usageSectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(202, getString(R.string.StorageUsage), getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new CacheControlActivity())),
                new SearchAdapter.SearchResult(203, getString(R.string.KeepMedia), "keepMediaRow", getString(R.string.DataSettings), getString(R.string.StorageUsage), R.drawable.msg2_data, () -> parent.presentFragment(new CacheControlActivity())),
                new SearchAdapter.SearchResult(204, getString(R.string.ClearMediaCache), "cacheRow", getString(R.string.DataSettings), getString(R.string.StorageUsage), R.drawable.msg2_data, () -> parent.presentFragment(new CacheControlActivity())),
                new SearchAdapter.SearchResult(205, getString(R.string.LocalDatabase), "databaseRow", getString(R.string.DataSettings), getString(R.string.StorageUsage), R.drawable.msg2_data, () -> parent.presentFragment(new CacheControlActivity())),
                new SearchAdapter.SearchResult(206, getString(R.string.NetworkUsage), getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataUsage2Activity())),
                new SearchAdapter.SearchResult(207, getString(R.string.AutomaticMediaDownload), "mediaDownloadSectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(208, getString(R.string.WhenUsingMobileData), getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataAutoDownloadActivity(0))),
                new SearchAdapter.SearchResult(209, getString(R.string.WhenConnectedOnWiFi), getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataAutoDownloadActivity(1))),
                new SearchAdapter.SearchResult(210, getString(R.string.WhenRoaming), getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataAutoDownloadActivity(2))),
                new SearchAdapter.SearchResult(211, getString(R.string.ResetAutomaticMediaDownload), "resetDownloadRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(215, getString(R.string.Streaming), "streamSectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(216, getString(R.string.EnableStreaming), "enableStreamRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(217, getString(R.string.Calls), "callsSectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(218, getString(R.string.VoipUseLessData), "useLessDataForCallsRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(219, getString(R.string.VoipQuickReplies), "quickRepliesRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(220, getString(R.string.ProxySettings), getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new ProxyListActivity())),
                new SearchAdapter.SearchResult(221, getString(R.string.UseProxyForCalls), "callsRow", getString(R.string.DataSettings), getString(R.string.ProxySettings), R.drawable.msg2_data, () -> parent.presentFragment(new ProxyListActivity())),
                new SearchAdapter.SearchResult(111, getString(R.string.PrivacyDeleteCloudDrafts), "clearDraftsRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(222, getString(R.string.SaveToGallery), "saveToGallerySectionRow", getString(R.string.DataSettings), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(223, getString(R.string.SaveToGalleryPrivate), "saveToGalleryPeerRow", getString(R.string.DataSettings), getString(R.string.SaveToGallery), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(224, getString(R.string.SaveToGalleryGroups), "saveToGalleryGroupsRow", getString(R.string.DataSettings), getString(R.string.SaveToGallery), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),
                new SearchAdapter.SearchResult(225, getString(R.string.SaveToGalleryChannels), "saveToGalleryChannelsRow", getString(R.string.DataSettings), getString(R.string.SaveToGallery), R.drawable.msg2_data, () -> parent.presentFragment(new DataSettingsActivity())),

                new SearchAdapter.SearchResult(300, getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(301, getString(R.string.TextSizeHeader), "textSizeHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(302, getString(R.string.ChangeChatBackground), getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_ALL))),
                new SearchAdapter.SearchResult(303, getString(R.string.SetColor), null, getString(R.string.ChatSettings), getString(R.string.ChatBackground), R.drawable.msg2_discussion, () -> parent.presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_COLOR))),
                new SearchAdapter.SearchResult(304, getString(R.string.ResetChatBackgrounds), "resetRow", getString(R.string.ChatSettings), getString(R.string.ChatBackground), R.drawable.msg2_discussion, () -> parent.presentFragment(new WallpapersListActivity(WallpapersListActivity.TYPE_ALL))),
                new SearchAdapter.SearchResult(306, getString(R.string.ColorTheme), "themeHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(319, getString(R.string.BrowseThemes), null, getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_THEMES_BROWSER))),
                new SearchAdapter.SearchResult(320, getString(R.string.CreateNewTheme), "createNewThemeRow", getString(R.string.ChatSettings), getString(R.string.BrowseThemes), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_THEMES_BROWSER))),
                new SearchAdapter.SearchResult(321, getString(R.string.BubbleRadius), "bubbleRadiusHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(322, getString(R.string.ChatList), "chatListHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(323, getString(R.string.ChatListSwipeGesture), "swipeGestureHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(324, getString(R.string.AppIcon), "appIconHeaderRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(305, getString(R.string.AutoNightTheme), getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_NIGHT))),
                new SearchAdapter.SearchResult(328, getString(R.string.NextMediaTap), "nextMediaTapRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(327, getString(R.string.RaiseToListen), "raiseToListenRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(310, getString(R.string.RaiseToSpeak), "raiseToSpeakRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(326, getString(R.string.PauseMusicOnMedia), "pauseOnMediaRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(325, getString(R.string.MicrophoneForVoiceMessages), "bluetoothScoRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(308, getString(R.string.DirectShare), "directShareRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(311, getString(R.string.SendByEnter), "sendByEnterRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),
                new SearchAdapter.SearchResult(318, getString(R.string.DistanceUnits), "distanceRow", getString(R.string.ChatSettings), R.drawable.msg2_discussion, () -> parent.presentFragment(new ThemeActivity(ThemeActivity.THEME_TYPE_BASIC))),

                new SearchAdapter.SearchResult(600, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchAdapter.SearchResult(601, getString(R.string.SuggestStickers), "suggestRow", getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchAdapter.SearchResult(602, getString(R.string.FeaturedStickers), "featuredStickersHeaderRow", getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchAdapter.SearchResult(603, getString(R.string.Masks), null, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_MASK, null))),
                new SearchAdapter.SearchResult(604, getString(R.string.ArchivedStickers), null, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new ArchivedStickersActivity(MediaDataController.TYPE_IMAGE))),
                new SearchAdapter.SearchResult(605, getString(R.string.ArchivedMasks), null, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new ArchivedStickersActivity(MediaDataController.TYPE_MASK))),
                new SearchAdapter.SearchResult(606, getString(R.string.LargeEmoji), "largeEmojiRow", getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchAdapter.SearchResult(607, getString(R.string.LoopAnimatedStickers), "loopRow", getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_IMAGE, null))),
                new SearchAdapter.SearchResult(608, getString(R.string.Emoji), null, getString(R.string.StickersName), R.drawable.input_smile, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_EMOJIPACKS, null))),
                new SearchAdapter.SearchResult(609, getString(R.string.SuggestAnimatedEmoji), "suggestAnimatedEmojiRow", getString(R.string.StickersName), getString(R.string.Emoji), R.drawable.input_smile, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_EMOJIPACKS, null))),
                new SearchAdapter.SearchResult(610, getString(R.string.FeaturedEmojiPacks), "featuredStickersHeaderRow", getString(R.string.StickersName), getString(R.string.Emoji), R.drawable.input_smile, () -> parent.presentFragment(new StickersActivity(MediaDataController.TYPE_EMOJIPACKS, null))),
                new SearchAdapter.SearchResult(611, getString(R.string.DoubleTapSetting), null, getString(R.string.StickersName), R.drawable.msg2_sticker, () -> parent.presentFragment(new ReactionsDoubleTapManageActivity())),

                new SearchAdapter.SearchResult(700, getString(R.string.Filters), null, R.drawable.msg2_folder, () -> parent.presentFragment(new FiltersSetupActivity())),
                new SearchAdapter.SearchResult(701, getString(R.string.CreateNewFilter), "createFilterRow", getString(R.string.Filters), R.drawable.msg2_folder, () -> parent.presentFragment(new FiltersSetupActivity())),

                isPremiumFeatureAvailable(-1) ? new SearchAdapter.SearchResult(800, getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.presentFragment(new PremiumPreviewFragment("settings"))) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_LIMITS) ? new SearchAdapter.SearchResult(801, getString(R.string.PremiumPreviewLimits), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_LIMITS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_EMOJI) ? new SearchAdapter.SearchResult(802, getString(R.string.PremiumPreviewEmoji), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_EMOJI, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_UPLOAD_LIMIT) ? new SearchAdapter.SearchResult(803, getString(R.string.PremiumPreviewUploads), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_UPLOAD_LIMIT, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_DOWNLOAD_SPEED) ? new SearchAdapter.SearchResult(804, getString(R.string.PremiumPreviewDownloadSpeed), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_DOWNLOAD_SPEED, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_VOICE_TO_TEXT) ? new SearchAdapter.SearchResult(805, getString(R.string.PremiumPreviewVoiceToText), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_VOICE_TO_TEXT, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_ADS) ? new SearchAdapter.SearchResult(806, getString(R.string.PremiumPreviewNoAds), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_ADS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_REACTIONS) ? new SearchAdapter.SearchResult(807, getString(R.string.PremiumPreviewReactions), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_REACTIONS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_STICKERS) ? new SearchAdapter.SearchResult(808, getString(R.string.PremiumPreviewStickers), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_STICKERS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_ADVANCED_CHAT_MANAGEMENT) ? new SearchAdapter.SearchResult(809, getString(R.string.PremiumPreviewAdvancedChatManagement), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_ADVANCED_CHAT_MANAGEMENT, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_PROFILE_BADGE) ? new SearchAdapter.SearchResult(810, getString(R.string.PremiumPreviewProfileBadge), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_PROFILE_BADGE, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_AVATARS) ? new SearchAdapter.SearchResult(811, getString(R.string.PremiumPreviewAnimatedProfiles), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_ANIMATED_AVATARS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_APPLICATION_ICONS) ? new SearchAdapter.SearchResult(812, getString(R.string.PremiumPreviewAppIcon), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_APPLICATION_ICONS, false).setForceAbout())) : null,
                isPremiumFeatureAvailable(PremiumPreviewFragment.PREMIUM_FEATURE_EMOJI_STATUS) ? new SearchAdapter.SearchResult(813, getString(R.string.PremiumPreviewEmojiStatus), getString(R.string.TelegramPremium), R.drawable.msg_settings_premium, () -> parent.showDialog(new PremiumFeatureBottomSheet(parent, PremiumPreviewFragment.PREMIUM_FEATURE_EMOJI_STATUS, false).setForceAbout())) : null,

                new SearchAdapter.SearchResult(900, getString(R.string.PowerUsage), null, R.drawable.msg2_battery, () -> parent.presentFragment(new LiteModeSettingsActivity())),
                new SearchAdapter.SearchResult(901, getString(R.string.LiteOptionsStickers), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAGS_ANIMATED_STICKERS);
                }),
                new SearchAdapter.SearchResult(902, getString(R.string.LiteOptionsAutoplayKeyboard), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsStickers), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_STICKERS, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_STICKERS_KEYBOARD);
                }),
                new SearchAdapter.SearchResult(903, getString(R.string.LiteOptionsAutoplayChat), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsStickers), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_STICKERS, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_STICKERS_CHAT);
                }),
                new SearchAdapter.SearchResult(904, getString(R.string.LiteOptionsEmoji), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAGS_ANIMATED_EMOJI);
                }),
                new SearchAdapter.SearchResult(905, getString(R.string.LiteOptionsAutoplayKeyboard), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsEmoji), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_EMOJI, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_EMOJI_KEYBOARD);
                }),
                new SearchAdapter.SearchResult(906, getString(R.string.LiteOptionsAutoplayReactions), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsEmoji), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_EMOJI, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_EMOJI_REACTIONS);
                }),
                new SearchAdapter.SearchResult(907, getString(R.string.LiteOptionsAutoplayChat), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsEmoji), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_ANIMATED_EMOJI, true);
                    set.scrollToFlags(LiteMode.FLAG_ANIMATED_EMOJI_CHAT);
                }),
                new SearchAdapter.SearchResult(908, getString(R.string.LiteOptionsChat), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAGS_CHAT);
                }),
                new SearchAdapter.SearchResult(909, getString(R.string.LiteOptionsBackground), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_BACKGROUND);
                }),
                new SearchAdapter.SearchResult(910, getString(R.string.LiteOptionsTopics), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_FORUM_TWOCOLUMN);
                }),
                new SearchAdapter.SearchResult(911, getString(R.string.LiteOptionsSpoiler), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_SPOILER);
                }),
                SharedConfig.getDevicePerformanceClass() >= SharedConfig.PERFORMANCE_CLASS_AVERAGE ? new SearchAdapter.SearchResult(326 /* for compatibility */, getString(R.string.LiteOptionsBlur), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_BLUR);
                }) : null,
                new SearchAdapter.SearchResult(912, getString(R.string.LiteOptionsScale), null, getString(R.string.PowerUsage), getString(R.string.LiteOptionsChat), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.setExpanded(LiteMode.FLAGS_CHAT, true);
                    set.scrollToFlags(LiteMode.FLAG_CHAT_SCALE);
                }),
                new SearchAdapter.SearchResult(913, getString(R.string.LiteOptionsCalls), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAG_CALLS_ANIMATIONS);
                }),
                new SearchAdapter.SearchResult(214 /* for compatibility */, getString(R.string.LiteOptionsAutoplayVideo), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAG_AUTOPLAY_VIDEOS);
                }),
                new SearchAdapter.SearchResult(213 /* for compatibility */, getString(R.string.LiteOptionsAutoplayGifs), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.scrollToFlags(LiteMode.FLAG_AUTOPLAY_GIFS);
                }),
                new SearchAdapter.SearchResult(914, getString(R.string.LiteSmoothTransitions), getString(R.string.PowerUsage), R.drawable.msg2_battery, () -> {
                    LiteModeSettingsActivity set = new LiteModeSettingsActivity();
                    parent.presentFragment(set);
                    set.scrollToType(LiteModeSettingsActivity.SWITCH_TYPE_SMOOTH_TRANSITIONS);
                }),

                new SearchAdapter.SearchResult(400, getString(R.string.Language), R.drawable.msg2_language, () -> parent.presentFragment(new LanguageSelectActivity())),
                new SearchAdapter.SearchResult(405, getString(R.string.ShowTranslateButton), getString(R.string.Language), R.drawable.msg2_language, () -> parent.presentFragment(new LanguageSelectActivity())),
                MessagesController.getInstance(parent.getCurrentAccount()).getTranslateController().isContextTranslateEnabled() ? new SearchAdapter.SearchResult(406, getString(R.string.DoNotTranslate), getString(R.string.Language), R.drawable.msg2_language, () -> parent.presentFragment(new LanguageSelectActivity())) : null,

                new SearchAdapter.SearchResult(402, getString(R.string.AskAQuestion), getString(R.string.SettingsHelp), R.drawable.msg2_help, () -> parent.showDialog(AlertsCreator.createSupportAlert(parent, null))),
                new SearchAdapter.SearchResult(403, getString(R.string.TelegramFAQ), getString(R.string.SettingsHelp), R.drawable.msg2_help, () -> Browser.openUrl(parent.getParentActivity(), getString(R.string.TelegramFaqUrl))),
                new SearchAdapter.SearchResult(404, getString(R.string.PrivacyPolicy), getString(R.string.SettingsHelp), R.drawable.msg2_help, () -> Browser.openUrl(parent.getParentActivity(), getString(R.string.PrivacyPolicyUrl))),
        };
    }

    private boolean isPremiumFeatureAvailable(int feature) {
        if (parent.getMessagesController().premiumFeaturesBlocked() && !parent.getUserConfig().isPremium()) {
            return false;
        }

        if (feature == -1) {
            return true;
        }
        return parent.getMessagesController().premiumFeaturesTypesToPosition.get(feature, -1) != -1;
    }

    void loadFaqWebPage() {
        faqWebPage = parent.getMessagesController().faqWebPage;
        if (faqWebPage != null) {
            faqSearchArray.addAll(parent.getMessagesController().faqSearchArray);
        }
        if (faqWebPage != null || loadingFaqPage) {
            return;
        }
        loadingFaqPage = true;
        final TLRPC.TL_messages_getWebPage req2 = new TLRPC.TL_messages_getWebPage();
        req2.url = LocaleController.getString(R.string.TelegramFaqUrl);
        req2.hash = 0;
        parent.getConnectionsManager().sendRequest(req2, (response2, error2) -> {
            if (response2 instanceof TLRPC.TL_messages_webPage) {
                TLRPC.TL_messages_webPage res = (TLRPC.TL_messages_webPage) response2;
                MessagesController.getInstance(parent.getCurrentAccount()).putUsers(res.users, false);
                MessagesController.getInstance(parent.getCurrentAccount()).putChats(res.chats, false);
                response2 = res.webpage;
            }
            if (response2 instanceof TLRPC.WebPage) {
                ArrayList<MessagesController.FaqSearchResult> arrayList = new ArrayList<>();
                TLRPC.WebPage page = (TLRPC.WebPage) response2;
                if (page.cached_page != null) {
                    for (int a = 0, N = page.cached_page.blocks.size(); a < N; a++) {
                        TLRPC.PageBlock block = page.cached_page.blocks.get(a);
                        if (block instanceof TLRPC.TL_pageBlockList) {
                            String paragraph = null;
                            if (a != 0) {
                                TLRPC.PageBlock prevBlock = page.cached_page.blocks.get(a - 1);
                                if (prevBlock instanceof TLRPC.TL_pageBlockParagraph) {
                                    TLRPC.TL_pageBlockParagraph pageBlockParagraph = (TLRPC.TL_pageBlockParagraph) prevBlock;
                                    paragraph = ArticleViewer.getPlainText(pageBlockParagraph.text).toString();
                                }
                            }
                            TLRPC.TL_pageBlockList list = (TLRPC.TL_pageBlockList) block;
                            for (int b = 0, N2 = list.items.size(); b < N2; b++) {
                                TLRPC.PageListItem item = list.items.get(b);
                                if (item instanceof TLRPC.TL_pageListItemText) {
                                    TLRPC.TL_pageListItemText itemText = (TLRPC.TL_pageListItemText) item;
                                    String url = ArticleViewer.getUrl(itemText.text);
                                    String text = ArticleViewer.getPlainText(itemText.text).toString();
                                    if (TextUtils.isEmpty(url) || TextUtils.isEmpty(text)) {
                                        continue;
                                    }
                                    String[] path;
                                    if (paragraph != null) {
                                        path = new String[]{LocaleController.getString(R.string.SettingsSearchFaq), paragraph};
                                    } else {
                                        path = new String[]{LocaleController.getString(R.string.SettingsSearchFaq)};
                                    }
                                    arrayList.add(new MessagesController.FaqSearchResult(text, path, url));
                                }
                            }
                        } else if (block instanceof TLRPC.TL_pageBlockAnchor) {
                            break;
                        }
                    }
                    faqWebPage = page;
                }
                AndroidUtilities.runOnUIThread(() -> {
                    faqSearchArray.addAll(arrayList);
                    parent.getMessagesController().faqSearchArray = arrayList;
                    parent.getMessagesController().faqWebPage = faqWebPage;
                    if (!searchWas) {
                        notifyDataSetChanged();
                    }
                });
            }
            loadingFaqPage = false;
        });
    }

    @Override
    public int getItemCount() {
        if (searchWas) {
            return searchResults.size() + (faqSearchResults.isEmpty() ? 0 : 1 + faqSearchResults.size());
        }
        return (recentSearches.isEmpty() ? 0 : recentSearches.size() + 1) + (faqSearchArray.isEmpty() ? 0 : faqSearchArray.size() + 1);
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        return holder.getItemViewType() == 0;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case 0: {
                SettingsSearchCell searchCell = (SettingsSearchCell) holder.itemView;
                if (searchWas) {
                    if (position < searchResults.size()) {
                        SearchAdapter.SearchResult result = searchResults.get(position);
                        SearchAdapter.SearchResult prevResult = position > 0 ? searchResults.get(position - 1) : null;
                        int icon;
                        if (prevResult != null && prevResult.iconResId == result.iconResId) {
                            icon = 0;
                        } else {
                            icon = result.iconResId;
                        }
                        searchCell.setTextAndValueAndIcon(resultNames.get(position), result.path, icon, position < searchResults.size() - 1);
                    } else {
                        position -= searchResults.size() + 1;
                        MessagesController.FaqSearchResult result = faqSearchResults.get(position);
                        searchCell.setTextAndValue(resultNames.get(position + searchResults.size()), result.path, true, position < searchResults.size() - 1);
                    }
                } else {
                    if (!recentSearches.isEmpty()) {
                        position--;
                    }
                    if (position < recentSearches.size()) {
                        Object object = recentSearches.get(position);
                        if (object instanceof SearchAdapter.SearchResult) {
                            SearchAdapter.SearchResult result = (SearchAdapter.SearchResult) object;
                            searchCell.setTextAndValue(result.searchTitle, result.path, false, position < recentSearches.size() - 1);
                        } else if (object instanceof MessagesController.FaqSearchResult) {
                            MessagesController.FaqSearchResult result = (MessagesController.FaqSearchResult) object;
                            searchCell.setTextAndValue(result.title, result.path, true, position < recentSearches.size() - 1);
                        }
                    } else {
                        position -= recentSearches.size() + 1;
                        MessagesController.FaqSearchResult result = faqSearchArray.get(position);
                        searchCell.setTextAndValue(result.title, result.path, true, position < recentSearches.size() - 1);
                    }
                }
                break;
            }
            case 1: {
                GraySectionCell sectionCell = (GraySectionCell) holder.itemView;
                sectionCell.setText(LocaleController.getString(R.string.SettingsFaqSearchTitle));
                break;
            }
            case 2: {
                HeaderCell headerCell = (HeaderCell) holder.itemView;
                headerCell.setText(LocaleController.getString(R.string.SettingsRecent));
                break;
            }
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = new SettingsSearchCell(mContext);
                break;
            case 1:
                view = new GraySectionCell(mContext);
                break;
            case 2:
            default:
                view = new HeaderCell(mContext, 16);
                break;
        }
        view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        return new RecyclerListView.Holder(view);
    }

    @Override
    public int getItemViewType(int position) {
        if (searchWas) {
            if (position < searchResults.size()) {
                return 0;
            } else if (position == searchResults.size()) {
                return 1;
            }
        } else {
            if (position == 0) {
                if (!recentSearches.isEmpty()) {
                    return 2;
                } else {
                    return 1;
                }
            } else if (!recentSearches.isEmpty() && position == recentSearches.size() + 1) {
                return 1;
            }
        }
        return 0;
    }

    public void addRecent(Object object) {
        int index = recentSearches.indexOf(object);
        if (index >= 0) {
            recentSearches.remove(index);
        }
        recentSearches.add(0, object);
        if (!searchWas) {
            notifyDataSetChanged();
        }
        if (recentSearches.size() > 20) {
            recentSearches.remove(recentSearches.size() - 1);
        }
        LinkedHashSet<String> toSave = new LinkedHashSet<>();
        for (int a = 0, N = recentSearches.size(); a < N; a++) {
            Object o = recentSearches.get(a);
            if (o instanceof SearchAdapter.SearchResult) {
                ((SearchAdapter.SearchResult) o).num = a;
            } else if (o instanceof MessagesController.FaqSearchResult) {
                ((MessagesController.FaqSearchResult) o).num = a;
            }
            toSave.add(o.toString());
        }
        MessagesController.getGlobalMainSettings().edit().putStringSet("settingsSearchRecent2", toSave).commit();
    }

    public void clearRecent() {
        recentSearches.clear();
        MessagesController.getGlobalMainSettings().edit().remove("settingsSearchRecent2").commit();
        notifyDataSetChanged();
    }

    private int getNum(Object o) {
        if (o instanceof SearchAdapter.SearchResult) {
            return ((SearchAdapter.SearchResult) o).num;
        } else if (o instanceof MessagesController.FaqSearchResult) {
            return ((MessagesController.FaqSearchResult) o).num;
        }
        return 0;
    }

    public void search(String text) {
        lastSearchString = text;
        if (searchRunnable != null) {
            Utilities.searchQueue.cancelRunnable(searchRunnable);
            searchRunnable = null;
        }
        if (TextUtils.isEmpty(text)) {
            searchWas = false;
            searchResults.clear();
            faqSearchResults.clear();
            resultNames.clear();
            parent.emptyView.stickerView.getImageReceiver().startAnimation();
            parent.emptyView.title.setText(getString(R.string.SettingsNoRecent));
            notifyDataSetChanged();
            return;
        }
        Utilities.searchQueue.postRunnable(searchRunnable = () -> {
            ArrayList<SearchAdapter.SearchResult> results = new ArrayList<>();
            ArrayList<MessagesController.FaqSearchResult> faqResults = new ArrayList<>();
            ArrayList<CharSequence> names = new ArrayList<>();
            String[] searchArgs = text.split(" ");
            String[] translitArgs = new String[searchArgs.length];
            for (int a = 0; a < searchArgs.length; a++) {
                translitArgs[a] = LocaleController.getInstance().getTranslitString(searchArgs[a]);
                if (translitArgs[a].equals(searchArgs[a])) {
                    translitArgs[a] = null;
                }
            }

            for (int a = 0; a < searchArray.length; a++) {
                SearchAdapter.SearchResult result = searchArray[a];
                if (result == null) {
                    continue;
                }
                String title = " " + result.searchTitle.toLowerCase();
                SpannableStringBuilder stringBuilder = null;
                for (int i = 0; i < searchArgs.length; i++) {
                    if (searchArgs[i].length() != 0) {
                        String searchString = searchArgs[i];
                        int index = title.indexOf(" " + searchString);
                        if (index < 0 && translitArgs[i] != null) {
                            searchString = translitArgs[i];
                            index = title.indexOf(" " + searchString);
                        }
                        if (index >= 0) {
                            if (stringBuilder == null) {
                                stringBuilder = new SpannableStringBuilder(result.searchTitle);
                            }
                            stringBuilder.setSpan(new ForegroundColorSpan(parent.getThemedColor(Theme.key_windowBackgroundWhiteBlueText4)), index, index + searchString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        } else {
                            break;
                        }
                    }
                    if (stringBuilder != null && i == searchArgs.length - 1) {
                        if (result.guid == 502) {
                            int freeAccount = -1;
                            for (int b = 0; b < UserConfig.MAX_ACCOUNT_COUNT; b++) {
                                if (!UserConfig.getInstance(b).isClientActivated()) {
                                    freeAccount = b;
                                    break;
                                }
                            }
                            if (freeAccount < 0) {
                                continue;
                            }
                        }
                        results.add(result);
                        names.add(stringBuilder);
                    }
                }
            }
            if (faqWebPage != null) {
                for (int a = 0, N = faqSearchArray.size(); a < N; a++) {
                    MessagesController.FaqSearchResult result = faqSearchArray.get(a);
                    String title = " " + result.title.toLowerCase();
                    SpannableStringBuilder stringBuilder = null;
                    for (int i = 0; i < searchArgs.length; i++) {
                        if (searchArgs[i].length() != 0) {
                            String searchString = searchArgs[i];
                            int index = title.indexOf(" " + searchString);
                            if (index < 0 && translitArgs[i] != null) {
                                searchString = translitArgs[i];
                                index = title.indexOf(" " + searchString);
                            }
                            if (index >= 0) {
                                if (stringBuilder == null) {
                                    stringBuilder = new SpannableStringBuilder(result.title);
                                }
                                stringBuilder.setSpan(new ForegroundColorSpan(parent.getThemedColor(Theme.key_windowBackgroundWhiteBlueText4)), index, index + searchString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else {
                                break;
                            }
                        }
                        if (stringBuilder != null && i == searchArgs.length - 1) {
                            faqResults.add(result);
                            names.add(stringBuilder);
                        }
                    }
                }
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (!text.equals(lastSearchString)) {
                    return;
                }
                if (!searchWas) {
                    parent.emptyView.stickerView.getImageReceiver().startAnimation();
                    parent.emptyView.title.setText(LocaleController.getString(R.string.SettingsNoResults));
                }
                searchWas = true;
                searchResults = results;
                faqSearchResults = faqResults;
                resultNames = names;
                notifyDataSetChanged();
                parent.emptyView.stickerView.getImageReceiver().startAnimation();
            });
        }, 300);
    }

    public boolean isSearchWas() {
        return searchWas;
    }

    class SearchResult {

        private final String searchTitle;
        private final Runnable openRunnable;
        private final String rowName;
        private final int iconResId;
        private final int guid;
        private String[] path;
        private int num;

        public SearchResult(int g, String search, int icon, Runnable open) {
            this(g, search, null, null, null, icon, open);
        }

        public SearchResult(int g, String search, String pathArg1, int icon, Runnable open) {
            this(g, search, null, pathArg1, null, icon, open);
        }

        public SearchResult(int g, String search, String row, String pathArg1, int icon, Runnable open) {
            this(g, search, row, pathArg1, null, icon, open);
        }

        public SearchResult(int g, String search, String row, String pathArg1, String pathArg2, int icon, Runnable open) {
            guid = g;
            searchTitle = search;
            rowName = row;
            openRunnable = open;
            iconResId = icon;
            if (pathArg1 != null && pathArg2 != null) {
                path = new String[]{pathArg1, pathArg2};
            } else if (pathArg1 != null) {
                path = new String[]{pathArg1};
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof SearchAdapter.SearchResult)) {
                return false;
            }
            SearchAdapter.SearchResult result = (SearchAdapter.SearchResult) obj;
            return guid == result.guid;
        }

        @Override
        public String toString() {
            SerializedData data = new SerializedData();
            data.writeInt32(num);
            data.writeInt32(1);
            data.writeInt32(guid);
            return Utilities.bytesToHex(data.toByteArray());
        }

        void open() {
            openRunnable.run();
            AndroidUtilities.scrollToFragmentRow(parent.getParentLayout(), rowName);
        }
    }
}