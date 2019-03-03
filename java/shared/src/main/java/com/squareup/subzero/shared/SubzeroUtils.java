package com.squareup.subzero.shared;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.squareup.subzero.proto.service.Common.Destination;
import com.squareup.subzero.proto.service.Common.EncryptedPubKey;
import com.squareup.subzero.proto.service.Common.Path;
import com.squareup.subzero.proto.service.Common.Signature;
import com.squareup.subzero.proto.service.Common.TxInput;
import com.squareup.subzero.proto.service.Common.TxOutput;
import com.squareup.subzero.proto.service.Internal.InternalCommandRequest;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;
import org.spongycastle.util.encoders.Hex;

import static java.lang.String.format;


/**
 * Helper class which provides code to:
 * - derive cold wallet P2SH-P2WSH addresses from public keys. This is used when sending funds to
 *   the cold wallet or to compute change addresses.
 * - create a segwit transaction and append witnesses.
 *
 * Note: if we have a bug in the way we derive addresses and send money there, we risk losing funds.
 *       i.e. the correctness of this code is critical!
 */
public class SubzeroUtils {
  static final String ERROR_ENCRYPTED_MASTER_SEED_SIZE = "EncryptedMasterSeed size exceeds limit";
  static final String ERROR_ENCRYPTED_PUB_KEYS_COUNT = "encrypted_pub_keys count exceeds limit";
  static final String ERROR_ENCRYPTED_PUB_KEY_SIZE = "EncryptedPubKey size exceeds limit";
  static final String ERROR_INPUTS_COUNT = "Inputs count exceeds limit";
  static final String ERROR_OUTPUTS_COUNT = "Outputs count exceeds limit";
  static final String ERROR_MASTER_SEED_ENCRYPTION_KEY_TICKET_SIZE = "master_seed_encryption_key_ticket size exceeds limit";
  static final String ERROR_PUB_KEY_ENCRYPTION_KEY_TICKET_SIZE = "master_seed_encryption_key_ticket size exceeds limit";
  static final String ERROR_TXINPUT_PREV_HASH_SIZE = "TxInput prev hash size exceeds limit";
  static final String ERROR_RANDOM_BYTES_SIZE = "Random bytes exceeds limit";
  static final String ERROR_INVALID_DESTINATION = "Invalid destination in output";
  static final String ERROR_INCONSISTENT_IS_CHANGE = "Inconsistent isChange in output";

  public static Address deriveP2SHP2WSH(NetworkParameters network, int threshold, List<DeterministicKey> publicRootKeys, Path path) {
    // Constraints imposed by the Bitcoin protocol
    if (threshold <= 1) {
      throw new IllegalArgumentException("threshold too small");
    }
    if (threshold > 20) {
      throw new IllegalArgumentException("threshold too large");
    }
    if (threshold > publicRootKeys.size()) {
      throw new IllegalArgumentException("inconsistent threshold");
    }

    // Additional consistency checks
    if (threshold != Constants.MULTISIG_THRESHOLD) {
      throw new IllegalArgumentException("threshold != MULTISIG_THRESHOLD");
    }
    if (publicRootKeys.size() != Constants.MULTISIG_PARTICIPANTS) {
      throw new IllegalArgumentException("publicRootKeys.size() != MULTISIG_PARTICIPANTS");
    }

    // Derive the public keys from the roots
    ArrayList<ECKey> publicKeys = new ArrayList<>();
    for (DeterministicKey publicRootKey : publicRootKeys) {
      DeterministicKey publicKey = derivePublicKey(publicRootKey, path);
      publicKeys.add(publicKey);
    }

    Script witnessScript = ScriptBuilder.createRedeemScript(threshold, publicKeys);
    Hex.toHexString(witnessScript.getProgram());
    byte[] scriptHash = Sha256Hash.of(witnessScript.getProgram()).getBytes();

    // checkArgument(scriptHash.length == 20);
    Script redeemScript = new ScriptBuilder()
        .addChunk(0, new ScriptChunk(ScriptOpCodes.OP_0, null))
        .data(scriptHash)
        .build();
    Script redeemScriptHash = ScriptBuilder.createP2SHOutputScript(redeemScript);
    return redeemScriptHash.getToAddress(network);
  }

