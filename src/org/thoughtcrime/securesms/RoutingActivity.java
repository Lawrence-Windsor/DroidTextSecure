package org.thoughtcrime.securesms;

import android.content.Intent;
import android.net.Uri;

import org.thoughtcrime.securesms.crypto.MasterSecret;
import org.thoughtcrime.securesms.crypto.MasterSecretUtil;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.recipients.RecipientFactory;
import org.thoughtcrime.securesms.recipients.RecipientFormattingException;
import org.thoughtcrime.securesms.recipients.Recipients;
import org.thoughtcrime.securesms.service.ApplicationMigrationService;

public class RoutingActivity extends PassphraseRequiredSherlockActivity {

  private static final int STATE_CREATE_PASSPHRASE    = 1;
  private static final int STATE_PROMPT_PASSPHRASE    = 2;
  private static final int STATE_IMPORT_DATABASE      = 3;
  private static final int STATE_CONVERSATION_OR_LIST = 4;
  private static final int STATE_UPGRADE_DATABASE     = 5;

  private MasterSecret masterSecret = null;
  private boolean      isVisible    = false;

  @Override
  public void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
  }

  @Override
  public void onResume() {
    this.isVisible = true;
    super.onResume();
  }

  @Override
  public void onPause() {
    this.isVisible = false;
    super.onPause();
  }

  @Override
  public void onNewMasterSecret(MasterSecret masterSecret) {
    this.masterSecret = masterSecret;

    if (isVisible) {
      routeApplicationState();
    }
  }

  @Override
  public void onMasterSecretCleared() {
    this.masterSecret = null;

    if (isVisible) {
      routeApplicationState();
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode == RESULT_CANCELED)
      finish();
  }

  private void routeApplicationState() {
    int state = getApplicationState();

    switch (state) {
    case STATE_CREATE_PASSPHRASE:    handleCreatePassphrase();          break;
    case STATE_PROMPT_PASSPHRASE:    handlePromptPassphrase();          break;
    case STATE_IMPORT_DATABASE:      handleImportDatabase();            break;
    case STATE_CONVERSATION_OR_LIST: handleDisplayConversationOrList(); break;
    case STATE_UPGRADE_DATABASE:     handleUpgradeDatabase();           break;
    }
  }

  private void handleCreatePassphrase() {
    Intent intent = new Intent(this, PassphraseCreateActivity.class);
    startActivityForResult(intent, 1);
  }

  private void handlePromptPassphrase() {
    Intent intent = new Intent(this, PassphrasePromptActivity.class);
    startActivityForResult(intent, 2);
  }

  private void handleImportDatabase() {
    Intent intent = new Intent(this, DatabaseMigrationActivity.class);
    intent.putExtra("master_secret", masterSecret);
    intent.putExtra("next_intent", getConversationListIntent());

    startActivity(intent);
    finish();
  }

  private void handleUpgradeDatabase() {
    Intent intent = new Intent(this, DatabaseUpgradeActivity.class);
    intent.putExtra("master_secret", masterSecret);
    intent.putExtra("next_intent", getConversationListIntent());

    startActivity(intent);
    finish();
  }

  private void handleDisplayConversationOrList() {
//    Intent intent = new Intent(this, RegistrationActivity.class);
//    startActivity(intent);
//    finish();

    ConversationParameters parameters = getConversationParameters();

    Intent intent;

    if (isShareAction() || parameters.recipients != null) {
      intent = getConversationIntent(parameters);
    } else {
      intent = getConversationListIntent();
    }

    startActivity(intent);
    finish();
  }

  private Intent getConversationIntent(ConversationParameters parameters) {
    Intent intent = new Intent(this, ConversationActivity.class);
    intent.putExtra(ConversationActivity.RECIPIENTS_EXTRA, parameters.recipients);
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, parameters.thread);
    intent.putExtra(ConversationActivity.MASTER_SECRET_EXTRA, masterSecret);
    intent.putExtra(ConversationActivity.DRAFT_TEXT_EXTRA, parameters.draftText);
    intent.putExtra(ConversationActivity.DRAFT_IMAGE_EXTRA, parameters.draftImage);
    intent.putExtra(ConversationActivity.DRAFT_AUDIO_EXTRA, parameters.draftAudio);

    return intent;
  }

  private Intent getConversationListIntent() {
    Intent intent = new Intent(this, ConversationListActivity.class);
    intent.putExtra("master_secret", masterSecret);

    return intent;
  }

  private int getApplicationState() {
    if (!MasterSecretUtil.isPassphraseInitialized(this))
      return STATE_CREATE_PASSPHRASE;

    if (masterSecret == null)
      return STATE_PROMPT_PASSPHRASE;

    if (!ApplicationMigrationService.isDatabaseImported(this))
      return STATE_IMPORT_DATABASE;

    if (DatabaseUpgradeActivity.isUpdate(this))
      return STATE_UPGRADE_DATABASE;

    return STATE_CONVERSATION_OR_LIST;
  }

  private ConversationParameters getConversationParameters() {
    if (isSendAction()) {
      return getConversationParametersForSendAction();
    } else if (isShareAction()) {
      return getConversationParametersForShareAction();
    } else {
      return getConversationParametersForInternalAction();
    }
  }

  private ConversationParameters getConversationParametersForSendAction() {
    Recipients recipients = null;
    long       threadId   = getIntent().getLongExtra("thread_id", -1);

    try {
      String data = getIntent().getData().getSchemeSpecificPart();
      recipients = RecipientFactory.getRecipientsFromString(this, data, false);
      threadId   = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipients);
    } catch (RecipientFormattingException rfe) {
      recipients = null;
    }

    return new ConversationParameters(threadId, recipients, null, null, null);
  }

  private ConversationParameters getConversationParametersForShareAction() {
    String type      = getIntent().getType();
    String draftText = null;
    Uri draftImage   = null;
    Uri draftAudio   = null;

    if ("text/plain".equals(type)) {
      draftText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
    } else if (type.startsWith("image/")) {
      draftImage = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
    } else if (type.startsWith("audio/")) {
      draftAudio = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
    }

    return new ConversationParameters(-1, null, draftText, draftImage, draftAudio);
  }

  private ConversationParameters getConversationParametersForInternalAction() {
    long threadId         = getIntent().getLongExtra("thread_id", -1);
    Recipients recipients = getIntent().getParcelableExtra("recipients");

    return new ConversationParameters(threadId, recipients, null, null, null);
  }

  private boolean isShareAction() {
    return Intent.ACTION_SEND.equals(getIntent().getAction());
  }

  private boolean isSendAction() {
    return Intent.ACTION_SENDTO.equals(getIntent().getAction());
  }

  private static class ConversationParameters {
    public final long       thread;
    public final Recipients recipients;
    public final String     draftText;
    public final Uri        draftImage;
    public final Uri        draftAudio;

    public ConversationParameters(long thread, Recipients recipients,
                                  String draftText, Uri draftImage, Uri draftAudio)
    {
     this.thread     = thread;
     this.recipients = recipients;
     this.draftText  = draftText;
     this.draftImage = draftImage;
     this.draftAudio = draftAudio;
    }
  }

}
