package com.squareup.subzero.ncipher;

import com.google.common.base.Strings;
import com.ncipher.km.marshall.NFKM_ModuleState;
import com.ncipher.km.nfkm.CardSet;
import com.ncipher.km.nfkm.Key;
import com.ncipher.km.nfkm.KeyGenerator;
import com.ncipher.km.nfkm.Module;
import com.ncipher.km.nfkm.SecurityWorld;
import com.ncipher.km.nfkm.Slot;
import com.ncipher.km.nfkm.SoftCard;
import com.ncipher.nfast.NFException;
import com.ncipher.nfast.connect.ClientException;
import com.ncipher.nfast.connect.CommandTooBig;
import com.ncipher.nfast.connect.ConnectionClosed;
import com.ncipher.nfast.marshall.M_Cmd;
import com.ncipher.nfast.marshall.M_Cmd_Args_GetTicket;
import com.ncipher.nfast.marshall.M_Cmd_Reply_GetTicket;
import com.ncipher.nfast.marshall.M_Command;
import com.ncipher.nfast.marshall.M_KeyGenParams;
import com.ncipher.nfast.marshall.M_KeyID;
import com.ncipher.nfast.marshall.M_KeyType;
import com.ncipher.nfast.marshall.M_KeyType_GenParams_Random;
import com.ncipher.nfast.marshall.M_Reply;
import com.ncipher.nfast.marshall.M_Ticket;
import com.ncipher.nfast.marshall.M_TicketDestination;
import com.ncipher.nfast.marshall.MarshallContext;
import com.ncipher.nfast.marshall.MarshallTypeError;
import com.ncipher.provider.km.nCipherKM;
import com.ncipher.provider.nCRuntimeException;
import com.squareup.subzero.framebuffer.Screens;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.security.Security;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import static java.lang.String.format;

/**
 * nCipher specific logic. Handles loading the Security World, OCS, various keys, etc.
 *
 * In theory, we could derive an interface from this class and support different HSM models/vendors.
 * In practice, it might be easier to maintain different forks, as each HSM will have its own way
 * of doing things.
 */
public class NCipher {
  private static final String DATA_SIGNING_KEY_NAME = "subzerodatasigner";

  private SecurityWorld securityWorld;
  private Module module;
  private CardSet ocsCardSet;
  private SoftCard softcard;
  private Key dataSigningKey;
  private M_KeyID masterSeedEncryptionKey;
  private Key pubKeyEncryptionKey;

  public NCipher() {
    nCipherKM provider = new nCipherKM();
    Security.addProvider(provider);
    securityWorld = nCipherKM.getSW();
  }

  public void healthCheck() throws NFException {
    if (!securityWorld.isInitialised()) {
      throw new IllegalStateException("nCipher not initialized");
    }
    if (!securityWorld.getModule(1).isUsable()) {
      String state = NFKM_ModuleState.toString(securityWorld.getModule(1).getData().state);
      throw new IllegalStateException(format("nCipher not usable: %s", state));
    }
  }

  public String getSecurityWorld() {
    // nCipher quirk: getData() returns null, unless getModules() was previously called.
    try {
      securityWorld.getModules();
      return Hex.toHexString(securityWorld.getData().hknso.value);
    } catch (nCRuntimeException | NFException e) {
      return "";
    }
  }

  public String getOcsId() {
    return Hex.toHexString(ocsCardSet.getID().value);
  }

  public byte[] getOcsCardsFile() throws IOException {
    return fileToByteString(format("cards_%s", getOcsId()));
  }

  public byte[] getOcsCardOneFile() throws IOException {
    return fileToByteString(format("card_%s_1", getOcsId()));
  }

  public void loadOcs(String defaultPassword, Screens screens) throws NFException, IOException {
    // Unless we have more than one HSM in our server, the first module is going to be usable.
    module = securityWorld.getModule(1);

    CardSet[] cardSets = securityWorld.getCardSets(null);
    if (cardSets.length > 1) {
      throw new IllegalStateException("more than one OCS detected");
    }
    if (cardSets.length == 0) {
      throw new IllegalStateException("no existing OCS found");
    }
    Slot slot = module.getSlot(0);

    System.out.println("OCS found");
    ocsCardSet = cardSets[0];
    // TODO: we could give the OCS a name, e.g. subzero-{dev, staging, prod}-ocs and then check
    // the name here.
    NCipherLoadOCS loadOCS = new NCipherLoadOCS(defaultPassword, screens, NCipherChangePasswordOCS.PLACEHOLDER_PASSWORD);

    // TODO: should ocsCardSet.load get called after renderLoading?
    ocsCardSet.load(slot, loadOCS);
    if (loadOCS.getRequirePasswordChange()) {
      String newPassword;
      while (true) {
        byte[] b = new byte[6];
        new SecureRandom().nextBytes(b);
        newPassword = Base64.toBase64String(b);
        screens.warnPasswordChange(newPassword);
        String again = screens.promptPasswordChange();
        if (again.equals(newPassword)) {
          break;
        }
        screens.promptPasswordChangeFailed();
      }
      securityWorld.changePP(slot, new NCipherChangePasswordOCS(NCipherChangePasswordOCS.PLACEHOLDER_PASSWORD, newPassword));
    }
    screens.renderLoading();

    // Get the data signing key
    dataSigningKey = securityWorld.getKey("seeinteg", DATA_SIGNING_KEY_NAME);
    if (dataSigningKey == null) {
      throw new IllegalStateException(format("seeinteg key %s not found", DATA_SIGNING_KEY_NAME));
    }
  }

