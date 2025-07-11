package org.telegram.ui.Profile;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.Stars.StarsIntroActivity.formatStarsAmountShort;
import static org.telegram.ui.bots.AffiliateProgramFragment.percents;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BirthdayController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_bots;
import org.telegram.tgnet.tl.TL_fragment;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionIntroActivity;
import org.telegram.ui.Business.ProfileHoursCell;
import org.telegram.ui.Business.ProfileLocationCell;
import org.telegram.ui.Cells.AboutLinkCell;
import org.telegram.ui.Cells.DividerCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ProfileChannelCell;
import org.telegram.ui.Cells.SettingsSuggestionCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.UserCell;
import org.telegram.ui.ChannelMonetizationLayout;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.IdenticonDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Premium.PremiumGradient;
import org.telegram.ui.Components.Premium.ProfilePremiumCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UndoView;
import org.telegram.ui.FragmentUsernameBottomSheet;
import org.telegram.ui.Stars.BotStarsController;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stars.StarsIntroActivity;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;
import org.telegram.ui.TwoStepVerificationSetupActivity;
import org.telegram.ui.UserInfoActivity;
import org.telegram.ui.bots.AffiliateProgramFragment;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

class ListAdapter extends RecyclerListView.SelectionAdapter {
    final static int
            VIEW_TYPE_HEADER = 1,
            VIEW_TYPE_TEXT_DETAIL = 2,
            VIEW_TYPE_ABOUT_LINK = 3,
            VIEW_TYPE_TEXT = 4,
            VIEW_TYPE_DIVIDER = 5,
            VIEW_TYPE_NOTIFICATIONS_CHECK = 6,
            VIEW_TYPE_SHADOW = 7,
            VIEW_TYPE_USER = 8,
            VIEW_TYPE_EMPTY = 11,
            VIEW_TYPE_BOTTOM_PADDING = 12,
            VIEW_TYPE_SHARED_MEDIA = 13,
            VIEW_TYPE_VERSION = 14,
            VIEW_TYPE_SUGGESTION = 15,
            VIEW_TYPE_ADDTOGROUP_INFO = 17,
            VIEW_TYPE_PREMIUM_TEXT_CELL = 18,
            VIEW_TYPE_TEXT_DETAIL_MULTILINE = 19,
            VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE = 20,
            VIEW_TYPE_LOCATION = 21,
            VIEW_TYPE_HOURS = 22,
            VIEW_TYPE_CHANNEL = 23,
            VIEW_TYPE_STARS_TEXT_CELL = 24,
            VIEW_TYPE_BOT_APP = 25,
            VIEW_TYPE_SHADOW_TEXT = 26,
            VIEW_TYPE_COLORFUL_TEXT = 27;

    private final ProfileActivityV2 parent;
    private final HashMap<TLRPC.TL_username, ClickableSpan> usernameSpans = new HashMap<TLRPC.TL_username, ClickableSpan>();
    private final Context mContext;