  // This validates that the two signatures are valid for two different pubkeys, and returns
  // them in the same order that the pubkeys are sorted into.
  // The hash is the value the signatures are over.
  protected static List<byte[]> validateAndSort(List<ECKey> pubkeys, byte[] hash, Signature sig1,
      Signature sig2) {
    // This needs to be the same sort that ScriptBuilder.createRedeemScript does.
    pubkeys.sort(ECKey.PUBKEY_COMPARATOR);
    List<byte[]> sortedSigs = new ArrayList<>();

    // If this check fails, we've probably got invalid signatures that would fail below, or when
    // broadcast.  However, this lets us distinguish between invalid signatures and signatures over
    // the wrong data.  Useful primarily for debugging when making changes to either piece of code.
    if(sig1.hasHash()) {
      if(!Arrays.equals(sig1.getHash().toByteArray(), hash)) {
        throw new RuntimeException(format(
            "Our calculated hash does not match the HSM provided sig1: %s != %s",
            Hex.toHexString(sig1.getHash().toByteArray()), Hex.toHexString(hash)));
      }
    }

    if(sig2.hasHash()) {
      if(!Arrays.equals(sig2.getHash().toByteArray(), hash)) {
        throw new RuntimeException(format(
            "Our calculated hash does not match the HSM provided sig2: %s != %s",
            Hex.toHexString(sig2.getHash().toByteArray()), Hex.toHexString(hash)));
      }
    }

    // Iterate over pubkeys, and find signatures valid for them.
    // We should expect exactly two verifications to succeed
    for(ECKey pubkey: pubkeys) {
      if(pubkey.verify(hash, sig1.getDer().toByteArray())) {
        sortedSigs.add(sig1.getDer().toByteArray());
      } else if(pubkey.verify(hash, sig2.getDer().toByteArray())) {
        sortedSigs.add(sig2.getDer().toByteArray());
      }
    }

    // Possible cases: Two of the same signature, or invalid signatures.
    if(sortedSigs.size() != 2) {
      throw new RuntimeException(format("Failed validating signatures. Expected 2, got %d", sortedSigs.size()));
    }

    return sortedSigs;
  }


  protected static void hashUint32LE(long val, MessageDigest digest) {
    byte[] buf = new byte[4];
    Utils.uint32ToByteArrayLE(val, buf, 0);
    digest.update(buf);
  }

  protected static void hashUint64LE(long val, MessageDigest digest) {
    byte[] buf = new byte[8];
    Utils.uint64ToByteArrayLE(val, buf, 0);
    digest.update(buf);
  }

  /**
   * This calculates the hash for a transaction
   */
  protected static byte[] bip0143hash(
      byte[] hashPrevouts,
      byte[] hashSequence,
      byte[] hashOutputs,
      TxInput input,
      byte[] script,
      int sequence,
      int locktime) {
    MessageDigest hash = Sha256Hash.newDigest();

    // The comments below are straight from BIP 0143.
    // Double SHA256 of the serialization of:
    //     1. nVersion of the transaction (4-byte little endian)
    hashUint32LE(1, hash);

    //     2. hashPrevouts (32-byte hash)
    hash.update(hashPrevouts);

    //     3. hashSequence (32-byte hash)
    hash.update(hashSequence);

    //     4. outpoint (32-byte hash + 4-byte little endian)
    hash.update(Utils.reverseBytes(input.getPrevHash().toByteArray()));
    hashUint32LE(input.getPrevIndex(), hash);

    //     5. scriptCode of the input (serialized as scripts inside CTxOuts)
    hash.update(new VarInt(script.length).encode());
    hash.update(script);

    //     6. value of the output spent by this input (8-byte little endian)
    hashUint64LE(input.getAmount(), hash);

    //     7. nSequence of the input (4-byte little endian)
    hashUint32LE(sequence, hash);

    //     8. hashOutputs (32-byte hash)
    hash.update(hashOutputs);

    //     9. nLocktime of the transaction (4-byte little endian)
    hashUint32LE(locktime, hash);

    //    10. sighash type of the signature (4-byte little endian)
    hashUint32LE(Transaction.SigHash.ALL.value, hash);

    // Double SHA256:
    return hash.digest(hash.digest());
  }

  // A helper constructor, because proto builders are verbose
  public static Path newPath(boolean ischange, int index) {
    return Path.newBuilder().setIsChange(ischange).setIndex(index).build();
  }

  protected static DeterministicKey derivePublicKey(DeterministicKey key, Path path) {
    if (path.getIndex() < 0) {
      throw new IllegalStateException("index should be between 0 and 2^31-1");
    }

    // note: key might be m/0 for prod, but it's m/ for testnet. Investigate if this is some kind of
    // BitcoinJ quirk?
    key = HDKeyDerivation.deriveChildKey(key, new ChildNumber(path.getIsChange() ? 1 : 0, false));

    key = HDKeyDerivation.deriveChildKey(key, new ChildNumber(path.getIndex(), false));

    int length = key.getPubKey().length;
    if (length != 33) {
      throw new IllegalStateException(format("expecting compressed key. Got %d bytes.", length));
    }

    return key;
  }

