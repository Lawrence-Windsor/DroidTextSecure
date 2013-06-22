/**
 * Copyright (C) 2012 Moxie Marlinspike
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.thoughtcrime.securesms.database.model;

import android.content.Context;
import android.text.SpannableString;

import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.SmsDatabase;
import org.thoughtcrime.securesms.protocol.Tag;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.Recipients;

/**
 * The message record model which represents standard SMS messages.
 *
 * @author Moxie Marlinspike
 *
 */

public class SmsMessageRecord extends MessageRecord {

  public SmsMessageRecord(Context context, long id,
                          Body body, Recipients recipients,
                          Recipient individualRecipient,
                          long dateSent, long dateReceived,
                          long type, long threadId,
                          int status)
  {
    super(context, id, body, recipients, individualRecipient, dateSent, dateReceived,
          threadId, getGenericDeliveryStatus(status), type);
  }

  public long getType() {
    return type;
  }

  @Override
  public SpannableString getDisplayBody() {
    if (isProcessedKeyExchange()) {
      return emphasisAdded(context.getString(R.string.ConversationItem_received_and_processed_key_exchange_message));
    } else if (isStaleKeyExchange()) {
      return emphasisAdded(context.getString(R.string.ConversationItem_error_received_stale_key_exchange_message));
    } else if (isKeyExchange() && isOutgoing()) {
      return emphasisAdded(context.getString(R.string.ConversationListAdapter_key_exchange_message));
    } else if (isKeyExchange() && !isOutgoing()) {
      return emphasisAdded(context.getString(R.string.ConversationItem_received_key_exchange_message_click_to_process));
    } else if (SmsDatabase.Types.isFailedDecryptType(type)) {
      return emphasisAdded(context.getString(R.string.MessageDisplayHelper_bad_encrypted_message));
    } else if (SmsDatabase.Types.isDecryptInProgressType(type)) {
      return emphasisAdded(context.getString(R.string.MessageDisplayHelper_decrypting_please_wait));
    } else if (SmsDatabase.Types.isNoRemoteSessionType(type)) {
      return emphasisAdded(context.getString(R.string.MessageDisplayHelper_message_encrypted_for_non_existing_session));
    } else if (!getBody().isPlaintext()) {
      return emphasisAdded(context.getString(R.string.MessageNotifier_encrypted_message));
    } else if (isOutgoing() && Tag.isTagged(getBody().getBody())) {
      return new SpannableString(Tag.stripTag(getBody().getBody()));
    } else {
      return super.getDisplayBody();
    }
  }

  @Override
  public boolean isMms() {
    return false;
  }

  private static int getGenericDeliveryStatus(int status) {
    if (status == SmsDatabase.Status.STATUS_NONE) {
      return MessageRecord.DELIVERY_STATUS_NONE;
    } else if (status >= SmsDatabase.Status.STATUS_FAILED) {
      return MessageRecord.DELIVERY_STATUS_FAILED;
    } else if (status >= SmsDatabase.Status.STATUS_PENDING) {
      return MessageRecord.DELIVERY_STATUS_PENDING;
    } else {
      return MessageRecord.DELIVERY_STATUS_RECEIVED;
    }
  }
}
