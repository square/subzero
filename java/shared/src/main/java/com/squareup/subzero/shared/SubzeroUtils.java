package com.squareup.subzero.shared;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
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
import com.squareup.subzero.proto.service.Service;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.SignatureDecodeException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.Networks;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;
import org.bouncycastle.util.encoders.Hex;

import static java.lang.String.format;
import static org.bitcoinj.crypto.DeterministicKey.deserializeB58;

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
  /** Everything in this class is static, so don't allow construction. */
  private SubzeroUtils() {}

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

  /**
   * Derives a P2SH-P2WSH address.
   * @param network org.bitcoinj.params.TestNet3Params or org.bitcoinj.params.MainNetParams
   * @param threshold Number of signatures needed to authorize a transaction.
   * @param publicRootKeys The public keys from wallet initialization.
   * @param path The HD wallet Path.
   * @return The derived address.
   * @throws IllegalArgumentException on invalid input.
   */
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

  /**
   * This validates that the two signatures are valid for two different pubkeys, and returns
   * them in the same order that the pubkeys are sorted into.
   * The hash is the value the signatures are over.
   * @param pubkeys the public keys to use to validate the signatures.
   * @param hash the signed message
   * @param signatures the list of signatures to validate
   * @return the sorted list of signatures, if they are valid
   * @throws RuntimeException if any of the signatures are invalid.
   */
  protected static List<byte[]> validateAndSort(List<ECKey> pubkeys, byte[] hash, List<Signature> signatures) {
    // This needs to be the same sort that ScriptBuilder.createRedeemScript does.
    pubkeys.sort(ECKey.PUBKEY_COMPARATOR);
    List<byte[]> sortedSigs = new ArrayList<>();
    Set<ByteString> seenSigs = new HashSet<>();

    // If this check fails, we've probably got invalid signatures that would fail below, or when
    // broadcast.  However, this lets us distinguish between invalid signatures and signatures over
    // the wrong data.  Useful primarily for debugging when making changes to either piece of code.
    ByteString expectedHash = ByteString.copyFrom(hash);
    for (Signature sig : signatures) {
      if (sig.hasHash()) {
        ByteString sigHash = sig.getHash();
        if (!sigHash.equals(expectedHash)) {
          throw new RuntimeException(format(
              "Our calculated hash does not match the HSM provided sig: %s != %s",
              sigHash.toStringUtf8(), expectedHash.toStringUtf8()));
        }
      }
    }

    // Iterate over pubkeys, and find signatures valid for them.
    // We should expect exactly two verifications to succeed
    for(ECKey pubkey: pubkeys) {
      for (Signature sig : signatures) {
        try {
          byte[] sigDerBytes = sig.getDer().toByteArray();
          if (pubkey.verify(hash, sigDerBytes)) {
            ByteString sigDerByteString = ByteString.copyFrom(sigDerBytes);
            if (!seenSigs.contains(sigDerByteString)) {
              // Only add the signature if it is unique
              sortedSigs.add(sigDerBytes);
              seenSigs.add(sigDerByteString);
            }
          }
        } catch (SignatureDecodeException e) {
          // keep going, we'll throw a RuntimeException later if we don't find the right number of
          // signatures
        }
      }
    }

    if(sortedSigs.size() != Constants.MULTISIG_THRESHOLD) {
      // If we end up here with less signatures than expected, it implies we either received two
      // or more of the same signature or one of the signatures is invalid.
      throw new RuntimeException(format("Failed validating signatures. Expected %d, got %d",
          Constants.MULTISIG_THRESHOLD, sortedSigs.size()));
    }

    return sortedSigs;
  }

  /**
   * Updates the given MessageDigest by hashing the given val as a little-endian 32-bit int.
   * @param val the value to update the digest with. Interpreted as a 32-bit little-endian integer.
   * @param digest the message digest to update.
   */
  protected static void hashUint32LE(long val, MessageDigest digest) {
    byte[] buf = new byte[4];
    Utils.uint32ToByteArrayLE(val, buf, 0);
    digest.update(buf);
  }

  /**
   * Updates the given MessageDigest by hashing the given val as a little-endian 64-bit int.
   * @param val the value to update the digest with. Interpreted as a 64-bit little-endian integer.
   * @param digest the message digest to update.
   */
  protected static void hashUint64LE(long val, MessageDigest digest) {
    byte[] buf = new byte[8];
    Utils.int64ToByteArrayLE(val, buf, 0);
    digest.update(buf);
  }

  /**
   * This calculates the BIP-143 hash for a transaction, see
   * <a href=https://github.com/bitcoin/bips/blob/master/bip-0143.mediawiki>bip-143</a> for the
   * details.
   * @param hashPrevouts the hashPrevouts param, a 32-byte hash.
   * @param hashSequence the hashSequence param, a 32-byte hash.
   * @param hashOutputs the hashOutputs param, a 32-byte hash.
   * @param input the TxInput object describing the transaction input. Describes the outpoint and
   *              value params.
   * @param script the scriptCode of the input.
   * @param sequence the nSequence param.
   * @param locktime the nLocktime param.
   * @return the BIP-143 hash of the transaction.
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

  /**
   * A helper constructor for making new Path objects, because proto builders are verbose.
   * @param ischange If true, the change component of the Path will be 1, otherwise it will be 0.
   * @param index The index component of the Path.
   * @return the new Path object.
   */
  public static Path newPath(boolean ischange, int index) {
    return Path.newBuilder().setIsChange(ischange).setIndex(index).build();
  }

  /**
   * Derives an HD-wallet subkey from the given key, using the given path. When path.is_change is
   * false, the subkey will be derived as key/0/index. When path.is_change is true, the subkey
   * will be derived as key/1/index.
   *
   * @param key the parent key to derive subkey from.
   * @param path the Path object describing is_change and index parameters.
   * @return the derived subkey.
   * @throws IllegalStateException on invalid input.
   */
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

  /**
   * Prints the QR code to standard output.
   * @param contents the encoded QR code.
   * @throws Exception on error.
   */
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
   * The rules for fees are identical to those implemented in the codesafe module.
   * This code allows us to catch any errors earlier.
   * @param signTxRequest request to verify fee for.
   * @throws VerificationException If the fee doesn't match the constraints described above.
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
   * Verifies size/count limits for CommandRequest proto fields.
   *
   * @param request CommandRequest to validate.
   * @return the provided request to allow chaining.
   * @throws VerificationException if limits are exceeded.
   * @throws IllegalArgumentException if CommandRequest doesn't contain matching message.
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
   * Verifies size/count limits for InternalCommandRequest proto fields.
   * @param request InternalCommandRequest to validate.
   * @throws VerificationException if limits are exceeded.
   * @throws IllegalArgumentException if InternalCommandRequest doesn't contain matching message.
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
   * Attempts to determne if the given Base58-encoded address belongs to BTC testnet or mainnet.
   * @param base58address The Base58-encoded bitcoin address.
   * @return the NetworkParameters of the BTC network the address belongs to.
   * @throws IllegalArgumentException if the address is invalid, or doesn't belong to either BTC
   *         testnet or mainnet.
   */
  public static NetworkParameters inferNetworkParameters(String base58address) {
    NetworkParameters params = null;
    for (NetworkParameters network : Networks.get()) {
      try {
        deserializeB58(base58address, network);
        return network;
      } catch (IllegalArgumentException e) {
        continue;
      }
    }
    throw new IllegalArgumentException(format("Failed to infer network parameters from %s", base58address));
  }

  /**
   * Converts the list of finalize wallet responses to a list of "xpub..." public keys.
   * @param finalizeResponses the list of base64-encoded finalize wallet responses.
   * @return a list of BIP-32 "xpub..." public keys.
   * @throws InvalidProtocolBufferException if any of the finalize wallet responses fail protobuf parsing.
   * @throws IllegalArgumentException if the number of responses != Constants.MULTISIG_PARTICIPANTS, or
   *         if any of the responses are not valid Base64.
   */
  public static List<String> finalizeResponsesToAddresses(List<String> finalizeResponses)
      throws InvalidProtocolBufferException {
    if (finalizeResponses.size() != Constants.MULTISIG_PARTICIPANTS) {
      throw new IllegalArgumentException(format("Expecting %d finalizeResponses, got %d",
          Constants.MULTISIG_PARTICIPANTS, finalizeResponses.size()));
    }

    List<String> r = Lists.newArrayList();
    for (String finalizeResponse : finalizeResponses) {
      byte[] rawFinalizeResponse = BaseEncoding.base64().decode(finalizeResponse);
      Service.CommandResponse commandResponse = Service.CommandResponse.parseFrom(rawFinalizeResponse);
      r.add(ColdWalletCreator.finalize(commandResponse));
    }
    return r;
  }

  /**
   * Verifies size/count limits for FinalizeWallet request proto fields.
   * These checks are done to align with limits set in the Codesafe module.
   * This version is for external protos.
   * @param request FinalizeWalletRequest to be checked.
   * @throws VerificationException in the event of a field exceeding a required limit.
   */
  private static void validateFinalizeWalletCommandRequest(CommandRequest.FinalizeWalletRequest
      request) throws VerificationException {
    validateEncPubKeys(request.getEncryptedPubKeysList());
  }

  /**
   * Verifies size/count limits for SignTx request proto fields.
   * These checks are done to align with limits set in the Codesafe module.
   * This version is for external protos.
   * @param request SignTxRequest to validate.
   * @throws VerificationException in the event of a field exceeding a required limit.
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
   * Verifies size/count limits for InitWallet request proto fields.
   * These checks are done to align with limits set in the Codesafe module.
   * This method is for internal protos.
   * @param request InitWalletRequest to validate.
   * @throws VerificationException in the event of a field exceeding a required limit.
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
   * Verifies size/count limits for FinalizeWallet request proto fields.
   * These checks are done to align with limits set in the Codesafe module.
   * This version is for internal protos.
   * @param request FinalizeWalletRequest to be checked.
   * @throws VerificationException in the event of a field exceeding a required limit.
   */
  private static void validateFinalizeWalletInternalCommandRequest(
      InternalCommandRequest.FinalizeWalletRequest request) throws VerificationException {
    validateEncPubKeys(request.getEncryptedPubKeysList());

    if (request.getEncryptedMasterSeed().getEncryptedMasterSeed().size() > Constants.ENCRYPTED_MASTER_SEED_MAX_SIZE) {
      throw new VerificationException(ERROR_ENCRYPTED_MASTER_SEED_SIZE);
    }
  }

  /**
   * Verifies size/count limits for SignTx request proto fields.
   * These checks are done to align with limits set in the Codesafe module.
   * This version is for internal protos.
   * @param request SignTxRequest to validate.
   * @throws VerificationException in the event of a field exceeding a required limit.
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
   * intended to perform checks on the inputs for either version.
   * @param inputs List of TxInput objects to validate.
   * @throws VerificationException in the event of a field exceeding a required limit.
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
   * SignTxRequests, this method is intended to perform checks on the keys for all of those.
   * @param pubKeys List of ByteString objects to validate.
   * @throws VerificationException in the event of a field exceeding a required limit.
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
