package org.telegram.ui.Profile;


import android.util.SparseIntArray;

import androidx.recyclerview.widget.DiffUtil;

import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

class DiffCallback extends DiffUtil.Callback {

    private final ProfileActivity parent;
    int oldRowCount;

    SparseIntArray oldPositionToItem = new SparseIntArray();
    SparseIntArray newPositionToItem = new SparseIntArray();
    ArrayList<TLRPC.ChatParticipant> oldChatParticipant = new ArrayList<>();
    ArrayList<Integer> oldChatParticipantSorted = new ArrayList<>();
    int oldMembersStartRow;
    int oldMembersEndRow;

    DiffCallback(ProfileActivity parent) {
        this.parent = parent;
    }

    @Override
    public int getOldListSize() {
        return oldRowCount;
    }

    @Override
    public int getNewListSize() {
        return parent.rowCount;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        if (newItemPosition >= parent.membersStartRow && newItemPosition < parent.membersEndRow) {
            if (oldItemPosition >= oldMembersStartRow && oldItemPosition < oldMembersEndRow) {
                TLRPC.ChatParticipant oldItem;
                TLRPC.ChatParticipant newItem;
                if (!oldChatParticipantSorted.isEmpty()) {
                    oldItem = oldChatParticipant.get(oldChatParticipantSorted.get(oldItemPosition - oldMembersStartRow));
                } else {
                    oldItem = oldChatParticipant.get(oldItemPosition - oldMembersStartRow);
                }

                if (!parent.sortedUsers.isEmpty()) {
                    newItem = parent.visibleChatParticipants.get(parent.visibleSortedUsers.get(newItemPosition - parent.membersStartRow));
                } else {
                    newItem = parent.visibleChatParticipants.get(newItemPosition - parent.membersStartRow);
                }
                return oldItem.user_id == newItem.user_id;
            }
        }
        int oldIndex = oldPositionToItem.get(oldItemPosition, -1);
        int newIndex = newPositionToItem.get(newItemPosition, -1);
        return oldIndex == newIndex && oldIndex >= 0;
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return areItemsTheSame(oldItemPosition, newItemPosition);
    }

