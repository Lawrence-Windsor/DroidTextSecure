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
package org.thoughtcrime.securesms;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.MenuItem;
import org.thoughtcrime.securesms.crypto.IdentityKey;
import org.thoughtcrime.securesms.crypto.IdentityKeyUtil;
import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.database.keys.SessionRecord;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.MemoryCleaner;

/**
 * Activity for verifying identity keys.
 *
 * @author Moxie Marlinspike
 */
public class VerifyIdentityActivity extends KeyScanningActivity {

  private Recipient recipient;
  private MasterSecret masterSecret;

  private TextView localIdentityFingerprint;
  private TextView remoteIdentityFingerprint;

  @Override
  public void onCreate(Bundle state) {
    super.onCreate(state);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    setContentView(R.layout.verify_identity_activity);

    initializeResources();
    initializeFingerprints();
  }

  @Override
  protected void onDestroy() {
    MemoryCleaner.clean(masterSecret);
    super.onDestroy();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home: finish(); return true;
    }

    return false;
  }

  private void initializeLocalIdentityKey() {
    if (!IdentityKeyUtil.hasIdentityKey(this)) {
      localIdentityFingerprint.setText(R.string.VerifyIdentityActivity_you_do_not_have_an_identity_key);
      return;
    }

    localIdentityFingerprint.setText(IdentityKeyUtil.getFingerprint(this));
  }

  private void initializeRemoteIdentityKey() {
    SessionRecord sessionRecord = new SessionRecord(this, masterSecret, recipient);
    IdentityKey identityKey     = sessionRecord.getIdentityKey();

    if (identityKey == null) {
      remoteIdentityFingerprint.setText(R.string.VerifyIdentityActivity_recipient_has_no_identity_key);
    } else {
      remoteIdentityFingerprint.setText(identityKey.getFingerprint());
    }
  }

  private void initializeFingerprints() {
    initializeLocalIdentityKey();
    initializeRemoteIdentityKey();
  }

  private void initializeResources() {
    localIdentityFingerprint  = (TextView)findViewById(R.id.you_read);
    remoteIdentityFingerprint = (TextView)findViewById(R.id.friend_reads);
    recipient                 = (Recipient)this.getIntent().getParcelableExtra("recipient");
    masterSecret              = (MasterSecret)this.getIntent().getParcelableExtra("master_secret");
  }

  @Override
  protected void initiateDisplay() {
    if (!IdentityKeyUtil.hasIdentityKey(this)) {
      Toast.makeText(this,
                     R.string.VerifyIdentityActivity_you_don_t_have_an_identity_key_exclamation,
                     Toast.LENGTH_LONG).show();
      return;
    }

    super.initiateDisplay();
  }

  @Override
  protected void initiateScan() {
    SessionRecord sessionRecord = new SessionRecord(this, masterSecret, recipient);
    IdentityKey identityKey     = sessionRecord.getIdentityKey();

    if (identityKey == null) {
      Toast.makeText(this, R.string.VerifyIdentityActivity_recipient_has_no_identity_key_exclamation,
                     Toast.LENGTH_LONG).show();
    } else {
      super.initiateScan();
    }
  }

  @Override
  protected String getScanString() {
    return getString(R.string.VerifyIdentityActivity_scan_their_key_to_compare);
  }

  @Override
  protected String getDisplayString() {
    return getString(R.string.VerifyIdentityActivity_get_my_key_scanned);
  }

  @Override
  protected IdentityKey getIdentityKeyToCompare() {
    SessionRecord sessionRecord = new SessionRecord(this, masterSecret, recipient);
    return sessionRecord.getIdentityKey();
  }

  @Override
  protected IdentityKey getIdentityKeyToDisplay() {
    return IdentityKeyUtil.getIdentityKey(this);
  }

  @Override
  protected String getNotVerifiedMessage() {
    return getString(R.string.VerifyIdentityActivity_warning_the_scanned_key_does_not_match_please_check_the_fingerprint_text_carefully);
  }

  @Override
  protected String getNotVerifiedTitle() {
    return getString(R.string.VerifyIdentityActivity_not_verified_exclamation);
  }

  @Override
  protected String getVerifiedMessage() {
    return getString(R.string.VerifyIdentityActivity_their_key_is_correct_it_is_also_necessary_to_verify_your_key_with_them_as_well);
  }

  @Override
  protected String getVerifiedTitle() {
    return getString(R.string.VerifyIdentityActivity_verified_exclamation);
  }
}