  /**
   * Failing to call this will result in a NPE next time the OCS is used.
   */
  public void unloadOcs() throws NFException {
    if (ocsCardSet != null) {
      ocsCardSet.unLoad();
      ocsCardSet = null;
    }
  }

  public String createMasterSeedEncryptionKey() throws NFException {
    // Create AES masterSeedEncryptionKey. In order to be able to use the key in CodeSafe, we
    // must create a non-Java key.
    KeyGenerator keyGenerator = securityWorld.getKeyGenerator();
    M_KeyGenParams m_keyGenParams =
        new M_KeyGenParams(M_KeyType.Rijndael, new M_KeyType_GenParams_Random(256 / 8));
    Key key = keyGenerator.generateUnrecordedKey(m_keyGenParams, module,
        ocsCardSet, dataSigningKey, true);
    key.setName("MasterSeedEncryptionKey");
    key.setAppName("simple");
    key.makeBlobs(ocsCardSet);
    key.save();

    System.out.println(format("created masterSeedEncryptionKey: %s", key.getIdent()));
    masterSeedEncryptionKey = key.getKeyID(module);
    return key.getIdent();
  }

  public void loadMasterSeedEncryptionKey(String mastSeedEncryptionKeyId) throws NFException {
    Key key = securityWorld.getKey("simple", mastSeedEncryptionKeyId);
    masterSeedEncryptionKey = key.load(ocsCardSet, module);
  }

  public byte[] getMasterSeedEncryptionKeyTicket()
      throws MarshallTypeError, ClientException, ConnectionClosed, CommandTooBig {
    M_Ticket masterSeedEncryptionKeyTicket = getTicket(masterSeedEncryptionKey);

    byte[] ticket = MarshallContext.marshall(masterSeedEncryptionKeyTicket);
    System.out.println(format("masterSeedEncryptionKeyTicket: %s", Hex.toHexString(ticket)));
    return ticket;
  }

  public void loadSoftcard(String softcardName, String password, String pubKeyEncryptionKeyName) throws NFException {
    // Load the softcard and pubkey encryption key.
    softcard = getSoftcardByNameOrId(softcardName);
    softcard.load(module, new NCipherLoadSoftcard(password));

    if (Strings.isNullOrEmpty(pubKeyEncryptionKeyName)) {
      throw new IllegalStateException("no pubKeyEncryptionKey");
    }
    pubKeyEncryptionKey = securityWorld.getKey("simple", pubKeyEncryptionKeyName);
  }

  /**
   * There is currently no way to call this code. We should either expose a flag in the CLI or
   * move this into some kind of SecurityWorld initializer.
   */
  public void createPubKeyEncryptionKeyTicket() throws NFException {
    // Create AES pubKeyEncryptionKey. In order to be able to use the key in CodeSafe, we
    // must create a non-Java key.
    KeyGenerator keyGenerator = securityWorld.getKeyGenerator();
    M_KeyGenParams m_keyGenParams =
        new M_KeyGenParams(M_KeyType.Rijndael, new M_KeyType_GenParams_Random(256 / 8));

    pubKeyEncryptionKey = keyGenerator.generateUnrecordedKey(m_keyGenParams, module, softcard,
        dataSigningKey,true);

    pubKeyEncryptionKey.setName("PubKeyEncryptionKey");
    pubKeyEncryptionKey.setAppName("simple");
    pubKeyEncryptionKey.makeBlobs(softcard);
    pubKeyEncryptionKey.save();
    System.out.println(format("created pubKeyEncryptionKey: %s", pubKeyEncryptionKey.getIdent()));
    pubKeyEncryptionKey.unLoad();
  }

  public byte[] getPubKeyEncryptionKeyTicket()
      throws NFException {
    M_Ticket pubKeyEncryptionKeyTicket = getTicket( pubKeyEncryptionKey.load(softcard, module));

    byte[] ticket = MarshallContext.marshall(pubKeyEncryptionKeyTicket);
    System.out.println(format("pubKeyEncryptionKeyTicket: %s", Hex.toHexString(ticket)));
    return ticket;
  }

  private M_Ticket getTicket(M_KeyID k) throws
      ClientException, CommandTooBig, MarshallTypeError, ConnectionClosed {
    M_Cmd_Args_GetTicket args = new M_Cmd_Args_GetTicket(0, k, M_TicketDestination.Any, null);
    M_Reply rep = securityWorld.getConnection().transact(new M_Command(M_Cmd.GetTicket, 0, args));
    return ((M_Cmd_Reply_GetTicket)(rep.reply)).ticket;
  }

  private SoftCard getSoftcardByNameOrId(String softcard) throws NFException {
    for (SoftCard card : securityWorld.getSoftCards()) {
      if (card.getName().equals(softcard)) {
        return card;
      }
      String ident = Hex.toHexString(card.getID().value);
      if (ident.equals(softcard)) {
        return card;
      }
    }
    throw new IllegalStateException(format("softcard %s not found", softcard));
  }

  private static byte[] fileToByteString(String filename) throws IOException {
    Path kmdata = Paths.get("/opt/nfast/kmdata/local");
    return Files.readAllBytes(kmdata.resolve(filename));
  }
}