    public void fillPositions(SparseIntArray sparseIntArray) {
        sparseIntArray.clear();
        int pointer = 0;
        put(++pointer, parent.setAvatarRow, sparseIntArray);
        put(++pointer, parent.setAvatarSectionRow, sparseIntArray);
        put(++pointer, parent.numberSectionRow, sparseIntArray);
        put(++pointer, parent.numberRow, sparseIntArray);
        put(++pointer, parent.setUsernameRow, sparseIntArray);
        put(++pointer, parent.bioRow, sparseIntArray);
        put(++pointer, parent.phoneSuggestionRow, sparseIntArray);
        put(++pointer, parent.phoneSuggestionSectionRow, sparseIntArray);
        put(++pointer, parent.passwordSuggestionRow, sparseIntArray);
        put(++pointer, parent.passwordSuggestionSectionRow, sparseIntArray);
        put(++pointer, parent.graceSuggestionRow, sparseIntArray);
        put(++pointer, parent.graceSuggestionSectionRow, sparseIntArray);
        put(++pointer, parent.settingsSectionRow, sparseIntArray);
        put(++pointer, parent.settingsSectionRow2, sparseIntArray);
        put(++pointer, parent.notificationRow, sparseIntArray);
        put(++pointer, parent.languageRow, sparseIntArray);
        put(++pointer, parent.premiumRow, sparseIntArray);
        put(++pointer, parent.starsRow, sparseIntArray);
        put(++pointer, parent.businessRow, sparseIntArray);
        put(++pointer, parent.premiumSectionsRow, sparseIntArray);
        put(++pointer, parent.premiumGiftingRow, sparseIntArray);
        put(++pointer, parent.privacyRow, sparseIntArray);
        put(++pointer, parent.dataRow, sparseIntArray);
        put(++pointer, parent.liteModeRow, sparseIntArray);
        put(++pointer, parent.chatRow, sparseIntArray);
        put(++pointer, parent.filtersRow, sparseIntArray);
        put(++pointer, parent.stickersRow, sparseIntArray);
        put(++pointer, parent.devicesRow, sparseIntArray);
        put(++pointer, parent.devicesSectionRow, sparseIntArray);
        put(++pointer, parent.helpHeaderRow, sparseIntArray);
        put(++pointer, parent.questionRow, sparseIntArray);
        put(++pointer, parent.faqRow, sparseIntArray);
        put(++pointer, parent.policyRow, sparseIntArray);
        put(++pointer, parent.helpSectionCell, sparseIntArray);
        put(++pointer, parent.debugHeaderRow, sparseIntArray);
        put(++pointer, parent.sendLogsRow, sparseIntArray);
        put(++pointer, parent.sendLastLogsRow, sparseIntArray);
        put(++pointer, parent.clearLogsRow, sparseIntArray);
        put(++pointer, parent.switchBackendRow, sparseIntArray);
        put(++pointer, parent.versionRow, sparseIntArray);
        put(++pointer, parent.emptyRow, sparseIntArray);
        put(++pointer, parent.bottomPaddingRow, sparseIntArray);
        put(++pointer, parent.infoHeaderRow, sparseIntArray);
        put(++pointer, parent.phoneRow, sparseIntArray);
        put(++pointer, parent.locationRow, sparseIntArray);
        put(++pointer, parent.userInfoRow, sparseIntArray);
        put(++pointer, parent.channelInfoRow, sparseIntArray);
        put(++pointer, parent.usernameRow, sparseIntArray);
        put(++pointer, parent.notificationsDividerRow, sparseIntArray);
        put(++pointer, parent.reportDividerRow, sparseIntArray);
        put(++pointer, parent.notificationsRow, sparseIntArray);
        put(++pointer, parent.infoSectionRow, sparseIntArray);
        put(++pointer, parent.affiliateRow, sparseIntArray);
        put(++pointer, parent.infoAffiliateRow, sparseIntArray);
        put(++pointer, parent.sendMessageRow, sparseIntArray);
        put(++pointer, parent.reportRow, sparseIntArray);
        put(++pointer, parent.reportReactionRow, sparseIntArray);
        put(++pointer, parent.addToContactsRow, sparseIntArray);
        put(++pointer, parent.settingsTimerRow, sparseIntArray);
        put(++pointer, parent.settingsKeyRow, sparseIntArray);
        put(++pointer, parent.secretSettingsSectionRow, sparseIntArray);
        put(++pointer, parent.membersHeaderRow, sparseIntArray);
        put(++pointer, parent.addMemberRow, sparseIntArray);
        put(++pointer, parent.subscribersRow, sparseIntArray);
        put(++pointer, parent.subscribersRequestsRow, sparseIntArray);
        put(++pointer, parent.administratorsRow, sparseIntArray);
        put(++pointer, parent.settingsRow, sparseIntArray);
        put(++pointer, parent.blockedUsersRow, sparseIntArray);
        put(++pointer, parent.membersSectionRow, sparseIntArray);
        put(++pointer, parent.channelBalanceSectionRow, sparseIntArray);
        put(++pointer, parent.sharedMediaRow, sparseIntArray);
        put(++pointer, parent.unblockRow, sparseIntArray);
        put(++pointer, parent.addToGroupButtonRow, sparseIntArray);
        put(++pointer, parent.addToGroupInfoRow, sparseIntArray);
        put(++pointer, parent.joinRow, sparseIntArray);
        put(++pointer, parent.lastSectionRow, sparseIntArray);
        put(++pointer, parent.notificationsSimpleRow, sparseIntArray);
        put(++pointer, parent.bizHoursRow, sparseIntArray);
        put(++pointer, parent.bizLocationRow, sparseIntArray);
        put(++pointer, parent.birthdayRow, sparseIntArray);
        put(++pointer, parent.channelRow, sparseIntArray);
        put(++pointer, parent.botStarsBalanceRow, sparseIntArray);
        put(++pointer, parent.botTonBalanceRow, sparseIntArray);
        put(++pointer, parent.channelBalanceRow, sparseIntArray);
        put(++pointer, parent.balanceDividerRow, sparseIntArray);
        put(++pointer, parent.botAppRow, sparseIntArray);
        put(++pointer, parent.botPermissionsHeader, sparseIntArray);
        put(++pointer, parent.botPermissionLocation, sparseIntArray);
        put(++pointer, parent.botPermissionEmojiStatus, sparseIntArray);
        put(++pointer, parent.botPermissionBiometry, sparseIntArray);
        put(++pointer, parent.botPermissionsDivider, sparseIntArray);
        put(++pointer, parent.channelDividerRow, sparseIntArray);
    }

    private void put(int id, int position, SparseIntArray sparseIntArray) {
        if (position >= 0) {
            sparseIntArray.put(position, id);
        }
    }
}
