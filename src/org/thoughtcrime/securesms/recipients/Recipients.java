/**
 * Copyright (C) 2011 Whisper Systems
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
package org.thoughtcrime.securesms.recipients;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Patterns;

import org.thoughtcrime.securesms.crypto.KeyUtil;
import org.thoughtcrime.securesms.recipients.Recipient.RecipientModifiedListener;
import org.thoughtcrime.securesms.util.NumberUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Recipients implements Parcelable {

  public static final Parcelable.Creator<Recipients> CREATOR = new Parcelable.Creator<Recipients>() {
    public Recipients createFromParcel(Parcel in) {
      return new Recipients(in);
    }

    public Recipients[] newArray(int size) {
      return new Recipients[size];
    }
  };


  private List<Recipient> recipients;

  public Recipients(List<Recipient> recipients) {
    this.recipients = recipients;
  }

  public Recipients(final Recipient recipient) {
    this.recipients = new LinkedList<Recipient>() {{
      add(recipient);
    }};
  }

  public Recipients(Parcel in) {
    this.recipients = new ArrayList<Recipient>();
    in.readTypedList(recipients, Recipient.CREATOR);
  }

  public void append(Recipients recipients) {
    this.recipients.addAll(recipients.getRecipientsList());
  }

//  public Recipients truncateToSingleRecipient() {
//    assert(!this.recipients.isEmpty());
//    this.recipients = this.recipients.subList(0, 1);
//    return this;
//  }

  public void addListener(RecipientModifiedListener listener) {
    for (Recipient recipient : recipients) {
      recipient.addListener(listener);
    }
  }

  public void removeListener(RecipientModifiedListener listener) {
    for (Recipient recipient : recipients) {
      recipient.removeListener(listener);
    }
  }

  public boolean isEmailRecipient() {
    for (Recipient recipient : recipients) {
      if (NumberUtil.isValidEmail(recipient.getNumber()))
        return true;
    }

    return false;
  }

//  public Recipients getSecureSessionRecipients(Context context) {
//    List<Recipient> secureRecipients = new LinkedList<Recipient>();
//
//    for (Recipient recipient : recipients) {
//      if (KeyUtil.isSessionFor(context, recipient)) {
//        secureRecipients.add(recipient);
//      }
//    }
//
//    return new Recipients(secureRecipients);
//  }
//
//  public Recipients getInsecureSessionRecipients(Context context) {
//    List<Recipient> insecureRecipients = new LinkedList<Recipient>();
//
//    for (Recipient recipient : recipients) {
//      if (!KeyUtil.isSessionFor(context, recipient)) {
//        insecureRecipients.add(recipient);
//      }
//    }
//
//    return new Recipients(insecureRecipients);
//  }

  public boolean isEmpty() {
    return this.recipients.isEmpty();
  }

  public boolean isSingleRecipient() {
    return this.recipients.size() == 1;
  }

  public Recipient getPrimaryRecipient() {
    if (!isEmpty())
      return this.recipients.get(0);
    else
      return null;
  }

  public List<Recipient> getRecipientsList() {
    return this.recipients;
  }

  public String[] toNumberStringArray(boolean scrub) {
    String[] recipientsArray     = new String[recipients.size()];
    Iterator<Recipient> iterator = recipients.iterator();
    int i                        = 0;

    while (iterator.hasNext()) {
      String number = iterator.next().getNumber();

      if (scrub && number != null && !Patterns.EMAIL_ADDRESS.matcher(number).matches()) {
        number = number.replaceAll("[^0-9+]", "");
      }

      recipientsArray[i++] = number;
    }

    return recipientsArray;
  }

  public String toShortString() {
    String fromString = "";

    for (int i=0;i<recipients.size();i++) {
      fromString += recipients.get(i).toShortString();

      if (i != recipients.size() -1 )
        fromString += ", ";
    }

    return fromString;
  }

  public int describeContents() {
    return 0;
  }

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeTypedList(recipients);
  }
}