    public ListAdapter(ProfileActivityV2 parent) {
        this.parent = parent;
        mContext = parent.getContext();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup p, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_TYPE_HEADER: {
                view = new HeaderCell(mContext, 23, parent.resourcesProvider);
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            }
            case VIEW_TYPE_TEXT_DETAIL_MULTILINE:
            case VIEW_TYPE_TEXT_DETAIL:
                final TextDetailCell textDetailCell = new TextDetailCell(
                        mContext,
                        parent.resourcesProvider,
                        viewType == VIEW_TYPE_TEXT_DETAIL_MULTILINE
                ) {
                    @Override
                    protected int processColor(int color) {
                        return parent.dontApplyPeerColor(color, false);
                    }
                };
                textDetailCell.setContentDescriptionValueFirst(true);
                view = textDetailCell;
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            case VIEW_TYPE_ABOUT_LINK: {
                view = parent.aboutLinkCell = new AboutLinkCell(mContext, parent, parent.resourcesProvider) {
                    @Override
                    protected void didPressUrl(String url, Browser.Progress progress) {
                        parent.openUrl(url, progress);
                    }

                    @Override
                    protected void didResizeEnd() {
                        parent.layoutManager.mIgnoreTopPadding = false;
                    }

                    @Override
                    protected void didResizeStart() {
                        parent.layoutManager.mIgnoreTopPadding = true;
                    }

                    @Override
                    protected int processColor(int color) {
                        return parent.dontApplyPeerColor(color, false);
                    }
                };
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            }
            case VIEW_TYPE_TEXT: {
                view = new TextCell(mContext, parent.resourcesProvider) {
                    @Override
                    protected int processColor(int color) {
                        return parent.dontApplyPeerColor(color, false);
                    }
                };
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            }
            case VIEW_TYPE_DIVIDER: {
                view = new DividerCell(mContext, parent.resourcesProvider);
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                view.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(4), 0, 0);
                break;
            }
            case VIEW_TYPE_NOTIFICATIONS_CHECK: {
                view = new NotificationsCheckCell(mContext, 23, 70, false, parent.resourcesProvider) {
                    @Override
                    protected int processColor(int color) {
                        return parent.dontApplyPeerColor(color, false);
                    }
                };
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            }
            case VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE: {
                view = new TextCheckCell(mContext, parent.resourcesProvider);
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            }
            case VIEW_TYPE_SHADOW: {
                view = new ShadowSectionCell(mContext, parent.resourcesProvider);
                break;
            }
            case VIEW_TYPE_SHADOW_TEXT: {
                view = new TextInfoPrivacyCell(mContext, parent.resourcesProvider);
                break;
            }
            case VIEW_TYPE_COLORFUL_TEXT: {
                view = new AffiliateProgramFragment.ColorfulTextCell(mContext, parent.resourcesProvider);
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            }
            case VIEW_TYPE_USER: {
                view = new UserCell(mContext, parent.addMemberRow == -1 ? 9 : 6, 0, true, parent.resourcesProvider);
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            }
            case VIEW_TYPE_EMPTY: {
                view = new View(mContext) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(32), MeasureSpec.EXACTLY));
                    }
                };
                break;
            }
            case VIEW_TYPE_BOTTOM_PADDING: {
                view = new View(mContext) {

                    private int lastPaddingHeight = 0;
                    private int lastListViewHeight = 0;

                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        if (lastListViewHeight != parent.getListView().getMeasuredHeight()) {
                            lastPaddingHeight = 0;
                        }
                        lastListViewHeight = parent.getListView().getMeasuredHeight();
                        int n = parent.getListView().getChildCount();
                        if (n == parent.listAdapter.getItemCount()) {
                            int totalHeight = 0;
                            for (int i = 0; i < n; i++) {
                                View view = parent.getListView().getChildAt(i);
                                int p = parent.getListView().getChildAdapterPosition(view);
                                if (p >= 0 && p != parent.bottomPaddingRow) {
                                    totalHeight += parent.getListView().getChildAt(i).getMeasuredHeight();
                                }
                            }
                            int paddingHeight = (parent.fragmentView == null ? 0 : parent.fragmentView.getMeasuredHeight()) - ActionBar.getCurrentActionBarHeight() - AndroidUtilities.statusBarHeight - totalHeight;
                            if (paddingHeight > AndroidUtilities.dp(88)) {
                                paddingHeight = 0;
                            }
                            if (paddingHeight <= 0) {
                                paddingHeight = 0;
                            }
                            setMeasuredDimension(parent.getListView().getMeasuredWidth(), lastPaddingHeight = paddingHeight);
                        } else {
                            setMeasuredDimension(parent.getListView().getMeasuredWidth(), lastPaddingHeight);
                        }
                    }
                };
                view.setBackground(new ColorDrawable(Color.TRANSPARENT));
                break;
            }
            case VIEW_TYPE_SHARED_MEDIA: {
                if (parent.sharedMediaLayout.getParent() != null) {
                    ((ViewGroup) parent.sharedMediaLayout.getParent()).removeView(parent.sharedMediaLayout);
                }
                view = parent.sharedMediaLayout;
                break;
            }
            case VIEW_TYPE_ADDTOGROUP_INFO: {
                view = new TextInfoPrivacyCell(mContext, parent.resourcesProvider);
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            }
            case VIEW_TYPE_LOCATION:
                view = new ProfileLocationCell(mContext, parent.resourcesProvider);
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            case VIEW_TYPE_HOURS:
                view = new ProfileHoursCell(mContext, parent.resourcesProvider) {
                    @Override
                    protected int processColor(int color) {
                        return parent.dontApplyPeerColor(color, false);
                    }
                };
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            case VIEW_TYPE_VERSION:
            default: {
                TextInfoPrivacyCell cell = new TextInfoPrivacyCell(mContext, 10, parent.resourcesProvider);
                cell.getTextView().setGravity(Gravity.CENTER_HORIZONTAL);
                cell.getTextView().setTextColor(parent.getThemedColor(Theme.key_windowBackgroundWhiteGrayText3));
                cell.getTextView().setMovementMethod(null);
                try {
                    PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                    int code = pInfo.versionCode / 10;
                    String abi = "";
                    switch (pInfo.versionCode % 10) {
                        case 1:
                        case 2:
                            abi = "store bundled " + Build.CPU_ABI + " " + Build.CPU_ABI2;
                            break;
                        default:
                        case 9:
                            if (ApplicationLoader.isStandaloneBuild()) {
                                abi = "direct " + Build.CPU_ABI + " " + Build.CPU_ABI2;
                            } else {
                                abi = "universal " + Build.CPU_ABI + " " + Build.CPU_ABI2;
                            }
                            break;
                    }
                    cell.setText(formatString("TelegramVersion", R.string.TelegramVersion, String.format(Locale.US, "v%s (%d) %s", pInfo.versionName, code, abi)));
                } catch (Exception e) {
                    FileLog.e(e);
                }
                cell.getTextView().setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));
                view = cell;
                view.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, parent.getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                break;
            }
            case VIEW_TYPE_SUGGESTION: {
                view = new SettingsSuggestionCell(mContext, parent.resourcesProvider) {
                    @Override
                    protected void onYesClick(int type) {
                        AndroidUtilities.runOnUIThread(() -> {
                            parent.getNotificationCenter().removeObserver(parent, NotificationCenter.newSuggestionsAvailable);
                            if (type == SettingsSuggestionCell.TYPE_GRACE) {
                                parent.getMessagesController().removeSuggestion(0, "PREMIUM_GRACE");
                                Browser.openUrl(mContext, parent.getMessagesController().premiumManageSubscriptionUrl);
                            } else {
                                parent.getMessagesController().removeSuggestion(0, type == SettingsSuggestionCell.TYPE_PHONE ? "VALIDATE_PHONE_NUMBER" : "VALIDATE_PASSWORD");
                            }
                            parent.getNotificationCenter().addObserver(parent, NotificationCenter.newSuggestionsAvailable);
                            parent.updateListAnimated(false);
                        });
                    }

                    @Override
                    protected void onNoClick(int type) {
                        if (type == SettingsSuggestionCell.TYPE_PHONE) {
                            parent.presentFragment(new ActionIntroActivity(ActionIntroActivity.ACTION_TYPE_CHANGE_PHONE_NUMBER));
                        } else {
                            parent.presentFragment(new TwoStepVerificationSetupActivity(TwoStepVerificationSetupActivity.TYPE_VERIFY, null));
                        }
                    }
                };
                break;
            }
            case VIEW_TYPE_PREMIUM_TEXT_CELL:
            case VIEW_TYPE_STARS_TEXT_CELL:
                view = new ProfilePremiumCell(mContext, viewType == VIEW_TYPE_PREMIUM_TEXT_CELL ? 0 : 1, parent.resourcesProvider);
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            case VIEW_TYPE_CHANNEL:
                view = new ProfileChannelCell(parent) {
                    @Override
                    public int processColor(int color) {
                        return parent.dontApplyPeerColor(color, false);
                    }
                };
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
            case VIEW_TYPE_BOT_APP:
                FrameLayout frameLayout = new FrameLayout(mContext);
                ButtonWithCounterView button = new ButtonWithCounterView(mContext, parent.resourcesProvider);
                button.setText(LocaleController.getString(R.string.ProfileBotOpenApp), false);
                button.setOnClickListener(v -> {
                    TLRPC.User bot = parent.getMessagesController().getUser(parent.userId);
                    parent.getMessagesController().openApp(parent, bot, null, parent.getClassGuid(), null);
                });
                frameLayout.addView(button, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.FILL, 18, 14, 18, 14));
                view = frameLayout;
                view.setBackgroundColor(parent.getThemedColor(Theme.key_windowBackgroundWhite));
                break;
        }
        if (viewType != VIEW_TYPE_SHARED_MEDIA) {
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
        }
        return new RecyclerListView.Holder(view);
    }

    @Override
    public void onViewAttachedToWindow(RecyclerView.ViewHolder holder) {
        if (holder.itemView == parent.sharedMediaLayout) {
            parent.sharedMediaLayoutAttached = true;
        }
        if (holder.itemView instanceof TextDetailCell) {
            ((TextDetailCell) holder.itemView).textView.setLoading(parent.loadingSpan);
            ((TextDetailCell) holder.itemView).valueTextView.setLoading(parent.loadingSpan);
        }
    }

    @Override
    public void onViewDetachedFromWindow(RecyclerView.ViewHolder holder) {
        if (holder.itemView == parent.sharedMediaLayout) {
            parent.sharedMediaLayoutAttached = false;
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_HEADER:
                HeaderCell headerCell = (HeaderCell) holder.itemView;
                if (position == parent.infoHeaderRow) {
                    if (ChatObject.isChannel(parent.getCurrentChat()) && !parent.getCurrentChat().megagroup && parent.channelInfoRow != -1) {
                        headerCell.setText(LocaleController.getString(R.string.ReportChatDescription));
                    } else {
                        headerCell.setText(LocaleController.getString(R.string.Info));
                    }
                } else if (position == parent.membersHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.ChannelMembers));
                } else if (position == parent.settingsSectionRow2) {
                    headerCell.setText(LocaleController.getString(R.string.SETTINGS));
                } else if (position == parent.numberSectionRow) {
                    headerCell.setText(LocaleController.getString(R.string.Account));
                } else if (position == parent.helpHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.SettingsHelp));
                } else if (position == parent.debugHeaderRow) {
                    headerCell.setText(LocaleController.getString(R.string.SettingsDebug));
                } else if (position == parent.botPermissionsHeader) {
                    headerCell.setText(LocaleController.getString(R.string.BotProfilePermissions));
                }
                headerCell.setTextColor(parent.dontApplyPeerColor(parent.getThemedColor(Theme.key_windowBackgroundWhiteBlueHeader), false));
                break;
            case VIEW_TYPE_TEXT_DETAIL_MULTILINE:
            case VIEW_TYPE_TEXT_DETAIL:
                TextDetailCell detailCell = (TextDetailCell) holder.itemView;
                boolean containsQr = false;
                boolean containsGift = false;
                if (position == parent.birthdayRow) {
                    TLRPC.UserFull userFull = parent.getMessagesController().getUserFull(parent.userId);
                    if (userFull != null && userFull.birthday != null) {
                        final boolean today = BirthdayController.isToday(userFull);
                        final boolean withYear = (userFull.birthday.flags & 1) != 0;
                        final int age = withYear ? Period.between(LocalDate.of(userFull.birthday.year, userFull.birthday.month, userFull.birthday.day), LocalDate.now()).getYears() : -1;

                        String text = UserInfoActivity.birthdayString(userFull.birthday);

                        if (withYear) {
                            text = LocaleController.formatPluralString(today ? "ProfileBirthdayTodayValueYear" : "ProfileBirthdayValueYear", age, text);
                        } else {
                            text = LocaleController.formatString(today ? R.string.ProfileBirthdayTodayValue : R.string.ProfileBirthdayValue, text);
                        }

                        detailCell.setTextAndValue(
                                Emoji.replaceWithRestrictedEmoji(text, detailCell.textView, () -> {
                                    if (holder.getAdapterPosition() == position && parent.birthdayRow == position && holder.getItemViewType() == VIEW_TYPE_TEXT_DETAIL) {
                                        onBindViewHolder(holder, position);
                                    }
                                }),
                                LocaleController.getString(today ? R.string.ProfileBirthdayToday : R.string.ProfileBirthday),
                                parent.isTopic || parent.bizHoursRow != -1 || parent.bizLocationRow != -1
                        );

                        containsGift = !parent.myProfile && today && !parent.getMessagesController().premiumPurchaseBlocked();
                    }
                } else if (position == parent.phoneRow) {
                    String text;
                    TLRPC.User user = parent.getMessagesController().getUser(parent.userId);
                    String phoneNumber;
                    if (user != null && !TextUtils.isEmpty(parent.vcardPhone)) {
                        text = PhoneFormat.getInstance().format("+" + parent.vcardPhone);
                        phoneNumber = parent.vcardPhone;
                    } else if (user != null && !TextUtils.isEmpty(user.phone)) {
                        text = PhoneFormat.getInstance().format("+" + user.phone);
                        phoneNumber = user.phone;
                    } else {
                        text = LocaleController.getString(R.string.PhoneHidden);
                        phoneNumber = null;
                    }
                    parent.isFragmentPhoneNumber = phoneNumber != null && phoneNumber.matches("888\\d{8}");
                    detailCell.setTextAndValue(text, LocaleController.getString(parent.isFragmentPhoneNumber ? R.string.AnonymousNumber : R.string.PhoneMobile), false);
                } else if (position == parent.usernameRow) {
                    String username = null;
                    CharSequence text;
                    CharSequence value;
                    ArrayList<TLRPC.TL_username> usernames = new ArrayList<>();
                    if (parent.userId != 0) {
                        final TLRPC.User user = parent.getMessagesController().getUser(parent.userId);
                        if (user != null) {
                            usernames.addAll(user.usernames);
                        }
                        TLRPC.TL_username usernameObj = null;
                        if (user != null && !TextUtils.isEmpty(user.username)) {
                            usernameObj = DialogObject.findUsername(user.username, usernames);
                            username = user.username;
                        }
                        usernames = user == null ? new ArrayList<>() : new ArrayList<>(user.usernames);
                        if (TextUtils.isEmpty(username) && usernames != null) {
                            for (int i = 0; i < usernames.size(); ++i) {
                                TLRPC.TL_username u = usernames.get(i);
                                if (u != null && u.active && !TextUtils.isEmpty(u.username)) {
                                    usernameObj = u;
                                    username = u.username;
                                    break;
                                }
                            }
                        }
                        value = LocaleController.getString(R.string.Username);
                        if (username != null) {
                            text = "@" + username;
                            if (usernameObj != null && !usernameObj.editable) {
                                text = new SpannableString(text);
                                ((SpannableString) text).setSpan(makeUsernameLinkSpan(usernameObj), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        } else {
                            text = "—";
                        }
                        containsQr = true;
                    } else if (parent.getCurrentChat() != null) {
                        TLRPC.Chat chat = parent.getMessagesController().getChat(parent.chatId);
                        username = ChatObject.getPublicUsername(chat);
                        if (chat != null) {
                            usernames.addAll(chat.usernames);
                        }
                        if (ChatObject.isPublic(chat)) {
                            containsQr = true;
                            text = parent.getMessagesController().linkPrefix + "/" + username + (parent.getTopicId() != 0 ? "/" + parent.getTopicId() : "");
                            value = LocaleController.getString(R.string.InviteLink);
                        } else {
                            text = parent.getMessagesController().linkPrefix + "/c/" + parent.chatId + (parent.getTopicId() != 0 ? "/" + parent.getTopicId() : "");
                            value = LocaleController.getString(R.string.InviteLinkPrivate);
                        }
                    } else {
                        text = "";
                        value = "";
                        usernames = new ArrayList<>();
                    }
                    detailCell.setTextAndValue(text, alsoUsernamesString(username, usernames, value), (parent.isTopic || parent.bizHoursRow != -1 || parent.bizLocationRow != -1) && parent.birthdayRow < 0);
                } else if (position == parent.locationRow) {
                    if (parent.chatInfo != null && parent.chatInfo.location instanceof TLRPC.TL_channelLocation) {
                        TLRPC.TL_channelLocation location = (TLRPC.TL_channelLocation) parent.chatInfo.location;
                        detailCell.setTextAndValue(location.address, LocaleController.getString(R.string.AttachLocation), false);
                    }
                } else if (position == parent.numberRow) {
                    TLRPC.User user = UserConfig.getInstance(parent.getCurrentAccount()).getCurrentUser();
                    String value;
                    if (user != null && user.phone != null && !user.phone.isEmpty()) {
                        value = PhoneFormat.getInstance().format("+" + user.phone);
                    } else {
                        value = LocaleController.getString(R.string.NumberUnknown);
                    }
                    detailCell.setTextAndValue(value, LocaleController.getString(R.string.TapToChangePhone), true);
                    detailCell.setContentDescriptionValueFirst(false);
                } else if (position == parent.setUsernameRow) {
                    TLRPC.User user = UserConfig.getInstance(parent.getCurrentAccount()).getCurrentUser();
                    String text = "";
                    CharSequence value = LocaleController.getString(R.string.Username);
                    String username = null;
                    if (user != null && user.usernames.size() > 0) {
                        for (int i = 0; i < user.usernames.size(); ++i) {
                            TLRPC.TL_username u = user.usernames.get(i);
                            if (u != null && u.active && !TextUtils.isEmpty(u.username)) {
                                username = u.username;
                                break;
                            }
                        }
                        if (username == null) {
                            username = user.username;
                        }
                        if (username == null || TextUtils.isEmpty(username)) {
                            text = LocaleController.getString(R.string.UsernameEmpty);
                        } else {
                            text = "@" + username;
                        }
                        value = alsoUsernamesString(username, user.usernames, value);
                    } else {
                        username = UserObject.getPublicUsername(user);
                        if (user != null && !TextUtils.isEmpty(username)) {
                            text = "@" + username;
                        } else {
                            text = LocaleController.getString(R.string.UsernameEmpty);
                        }
                    }
                    detailCell.setTextAndValue(text, value, true);
                    detailCell.setContentDescriptionValueFirst(true);
                }
                if (containsGift) {
                    Drawable drawable = ContextCompat.getDrawable(detailCell.getContext(), R.drawable.msg_input_gift);
                    drawable.setColorFilter(new PorterDuffColorFilter(parent.dontApplyPeerColor(parent.getThemedColor(Theme.key_switch2TrackChecked), false), PorterDuff.Mode.MULTIPLY));
                    if (UserObject.areGiftsDisabled(parent.getUserInfo())) {
                        detailCell.setImage(null);
                        detailCell.setImageClickListener(null);
                    } else {
                        detailCell.setImage(drawable, LocaleController.getString(R.string.GiftPremium));
                        detailCell.setImageClickListener(parent::onTextDetailCellImageClicked);
                    }
                } else if (containsQr) {
                    Drawable drawable = ContextCompat.getDrawable(detailCell.getContext(), R.drawable.msg_qr_mini);
                    drawable.setColorFilter(new PorterDuffColorFilter(parent.dontApplyPeerColor(parent.getThemedColor(Theme.key_switch2TrackChecked), false), PorterDuff.Mode.MULTIPLY));
                    detailCell.setImage(drawable, LocaleController.getString(R.string.GetQRCode));
                    detailCell.setImageClickListener(parent::onTextDetailCellImageClicked);
                } else {
                    detailCell.setImage(null);
                    detailCell.setImageClickListener(null);
                }
                detailCell.setTag(position);
                detailCell.textView.setLoading(parent.loadingSpan);
                detailCell.valueTextView.setLoading(parent.loadingSpan);
                break;
            case VIEW_TYPE_ABOUT_LINK:
                AboutLinkCell aboutLinkCell = (AboutLinkCell) holder.itemView;
                if (position == parent.userInfoRow) {
                    TLRPC.User user = parent.getUserInfo().user != null ? parent.getUserInfo().user : parent.getMessagesController().getUser(parent.getUserInfo().id);
                    boolean addlinks = parent.isBot || (user != null && user.premium && parent.getUserInfo().about != null);
                    aboutLinkCell.setTextAndValue(parent.getUserInfo().about, LocaleController.getString(R.string.UserBio), addlinks);
                } else if (position == parent.channelInfoRow) {
                    String text = parent.chatInfo.about;
                    while (text.contains("\n\n\n")) {
                        text = text.replace("\n\n\n", "\n\n");
                    }
                    aboutLinkCell.setText(text, ChatObject.isChannel(parent.getCurrentChat()) && !parent.getCurrentChat().megagroup);
                } else if (position == parent.bioRow) {
                    String value;
                    if (parent.getUserInfo() == null || !TextUtils.isEmpty(parent.getUserInfo().about)) {
                        value = parent.getUserInfo() == null ? LocaleController.getString(R.string.Loading) : parent.getUserInfo().about;
                        aboutLinkCell.setTextAndValue(value, LocaleController.getString(R.string.UserBio), parent.getUserConfig().isPremium());
                        parent.currentBio = parent.getUserInfo() != null ? parent.getUserInfo().about : null;
                    } else {
                        aboutLinkCell.setTextAndValue(LocaleController.getString(R.string.UserBio), LocaleController.getString(R.string.UserBioDetail), false);
                        parent.currentBio = null;
                    }
                    aboutLinkCell.setMoreButtonDisabled(true);
                }
                break;
            case VIEW_TYPE_PREMIUM_TEXT_CELL:
            case VIEW_TYPE_STARS_TEXT_CELL:
            case VIEW_TYPE_TEXT:
                TextCell textCell = (TextCell) holder.itemView;
                textCell.setColors(Theme.key_windowBackgroundWhiteGrayIcon, Theme.key_windowBackgroundWhiteBlackText);
                textCell.setTag(Theme.key_windowBackgroundWhiteBlackText);
                if (position == parent.settingsTimerRow) {
                    TLRPC.EncryptedChat encryptedChat = parent.getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(parent.getDialogId()));
                    String value;
                    if (encryptedChat.ttl == 0) {
                        value = LocaleController.getString(R.string.ShortMessageLifetimeForever);
                    } else {
                        value = LocaleController.formatTTLString(encryptedChat.ttl);
                    }
                    textCell.setTextAndValue(LocaleController.getString(R.string.MessageLifetime), value, false, false);
                } else if (position == parent.unblockRow) {
                    textCell.setText(LocaleController.getString(R.string.Unblock), false);
                    textCell.setColors(-1, Theme.key_text_RedRegular);
                } else if (position == parent.settingsKeyRow) {
                    IdenticonDrawable identiconDrawable = new IdenticonDrawable();
                    TLRPC.EncryptedChat encryptedChat = parent.getMessagesController().getEncryptedChat(DialogObject.getEncryptedChatId(parent.getDialogId()));
                    identiconDrawable.setEncryptedChat(encryptedChat);
                    textCell.setTextAndValueDrawable(LocaleController.getString(R.string.EncryptionKey), identiconDrawable, false);
                } else if (position == parent.joinRow) {
                    textCell.setColors(-1, Theme.key_windowBackgroundWhiteBlueText2);
                    if (parent.getCurrentChat().megagroup) {
                        textCell.setText(LocaleController.getString(R.string.ProfileJoinGroup), false);
                    } else {
                        textCell.setText(LocaleController.getString(R.string.ProfileJoinChannel), false);
                    }
                } else if (position == parent.subscribersRow) {
                    if (parent.chatInfo != null) {
                        if (ChatObject.isChannel(parent.getCurrentChat()) && !parent.getCurrentChat().megagroup) {
                            textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ChannelSubscribers), LocaleController.formatNumber(parent.chatInfo.participants_count, ','), R.drawable.msg_groups, position != parent.membersSectionRow - 1);
                        } else {
                            textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ChannelMembers), LocaleController.formatNumber(parent.chatInfo.participants_count, ','), R.drawable.msg_groups, position != parent.membersSectionRow - 1);
                        }
                    } else {
                        if (ChatObject.isChannel(parent.getCurrentChat()) && !parent.getCurrentChat().megagroup) {
                            textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelSubscribers), R.drawable.msg_groups, position != parent.membersSectionRow - 1);
                        } else {
                            textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelMembers), R.drawable.msg_groups, position != parent.membersSectionRow - 1);
                        }
                    }
                } else if (position == parent.subscribersRequestsRow) {
                    if (parent.chatInfo != null) {
                        textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.SubscribeRequests), String.format("%d", parent.chatInfo.requests_pending), R.drawable.msg_requests, position != parent.membersSectionRow - 1);
                    }
                } else if (position == parent.administratorsRow) {
                    if (parent.chatInfo != null) {
                        textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ChannelAdministrators), String.format("%d", parent.chatInfo.admins_count), R.drawable.msg_admins, position != parent.membersSectionRow - 1);
                    } else {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelAdministrators), R.drawable.msg_admins, position != parent.membersSectionRow - 1);
                    }
                } else if (position == parent.settingsRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelAdminSettings), R.drawable.msg_customize, position != parent.membersSectionRow - 1);
                } else if (position == parent.channelBalanceRow) {
                    final TL_stars.StarsAmount stars_balance = BotStarsController.getInstance(parent.getCurrentAccount()).getBotStarsBalance(-parent.chatId);
                    final long ton_balance = BotStarsController.getInstance(parent.getCurrentAccount()).getTONBalance(-parent.chatId);
                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    if (ton_balance > 0) {
                        if (ton_balance / 1_000_000_000.0 > 1000.0) {
                            ssb.append("TON ").append(AndroidUtilities.formatWholeNumber((int) (ton_balance / 1_000_000_000.0), 0));
                        } else {
                            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                            symbols.setDecimalSeparator('.');
                            DecimalFormat formatterTON = new DecimalFormat("#.##", symbols);
                            formatterTON.setMinimumFractionDigits(2);
                            formatterTON.setMaximumFractionDigits(3);
                            formatterTON.setGroupingUsed(false);
                            ssb.append("TON ").append(formatterTON.format(ton_balance / 1_000_000_000.0));
                        }
                    }
                    if (stars_balance.amount > 0) {
                        if (ssb.length() > 0) ssb.append(" ");
                        ssb.append("XTR ").append(formatStarsAmountShort(stars_balance));
                    }
                    textCell.setTextAndValueAndIcon(getString(R.string.ChannelStars), ChannelMonetizationLayout.replaceTON(StarsIntroActivity.replaceStarsWithPlain(ssb, .7f), textCell.getTextView().getPaint()), R.drawable.menu_feature_paid, true);
                } else if (position == parent.botStarsBalanceRow) {
                    final TL_stars.StarsAmount stars_balance = BotStarsController.getInstance(parent.getCurrentAccount()).getBotStarsBalance(parent.userId);
                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    if (stars_balance.amount > 0) {
                        ssb.append("XTR ").append(formatStarsAmountShort(stars_balance));
                    }
                    textCell.setTextAndValueAndIcon(getString(R.string.BotBalanceStars), ChannelMonetizationLayout.replaceTON(StarsIntroActivity.replaceStarsWithPlain(ssb, .7f), textCell.getTextView().getPaint()), R.drawable.menu_premium_main, true);
                } else if (position == parent.botTonBalanceRow) {
                    long ton_balance = BotStarsController.getInstance(parent.getCurrentAccount()).getTONBalance(parent.userId);
                    SpannableStringBuilder ssb = new SpannableStringBuilder();
                    if (ton_balance > 0) {
                        if (ton_balance / 1_000_000_000.0 > 1000.0) {
                            ssb.append("TON ").append(AndroidUtilities.formatWholeNumber((int) (ton_balance / 1_000_000_000.0), 0));
                        } else {
                            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                            symbols.setDecimalSeparator('.');
                            DecimalFormat formatterTON = new DecimalFormat("#.##", symbols);
                            formatterTON.setMinimumFractionDigits(2);
                            formatterTON.setMaximumFractionDigits(3);
                            formatterTON.setGroupingUsed(false);
                            ssb.append("TON ").append(formatterTON.format(ton_balance / 1_000_000_000.0));
                        }
                    }
                    textCell.setTextAndValueAndIcon(getString(R.string.BotBalanceTON), ChannelMonetizationLayout.replaceTON(StarsIntroActivity.replaceStarsWithPlain(ssb, .7f), textCell.getTextView().getPaint()), R.drawable.msg_ton, true);
                } else if (position == parent.blockedUsersRow) {
                    if (parent.chatInfo != null) {
                        textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.ChannelBlacklist), String.format("%d", Math.max(parent.chatInfo.banned_count, parent.chatInfo.kicked_count)), R.drawable.msg_user_remove, position != parent.membersSectionRow - 1);
                    } else {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.ChannelBlacklist), R.drawable.msg_user_remove, position != parent.membersSectionRow - 1);
                    }
                } else if (position == parent.addMemberRow) {
                    textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                    boolean isNextPositionMember = position + 1 >= parent.membersStartRow && position + 1 < parent.membersEndRow;
                    textCell.setTextAndIcon(LocaleController.getString(R.string.AddMember), R.drawable.msg_contact_add, parent.membersSectionRow == -1 || isNextPositionMember);
                } else if (position == parent.sendMessageRow) {
                    textCell.setText(LocaleController.getString(R.string.SendMessageLocation), true);
                } else if (position == parent.addToContactsRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.AddToContacts), R.drawable.msg_contact_add, false);
                    textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                } else if (position == parent.reportReactionRow) {
                    TLRPC.Chat chat = parent.getMessagesController().getChat(-parent.reportReactionFromDialogId);
                    if (chat != null && ChatObject.canBlockUsers(chat)) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.ReportReactionAndBan), R.drawable.msg_block2, false);
                    } else {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.ReportReaction), R.drawable.msg_report, false);
                    }

                    textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedRegular);
                    textCell.setColors(Theme.key_text_RedBold, Theme.key_text_RedRegular);
                } else if (position == parent.reportRow) {
                    textCell.setText(LocaleController.getString(R.string.ReportUserLocation), false);
                    textCell.setColors(-1, Theme.key_text_RedRegular);
                    textCell.setColors(-1, Theme.key_text_RedRegular);
                } else if (position == parent.languageRow) {
                    textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.Language), LocaleController.getCurrentLanguageName(), false, R.drawable.msg2_language, false);
                    textCell.setImageLeft(23);
                } else if (position == parent.notificationRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.NotificationsAndSounds), R.drawable.msg2_notifications, true);
                } else if (position == parent.privacyRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.PrivacySettings), R.drawable.msg2_secret, true);
                } else if (position == parent.dataRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.DataSettings), R.drawable.msg2_data, true);
                } else if (position == parent.chatRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.ChatSettings), R.drawable.msg2_discussion, true);
                } else if (position == parent.filtersRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.Filters), R.drawable.msg2_folder, true);
                } else if (position == parent.stickersRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.StickersName), R.drawable.msg2_sticker, true);
                } else if (position == parent.liteModeRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.PowerUsage), R.drawable.msg2_battery, true);
                } else if (position == parent.questionRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.AskAQuestion), R.drawable.msg2_ask_question, true);
                } else if (position == parent.faqRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.TelegramFAQ), R.drawable.msg2_help, true);
                } else if (position == parent.policyRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.PrivacyPolicy), R.drawable.msg2_policy, false);
                } else if (position == parent.sendLogsRow) {
                    textCell.setText(LocaleController.getString(R.string.DebugSendLogs), true);
                } else if (position == parent.sendLastLogsRow) {
                    textCell.setText(LocaleController.getString(R.string.DebugSendLastLogs), true);
                } else if (position == parent.clearLogsRow) {
                    textCell.setText(LocaleController.getString(R.string.DebugClearLogs), parent.switchBackendRow != -1);
                } else if (position == parent.switchBackendRow) {
                    textCell.setText("Switch Backend", false);
                } else if (position == parent.devicesRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.Devices), R.drawable.msg2_devices, true);
                } else if (position == parent.setAvatarRow) {
                    parent.cellCameraDrawable.setCustomEndFrame(86);
                    parent.cellCameraDrawable.setCurrentFrame(85, false);
                    textCell.setTextAndIcon(LocaleController.getString(R.string.SetProfilePhoto), parent.cellCameraDrawable, false);
                    textCell.setColors(Theme.key_windowBackgroundWhiteBlueIcon, Theme.key_windowBackgroundWhiteBlueButton);
                    textCell.getImageView().setPadding(0, 0, 0, AndroidUtilities.dp(8));
                    textCell.setImageLeft(12);
                    parent.setAvatarCell = textCell;
                } else if (position == parent.addToGroupButtonRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.AddToGroupOrChannel), R.drawable.msg_groups_create, false);
                } else if (position == parent.premiumRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.TelegramPremium), new AnimatedEmojiDrawable.WrapSizeDrawable(PremiumGradient.getInstance().premiumStarMenuDrawable, dp(24), dp(24)), true);
                    textCell.setImageLeft(23);
                } else if (position == parent.starsRow) {
                    StarsController c = StarsController.getInstance(parent.getCurrentAccount());
                    long balance = c.getBalance().amount;
                    textCell.setTextAndValueAndIcon(LocaleController.getString(R.string.MenuTelegramStars), c.balanceAvailable() && balance > 0 ? StarsIntroActivity.formatStarsAmount(c.getBalance(), 0.85f, ' ') : "", new AnimatedEmojiDrawable.WrapSizeDrawable(PremiumGradient.getInstance().goldenStarMenuDrawable, dp(24), dp(24)), true);
                    textCell.setImageLeft(23);
                } else if (position == parent.tonRow) {
                    StarsController c = StarsController.getTonInstance(parent.getCurrentAccount());
                    long balance = c.getBalance().amount;
                    textCell.setTextAndValueAndIcon(getString(R.string.MyTON), c.balanceAvailable() && balance > 0 ? StarsIntroActivity.formatStarsAmount(c.getBalance(), 0.85f, ' ') : "", R.drawable.menu_my_ton, true);
                    textCell.setImageLeft(23);
                } else if (position == parent.businessRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.TelegramBusiness), R.drawable.menu_shop, true);
                    textCell.setImageLeft(23);
                } else if (position == parent.premiumGiftingRow) {
                    textCell.setTextAndIcon(LocaleController.getString(R.string.SendAGift), R.drawable.menu_gift, false);
                    textCell.setImageLeft(23);
                } else if (position == parent.botPermissionLocation) {
                    textCell.setTextAndCheckAndColorfulIcon(LocaleController.getString(R.string.BotProfilePermissionLocation), parent.botLocation != null && parent.botLocation.granted(), R.drawable.filled_access_location, parent.getThemedColor(Theme.key_color_green), parent.botPermissionBiometry != -1);
                } else if (position == parent.botPermissionBiometry) {
                    textCell.setTextAndCheckAndColorfulIcon(LocaleController.getString(R.string.BotProfilePermissionBiometry), parent.botBiometry != null && parent.botBiometry.granted(), R.drawable.filled_access_fingerprint, parent.getThemedColor(Theme.key_color_orange), false);
                } else if (position == parent.botPermissionEmojiStatus) {
                    textCell.setTextAndCheckAndColorfulIcon(LocaleController.getString(R.string.BotProfilePermissionEmojiStatus), parent.getUserInfo() != null && parent.getUserInfo().bot_can_manage_emoji_status, R.drawable.filled_access_sleeping, parent.getThemedColor(Theme.key_color_lightblue), parent.botPermissionLocation != -1 || parent.botPermissionBiometry != -1);
                }
                textCell.valueTextView.setTextColor(parent.dontApplyPeerColor(parent.getThemedColor(Theme.key_windowBackgroundWhiteValueText), false));
                break;
            case VIEW_TYPE_NOTIFICATIONS_CHECK:
                NotificationsCheckCell checkCell = (NotificationsCheckCell) holder.itemView;
                if (position == parent.notificationsRow) {
                    SharedPreferences preferences = MessagesController.getNotificationsSettings(parent.getCurrentAccount());
                    long did;
                    if (parent.getDialogId() != 0) {
                        did = parent.getDialogId();
                    } else if (parent.userId != 0) {
                        did = parent.userId;
                    } else {
                        did = -parent.chatId;
                    }
                    String key = NotificationsController.getSharedPrefKey(did, parent.getTopicId());
                    boolean enabled = false;
                    boolean custom = preferences.getBoolean("custom_" + key, false);
                    boolean hasOverride = preferences.contains("notify2_" + key);
                    int value = preferences.getInt("notify2_" + key, 0);
                    int delta = preferences.getInt("notifyuntil_" + key, 0);
                    String val;
                    if (value == 3 && delta != Integer.MAX_VALUE) {
                        delta -= parent.getConnectionsManager().getCurrentTime();
                        if (delta <= 0) {
                            if (custom) {
                                val = LocaleController.getString(R.string.NotificationsCustom);
                            } else {
                                val = LocaleController.getString(R.string.NotificationsOn);
                            }
                            enabled = true;
                        } else if (delta < 60 * 60) {
                            val = formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Minutes", delta / 60));
                        } else if (delta < 60 * 60 * 24) {
                            val = formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Hours", (int) Math.ceil(delta / 60.0f / 60)));
                        } else if (delta < 60 * 60 * 24 * 365) {
                            val = formatString("WillUnmuteIn", R.string.WillUnmuteIn, LocaleController.formatPluralString("Days", (int) Math.ceil(delta / 60.0f / 60 / 24)));
                        } else {
                            val = null;
                        }
                    } else {
                        if (value == 0) {
                            if (hasOverride) {
                                enabled = true;
                            } else {
                                enabled = parent.getNotificationsController().isGlobalNotificationsEnabled(did, false, false);
                            }
                        } else if (value == 1) {
                            enabled = true;
                        }
                        if (enabled && custom) {
                            val = LocaleController.getString(R.string.NotificationsCustom);
                        } else {
                            val = enabled ? LocaleController.getString(R.string.NotificationsOn) : LocaleController.getString(R.string.NotificationsOff);
                        }
                    }
                    if (val == null) {
                        val = LocaleController.getString(R.string.NotificationsOff);
                    }
                    if (parent.notificationsExceptionTopics != null && !parent.notificationsExceptionTopics.isEmpty()) {
                        val = String.format(Locale.US, LocaleController.getPluralString("NotificationTopicExceptionsDesctription", parent.notificationsExceptionTopics.size()), val, parent.notificationsExceptionTopics.size());
                    }
                    checkCell.setAnimationsEnabled(parent.isFragmentOpened());
                    checkCell.setTextAndValueAndCheck(LocaleController.getString(R.string.Notifications), val, enabled, parent.botAppRow >= 0);
                }
                break;
            case VIEW_TYPE_SHADOW:
                View sectionCell = holder.itemView;
                sectionCell.setTag(position);
                Drawable drawable;
                if (position == parent.infoSectionRow && parent.lastSectionRow == -1 && parent.secretSettingsSectionRow == -1 && parent.sharedMediaRow == -1 && parent.membersSectionRow == -1 || position == parent.secretSettingsSectionRow || position == parent.lastSectionRow || position == parent.membersSectionRow && parent.lastSectionRow == -1 && parent.sharedMediaRow == -1) {
                    sectionCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, parent.getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                } else {
                    sectionCell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, parent.getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                }
                break;
            case VIEW_TYPE_SHADOW_TEXT: {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                cell.setLinkTextRippleColor(null);
                if (position == parent.infoSectionRow) {
                    final long did = parent.getDialogId();
                    TLObject obj = parent.getMessagesController().getUserOrChat(did);
                    TL_bots.botVerification bot_verification = parent.getUserInfo() != null ? parent.getUserInfo().bot_verification : parent.chatInfo != null ? parent.chatInfo.bot_verification : null;
                    if (parent.botAppRow >= 0 || bot_verification != null) {
                        cell.setFixedSize(0);
                        final TLRPC.User user = parent.getMessagesController().getUser(parent.userId);
                        final boolean botOwner = user != null && user.bot && user.bot_can_edit;
                        SpannableStringBuilder sb = new SpannableStringBuilder();

                        if (parent.botAppRow >= 0) {
                            sb.append(AndroidUtilities.replaceSingleTag(getString(botOwner ? R.string.ProfileBotOpenAppInfoOwner : R.string.ProfileBotOpenAppInfo), () -> {
                                Browser.openUrl(mContext, getString(botOwner ? R.string.ProfileBotOpenAppInfoOwnerLink : R.string.ProfileBotOpenAppInfoLink));
                            }));
                            if (bot_verification != null) {
                                sb.append("\n\n\n");
                            }
                        }
                        if (bot_verification != null) {
                            sb.append("x");
                            sb.setSpan(new AnimatedEmojiSpan(bot_verification.icon, cell.getTextView().getPaint().getFontMetricsInt()), sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            sb.append(" ");
                            SpannableString description = new SpannableString(bot_verification.description);
                            try {
                                AndroidUtilities.addLinksSafe(description, Linkify.WEB_URLS, false, false);
                                URLSpan[] spans = description.getSpans(0, description.length(), URLSpan.class);
                                for (int i = 0; i < spans.length; ++i) {
                                    URLSpan span = spans[i];
                                    int start = description.getSpanStart(span);
                                    int end = description.getSpanEnd(span);
                                    final String url = span.getURL();

                                    description.removeSpan(span);
                                    description.setSpan(new URLSpan(url) {
                                        @Override
                                        public void onClick(View widget) {
                                            Browser.openUrl(mContext, url);
                                        }

                                        @Override
                                        public void updateDrawState(@NonNull TextPaint ds) {
                                            ds.setUnderlineText(true);
                                        }
                                    }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                                }
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            sb.append(description);
                        }

                        cell.setLinkTextRippleColor(Theme.multAlpha(parent.getThemedColor(Theme.key_windowBackgroundWhiteGrayText4), 0.2f));
                        cell.setText(sb);
                    } else {
                        cell.setFixedSize(14);
                        cell.setText(null);
                    }
                } else if (position == parent.infoAffiliateRow) {
                    final TLRPC.User botUser = parent.getMessagesController().getUser(parent.userId);
                    if (botUser != null && botUser.bot && botUser.bot_can_edit) {
                        cell.setFixedSize(0);
                        cell.setText(formatString(R.string.ProfileBotAffiliateProgramInfoOwner, UserObject.getUserName(botUser), percents(parent.getUserInfo() != null && parent.getUserInfo().starref_program != null ? parent.getUserInfo().starref_program.commission_permille : 0)));
                    } else {
                        cell.setFixedSize(0);
                        cell.setText(formatString(R.string.ProfileBotAffiliateProgramInfo, UserObject.getUserName(botUser), percents(parent.getUserInfo() != null && parent.getUserInfo().starref_program != null ? parent.getUserInfo().starref_program.commission_permille : 0)));
                    }
                }
                if (position == parent.infoSectionRow && parent.lastSectionRow == -1 && parent.secretSettingsSectionRow == -1 && parent.sharedMediaRow == -1 && parent.membersSectionRow == -1 || position == parent.secretSettingsSectionRow || position == parent.lastSectionRow || position == parent.membersSectionRow && parent.lastSectionRow == -1 && parent.sharedMediaRow == -1) {
                    cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, parent.getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                } else {
                    cell.setBackgroundDrawable(Theme.getThemedDrawable(mContext, R.drawable.greydivider, parent.getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                }
                break;
            }
            case VIEW_TYPE_COLORFUL_TEXT: {
                AffiliateProgramFragment.ColorfulTextCell cell = (AffiliateProgramFragment.ColorfulTextCell) holder.itemView;
                cell.set(parent.getThemedColor(Theme.key_color_green), R.drawable.filled_affiliate, getString(R.string.ProfileBotAffiliateProgram), null);
                cell.setPercent(parent.getUserInfo() != null && parent.getUserInfo().starref_program != null ? percents(parent.getUserInfo().starref_program.commission_permille) : null);
                break;
            }
            case VIEW_TYPE_USER:
                UserCell userCell = (UserCell) holder.itemView;
                TLRPC.ChatParticipant part;
                try {
                    if (!parent.visibleSortedUsers.isEmpty()) {
                        part = parent.visibleChatParticipants.get(parent.visibleSortedUsers.get(position - parent.membersStartRow));
                    } else {
                        part = parent.visibleChatParticipants.get(position - parent.membersStartRow);
                    }
                } catch (Exception e) {
                    part = null;
                    FileLog.e(e);
                }
                if (part != null) {
                    String role;
                    if (part instanceof TLRPC.TL_chatChannelParticipant) {
                        TLRPC.ChannelParticipant channelParticipant = ((TLRPC.TL_chatChannelParticipant) part).channelParticipant;
                        if (!TextUtils.isEmpty(channelParticipant.rank)) {
                            role = channelParticipant.rank;
                        } else {
                            if (channelParticipant instanceof TLRPC.TL_channelParticipantCreator) {
                                role = LocaleController.getString(R.string.ChannelCreator);
                            } else if (channelParticipant instanceof TLRPC.TL_channelParticipantAdmin) {
                                role = LocaleController.getString(R.string.ChannelAdmin);
                            } else {
                                role = null;
                            }
                        }
                    } else {
                        if (part instanceof TLRPC.TL_chatParticipantCreator) {
                            role = LocaleController.getString(R.string.ChannelCreator);
                        } else if (part instanceof TLRPC.TL_chatParticipantAdmin) {
                            role = getString(R.string.ChannelAdmin);
                        } else {
                            role = null;
                        }
                    }
                    userCell.setAdminRole(role);
                    userCell.setData(parent.getMessagesController().getUser(part.user_id), null, null, 0, position != parent.membersEndRow - 1);
                }
                break;
            case VIEW_TYPE_BOTTOM_PADDING:
                holder.itemView.requestLayout();
                break;
            case VIEW_TYPE_SUGGESTION:
                SettingsSuggestionCell suggestionCell = (SettingsSuggestionCell) holder.itemView;
                if (position == parent.passwordSuggestionRow) {
                    suggestionCell.setType(SettingsSuggestionCell.TYPE_PASSWORD);
                } else if (position == parent.phoneSuggestionRow) {
                    suggestionCell.setType(SettingsSuggestionCell.TYPE_PHONE);
                } else if (position == parent.graceSuggestionRow) {
                    suggestionCell.setType(SettingsSuggestionCell.TYPE_GRACE);
                }
                break;
            case VIEW_TYPE_ADDTOGROUP_INFO:
                TextInfoPrivacyCell addToGroupInfo = (TextInfoPrivacyCell) holder.itemView;
                addToGroupInfo.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, parent.getThemedColor(Theme.key_windowBackgroundGrayShadow)));
                addToGroupInfo.setText(LocaleController.getString(R.string.BotAddToGroupOrChannelInfo));
                break;
            case VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE:
                TextCheckCell textCheckCell = (TextCheckCell) holder.itemView;
                textCheckCell.setTextAndCheck(LocaleController.getString(R.string.Notifications), !parent.getMessagesController().isDialogMuted(parent.getDialogId(), parent.getTopicId()), false);
                break;
            case VIEW_TYPE_LOCATION:
                ((ProfileLocationCell) holder.itemView).set(parent.getUserInfo() != null ? parent.getUserInfo().business_location : null, parent.notificationsDividerRow < 0 && !parent.myProfile);
                break;
            case VIEW_TYPE_HOURS:
                ProfileHoursCell hoursCell = (ProfileHoursCell) holder.itemView;
                hoursCell.setOnTimezoneSwitchClick(view -> {
                    parent.hoursShownMine = !parent.hoursShownMine;
                    if (!parent.hoursExpanded) {
                        parent.hoursExpanded = true;
                    }
                    parent.saveScrollPosition();
                    view.requestLayout();
                    parent.listAdapter.notifyItemChanged(parent.bizHoursRow);
                    if (parent.savedScrollPosition >= 0) {
                        parent.layoutManager.scrollToPositionWithOffset(parent.savedScrollPosition, parent.savedScrollOffset - parent.getListView().getPaddingTop());
                    }
                });
                hoursCell.set(parent.getUserInfo() != null ? parent.getUserInfo().business_work_hours : null, parent.hoursExpanded, parent.hoursShownMine, parent.notificationsDividerRow < 0 && !parent.myProfile || parent.bizLocationRow >= 0);
                break;
            case VIEW_TYPE_CHANNEL:
                ((ProfileChannelCell) holder.itemView).set(
                        parent.getMessagesController().getChat(parent.getUserInfo().personal_channel_id),
                        parent.profileChannelMessageFetcher != null ? parent.profileChannelMessageFetcher.messageObject : null
                );
                break;
            case VIEW_TYPE_BOT_APP:

                break;
        }
    }

    private CharSequence alsoUsernamesString(String originalUsername, ArrayList<TLRPC.TL_username> alsoUsernames, CharSequence fallback) {
        if (alsoUsernames == null) {
            return fallback;
        }
        alsoUsernames = new ArrayList<>(alsoUsernames);
        for (int i = 0; i < alsoUsernames.size(); ++i) {
            if (
                    !alsoUsernames.get(i).active ||
                            originalUsername != null && originalUsername.equals(alsoUsernames.get(i).username)
            ) {
                alsoUsernames.remove(i--);
            }
        }
        if (alsoUsernames.size() > 0) {
            SpannableStringBuilder usernames = new SpannableStringBuilder();
            for (int i = 0; i < alsoUsernames.size(); ++i) {
                TLRPC.TL_username usernameObj = alsoUsernames.get(i);
                final String usernameRaw = usernameObj.username;
                SpannableString username = new SpannableString("@" + usernameRaw);
                username.setSpan(makeUsernameLinkSpan(usernameObj), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                username.setSpan(new ForegroundColorSpan(parent.dontApplyPeerColor(parent.getThemedColor(Theme.key_chat_messageLinkIn), false)), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                usernames.append(username);
                if (i < alsoUsernames.size() - 1) {
                    usernames.append(", ");
                }
            }
            String string = getString(R.string.UsernameAlso);
            SpannableStringBuilder finalString = new SpannableStringBuilder(string);
            final String toFind = "%1$s";
            int index = string.indexOf(toFind);
            if (index >= 0) {
                finalString.replace(index, index + toFind.length(), usernames);
            }
            return finalString;
        } else {
            return fallback;
        }
    }

    public ClickableSpan makeUsernameLinkSpan(TLRPC.TL_username usernameObj) {
        ClickableSpan span = usernameSpans.get(usernameObj);
        if (span != null) return span;

        final String usernameRaw = usernameObj.username;
        span = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View view) {
                if (!usernameObj.editable) {
                    if (parent.loadingSpan == this) return;
                    parent.setLoadingSpan(this);
                    TL_fragment.TL_getCollectibleInfo req = new TL_fragment.TL_getCollectibleInfo();
                    TL_fragment.TL_inputCollectibleUsername input = new TL_fragment.TL_inputCollectibleUsername();
                    input.username = usernameObj.username;
                    req.collectible = input;
                    int reqId = parent.getConnectionsManager().sendRequest(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
                        parent.setLoadingSpan(null);
                        if (res instanceof TL_fragment.TL_collectibleInfo) {
                            TLObject obj;
                            if (parent.userId != 0) {
                                obj = parent.getMessagesController().getUser(parent.userId);
                            } else {
                                obj = parent.getMessagesController().getChat(parent.chatId);
                            }
                            if (mContext == null) {
                                return;
                            }
                            FragmentUsernameBottomSheet.open(mContext, FragmentUsernameBottomSheet.TYPE_USERNAME, usernameObj.username, obj, (TL_fragment.TL_collectibleInfo) res, parent.getResourceProvider());
                        } else {
                            BulletinFactory.showError(err);
                        }
                    }));
                    parent.getConnectionsManager().bindRequestToGuid(reqId, parent.getClassGuid());
                } else {
                    parent.setLoadingSpan(null);
                    String urlFinal = parent.getMessagesController().linkPrefix + "/" + usernameRaw;
                    if (parent.getCurrentChat() == null || !parent.getCurrentChat().noforwards) {
                        AndroidUtilities.addToClipboard(urlFinal);
                        parent.getUndoView().showWithAction(0, UndoView.ACTION_USERNAME_COPIED, null);
                    }
                }
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setUnderlineText(false);
                ds.setColor(ds.linkColor);
            }
        };
        usernameSpans.put(usernameObj, span);
        return span;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder.getAdapterPosition() == parent.setAvatarRow) {
            parent.setAvatarCell = null;
        }
    }

    @Override
    public boolean isEnabled(RecyclerView.ViewHolder holder) {
        if (parent.notificationRow != -1) {
            int position = holder.getAdapterPosition();
            return position == parent.notificationRow || position == parent.numberRow || position == parent.privacyRow ||
                    position == parent.languageRow || position == parent.setUsernameRow || position == parent.bioRow ||
                    position == parent.versionRow || position == parent.dataRow || position == parent.chatRow ||
                    position == parent.questionRow || position == parent.devicesRow || position == parent.filtersRow || position == parent.stickersRow ||
                    position == parent.faqRow || position == parent.policyRow || position == parent.sendLogsRow || position == parent.sendLastLogsRow ||
                    position == parent.clearLogsRow || position == parent.switchBackendRow || position == parent.setAvatarRow ||
                    position == parent.addToGroupButtonRow || position == parent.premiumRow || position == parent.premiumGiftingRow ||
                    position == parent.businessRow || position == parent.liteModeRow || position == parent.birthdayRow || position == parent.channelRow ||
                    position == parent.starsRow || position == parent.tonRow;
        }
        if (holder.itemView instanceof UserCell) {
            UserCell userCell = (UserCell) holder.itemView;
            Object object = userCell.getCurrentObject();
            if (object instanceof TLRPC.User) {
                TLRPC.User user = (TLRPC.User) object;
                if (UserObject.isUserSelf(user)) {
                    return false;
                }
            }
        }
        int type = holder.getItemViewType();
        return type != VIEW_TYPE_HEADER && type != VIEW_TYPE_DIVIDER && type != VIEW_TYPE_SHADOW &&
                type != VIEW_TYPE_EMPTY && type != VIEW_TYPE_BOTTOM_PADDING && type != VIEW_TYPE_SHARED_MEDIA &&
                type != 9 && type != 10 && type != VIEW_TYPE_BOT_APP; // These are legacy ones, left for compatibility
    }

    @Override
    public int getItemCount() {
        return parent.rowCount;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == parent.infoHeaderRow || position == parent.membersHeaderRow || position == parent.settingsSectionRow2 ||
                position == parent.numberSectionRow || position == parent.helpHeaderRow || position == parent.debugHeaderRow || position == parent.botPermissionsHeader) {
            return VIEW_TYPE_HEADER;
        } else if (position == parent.phoneRow || position == parent.locationRow || position == parent.numberRow || position == parent.birthdayRow) {
            return VIEW_TYPE_TEXT_DETAIL;
        } else if (position == parent.usernameRow || position == parent.setUsernameRow) {
            return VIEW_TYPE_TEXT_DETAIL_MULTILINE;
        } else if (position == parent.userInfoRow || position == parent.channelInfoRow || position == parent.bioRow) {
            return VIEW_TYPE_ABOUT_LINK;
        } else if (position == parent.settingsTimerRow || position == parent.settingsKeyRow || position == parent.reportRow || position == parent.reportReactionRow ||
                position == parent.subscribersRow || position == parent.subscribersRequestsRow || position == parent.administratorsRow || position == parent.settingsRow || position == parent.blockedUsersRow ||
                position == parent.addMemberRow || position == parent.joinRow || position == parent.unblockRow ||
                position == parent.sendMessageRow || position == parent.notificationRow || position == parent.privacyRow ||
                position == parent.languageRow || position == parent.dataRow || position == parent.chatRow ||
                position == parent.questionRow || position == parent.devicesRow || position == parent.filtersRow || position == parent.stickersRow ||
                position == parent.faqRow || position == parent.policyRow || position == parent.sendLogsRow || position == parent.sendLastLogsRow ||
                position == parent.clearLogsRow || position == parent.switchBackendRow || position == parent.setAvatarRow || position == parent.addToGroupButtonRow ||
                position == parent.addToContactsRow || position == parent.liteModeRow || position == parent.premiumGiftingRow || position == parent.businessRow || position == parent.botStarsBalanceRow || position == parent.botTonBalanceRow || position == parent.channelBalanceRow || position == parent.botPermissionLocation || position == parent.botPermissionBiometry || position == parent.botPermissionEmojiStatus) {
            return VIEW_TYPE_TEXT;
        } else if (position == parent.notificationsDividerRow) {
            return VIEW_TYPE_DIVIDER;
        } else if (position == parent.notificationsRow) {
            return VIEW_TYPE_NOTIFICATIONS_CHECK;
        } else if (position == parent.notificationsSimpleRow) {
            return VIEW_TYPE_NOTIFICATIONS_CHECK_SIMPLE;
        } else if (position == parent.lastSectionRow || position == parent.membersSectionRow ||
                position == parent.secretSettingsSectionRow || position == parent.settingsSectionRow || position == parent.devicesSectionRow ||
                position == parent.helpSectionCell || position == parent.setAvatarSectionRow || position == parent.passwordSuggestionSectionRow ||
                position == parent.phoneSuggestionSectionRow || position == parent.premiumSectionsRow || position == parent.reportDividerRow ||
                position == parent.channelDividerRow || position == parent.graceSuggestionSectionRow || position == parent.balanceDividerRow ||
                position == parent.botPermissionsDivider || position == parent.channelBalanceSectionRow
        ) {
            return VIEW_TYPE_SHADOW;
        } else if (position >= parent.membersStartRow && position < parent.membersEndRow) {
            return VIEW_TYPE_USER;
        } else if (position == parent.emptyRow) {
            return VIEW_TYPE_EMPTY;
        } else if (position == parent.bottomPaddingRow) {
            return VIEW_TYPE_BOTTOM_PADDING;
        } else if (position == parent.sharedMediaRow) {
            return VIEW_TYPE_SHARED_MEDIA;
        } else if (position == parent.versionRow) {
            return VIEW_TYPE_VERSION;
        } else if (position == parent.passwordSuggestionRow || position == parent.phoneSuggestionRow || position == parent.graceSuggestionRow) {
            return VIEW_TYPE_SUGGESTION;
        } else if (position == parent.addToGroupInfoRow) {
            return VIEW_TYPE_ADDTOGROUP_INFO;
        } else if (position == parent.premiumRow) {
            return VIEW_TYPE_PREMIUM_TEXT_CELL;
        } else if (position == parent.starsRow) {
            return VIEW_TYPE_STARS_TEXT_CELL;
        } else if (position == parent.bizLocationRow) {
            return VIEW_TYPE_LOCATION;
        } else if (position == parent.bizHoursRow) {
            return VIEW_TYPE_HOURS;
        } else if (position == parent.channelRow) {
            return VIEW_TYPE_CHANNEL;
        } else if (position == parent.botAppRow) {
            return VIEW_TYPE_BOT_APP;
        } else if (position == parent.infoSectionRow || position == parent.infoAffiliateRow) {
            return VIEW_TYPE_SHADOW_TEXT;
        } else if (position == parent.affiliateRow) {
            return VIEW_TYPE_COLORFUL_TEXT;
        }
        return 0;
    }
}