  public static void printQrCode(String contents) throws Exception {
    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix matrix = qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, 1, 1);
    for (int y=0; y<matrix.getHeight(); y++) {
      for (int x=0; x<matrix.getWidth(); x++) {
        if (matrix.get(x, y)) {
          System.out.print("\033[40m  \033[0m");
        } else {
          System.out.print("\033[107m  \033[0m");
        }
      }
      System.out.println();
    }
    System.out.println();
  }

  /**
   * Verifies that the fee is less than 1 BTC or less than 10% of funds going to gateway address
   * The rules for fees are identical to those implemented in the codesafe module
   * This code allows us to catch any errors earlier
   * @param signTxRequest request to verify fee for
   */
  protected static void validateFees(CommandRequest.SignTxRequest signTxRequest)
      throws VerificationException {
    Coin total = Coin.ZERO;
    Coin fee = Coin.ZERO;
    for (TxInput input : signTxRequest.getInputsList()) {
      fee = fee.add(Coin.valueOf(input.getAmount()));
    }
    for (TxOutput output : signTxRequest.getOutputsList()) {
      fee = fee.subtract(Coin.valueOf(output.getAmount()));

      if (output.getDestination().equals(Destination.GATEWAY)) {
        total = total.add(Coin.valueOf(output.getAmount()));
      }
    }

    if (fee.isNegative()) {
      throw new VerificationException("Transaction fee is negative");
    }

    if (fee.isGreaterThan(Coin.COIN) && fee.isGreaterThan(total.divide(10))) {
      throw new VerificationException("Transaction fee is greater than 1BTC and 10% of the total "
          + "amount transferred");
    }
  }

  /**
   * Verifies size/count limits for CommandRequest proto fields
   *
   * @param request CommandRequest to validate
   * @return the provided request to allow chaining
   * @throws VerificationException if limits are exceeded
   * @throws IllegalArgumentException if CommandRequest doesn't contain matching message
   */
  public static CommandRequest validateCommandRequest(CommandRequest request)
      throws VerificationException, IllegalArgumentException {
    switch (request.getCommandCase()) {
      case INITWALLET:
        // We don't need to validate anything.
        break;
      case FINALIZEWALLET:
        validateFinalizeWalletCommandRequest(request.getFinalizeWallet());
        break;
      case SIGNTX:
        CommandRequest.SignTxRequest signRequest = request.getSignTx();
        validateSignTxCommandRequest(signRequest);
        validateFees(signRequest);
        break;
      default:
        throw new IllegalStateException("unreachable");
    }
    return request;
  }

  /**
   * Verifies size/count limits for InternalCommandRequest proto fields
   * @param request InternalCommandRequest to validate
   * @throws VerificationException if limits are exceeded
   * @throws IllegalArgumentException if InternalCommandRequest doesn't contain matching message
   */
  public static void validateInternalCommandRequest(InternalCommandRequest request)
      throws VerificationException, IllegalArgumentException {
    switch (request.getCommandCase()) {
      case INITWALLET:
        validateInitWalletInternalCommandRequest(request);
        break;
      case FINALIZEWALLET:
        validateFinalizeWalletInternalCommandRequest(request.getFinalizeWallet());
        break;
      case SIGNTX:
        validateSignTxInternalCommandRequest(request.getSignTx());
        break;
      default:
        throw new IllegalStateException("unreachable");
    }
  }

  /**
   * Verifies size/count limits for FinalizeWallet request proto fields
   * These checks are done to align with limits set in the Codesafe module
   * This version is for external protos
   * @param request FinalizeWalletRequest to be checked
   * @throws VerificationException in the event of a field exceeding a required limit
   */
  private static void validateFinalizeWalletCommandRequest(CommandRequest.FinalizeWalletRequest
      request) throws VerificationException {
    validateEncPubKeys(request.getEncryptedPubKeysList());
  }

  /**
   * Verifies size/count limits for SignTx request proto fields
   * These checks are done to align with limits set in the Codesafe module
   * This version is for external protos
   * @param request SignTxRequest to validate
   * @throws VerificationException in the event of a field exceeding a required limit
   */
  private static void validateSignTxCommandRequest(CommandRequest.SignTxRequest request)
      throws VerificationException {
    validateTxInputs(request.getInputsList());
    if (request.getOutputsCount() > Constants.OUTPUTS_COUNT_MAX) {
      throw new VerificationException(ERROR_OUTPUTS_COUNT);
    }

    // Validate that outputs are either CHANGE or GATEWAY. For outputs, the isChange attribute
    // of the path is redundant, so perform a consistency check.
    for (TxOutput output : request.getOutputsList()) {
      switch (output.getDestination()) {
        case CHANGE:
          if (!output.getPath().getIsChange()) {
            throw new VerificationException(ERROR_INCONSISTENT_IS_CHANGE);
          }
          break;
        case GATEWAY:
          if (output.getPath().getIsChange()) {
            throw new VerificationException(ERROR_INCONSISTENT_IS_CHANGE);
          }
          break;
        default:
          throw new VerificationException(ERROR_INVALID_DESTINATION);
      }
    }
  }

  /**
   * Verifies size/count limits for InitWallet request proto fields
   * These checks are done to align with limits set in the Codesafe module
   * This method is for internal protos
   * @param request InitWalletRequest to validate
   * @throws VerificationException in the event of a field exceeding a required limit
   */
  private static void validateInitWalletInternalCommandRequest(
      InternalCommandRequest request) throws VerificationException {
    if (request.getMasterSeedEncryptionKeyTicket().size() >
        Constants.MASTER_SEED_ENCRYPTION_KEY_TICKET_MAX_SIZE) {
      throw new VerificationException(ERROR_MASTER_SEED_ENCRYPTION_KEY_TICKET_SIZE);
    }
    if (request.getPubKeyEncryptionKeyTicket().size() > Constants.PUB_KEY_ENCRYPTION_KEY_TICKET_MAX_SIZE) {
      throw new VerificationException(ERROR_PUB_KEY_ENCRYPTION_KEY_TICKET_SIZE);
    }
    if (request.getInitWallet().getRandomBytes().size() > Constants.RANDOM_BYTES_MAX_SIZE) {
      throw new VerificationException(ERROR_RANDOM_BYTES_SIZE);
    }
  }

  /**
   * Verifies size/count limits for FinalizeWallet request proto fields
   * These checks are done to align with limits set in the Codesafe module
   * This version is for internal protos
   * @param request FinalizeWalletRequest to be checked
   * @throws VerificationException in the event of a field exceeding a required limit
   */
  private static void validateFinalizeWalletInternalCommandRequest(
      InternalCommandRequest.FinalizeWalletRequest request) throws VerificationException {
    validateEncPubKeys(request.getEncryptedPubKeysList());

    if (request.getEncryptedMasterSeed().getEncryptedMasterSeed().size() > Constants.ENCRYPTED_MASTER_SEED_MAX_SIZE) {
      throw new VerificationException(ERROR_ENCRYPTED_MASTER_SEED_SIZE);
    }
  }

  /**
   * Verifies size/count limits for SignTx request proto fields
   * These checks are done to align with limits set in the Codesafe module
   * This version is for internal protos
   * @param request SignTxRequest to validate
   * @throws VerificationException in the event of a field exceeding a required limit
   */
  private static void validateSignTxInternalCommandRequest(
      InternalCommandRequest.SignTxRequest request) throws VerificationException {
    validateEncPubKeys(request.getEncryptedPubKeysList());
    validateTxInputs(request.getInputsList());
    if (request.getOutputsCount() > Constants.OUTPUTS_COUNT_MAX) {
      throw new VerificationException(ERROR_OUTPUTS_COUNT);
    }
    if (request.getEncryptedMasterSeed().getEncryptedMasterSeed().size() > Constants.ENCRYPTED_MASTER_SEED_MAX_SIZE) {
      throw new VerificationException(ERROR_ENCRYPTED_MASTER_SEED_SIZE);
    }
  }

  /**
   * As TxInputs are shared between internal and external SignTx requests, this method is
   * intended to perform checks on the inputs for either version
   * @param inputs List of TxInput objects to validate
   * @throws VerificationException in the event of a field exceeding a required limit
   */
  private static void validateTxInputs(List<TxInput> inputs) throws VerificationException {
    if (inputs.size() > Constants.INPUTS_COUNT_MAX) {
      throw new VerificationException(ERROR_INPUTS_COUNT);
    }
    for (TxInput input : inputs) {
      if (input.getPrevHash().size() > Constants.TXINPUT_PREV_HASH_MAX_SIZE) {
        throw new VerificationException(ERROR_TXINPUT_PREV_HASH_SIZE);
      }
    }
  }

  /**
   * As EncPubKeys are shared between internal/external FinalizeWalletRequests, and internal
   * SignTxRequests, this method is intended to perform checks on the keys for all of those
   * @param pubKeys List of ByteString objects to validate
   * @throws VerificationException in the event of a field exceeding a required limit
   */
  private static void validateEncPubKeys(List<EncryptedPubKey> pubKeys) throws VerificationException {
    if (pubKeys.size() > Constants.ENCRYPTED_PUB_KEYS_MAX_COUNT) {
      throw new VerificationException(ERROR_ENCRYPTED_PUB_KEYS_COUNT);
    }
    for (EncryptedPubKey pubKey : pubKeys) {
      if (pubKey.getEncryptedPubKey().size() > Constants.ENCRYPTED_PUB_KEY_MAX_SIZE) {
        throw new VerificationException(ERROR_ENCRYPTED_PUB_KEY_SIZE);
      }
    }
  }
}
