package com.squareup.plutus.shared;

import com.squareup.core.Pair;
import com.squareup.protos.plutus.service.Common.Path;
import com.squareup.protos.plutus.service.Common.Signature;
import com.squareup.protos.plutus.service.Common.TxInput;
import com.squareup.protos.plutus.service.Common.TxOutput;
import com.squareup.protos.plutus.service.Service.CommandRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;
import org.bitcoinj.script.ScriptChunk;
import org.bitcoinj.script.ScriptOpCodes;

import static com.squareup.plutus.shared.PlutusUtils.derivePublicKey;
import static java.lang.String.format;
import static org.bitcoinj.crypto.DeterministicKey.deserializeB58;

public class ColdWallet {
  private NetworkParameters params;
  private int walletId;
  private List<DeterministicKey> publicRootKeys;
  private DeterministicKey gateway;

  /**
   * Constructor for ColdWallet object, used to interact with a cold wallet.
   *
   * @param params org.bitcoinj.params.TestNet3Params or org.bitcoinj.params.MainNetParams
   * @param walletId A walletId used by Plutus to identify which wallet to use
   * @param publicRootKeys The public keys from wallet initialization
   */
  public ColdWallet(NetworkParameters params, int walletId, List<String> publicRootKeys,
      String gateway) {
    this.params = params;
    this.walletId = walletId;
    this.publicRootKeys = publicRootKeys.stream()
        .map(key -> deserializeB58(key, params))
        .collect(Collectors.toList());
    this.gateway = deserializeB58(gateway, params);
  }

  public ColdWallet(NetworkParameters params, int walletId, List<DeterministicKey> publicRootKeys,
      DeterministicKey gateway) {
    this.params = params;
    this.walletId = walletId;
    this.publicRootKeys = publicRootKeys;
    this.gateway = gateway;
  }

  /**
   * Get an address to send to, for a given derivation path
   *
   * @param path The derivation path to use
   * @return Address that you can send bitcoin to cold storage with
   */
  public Address address(Path path) {
    return PlutusUtils.deriveP2SHP2WSH(params, Constants.MULTISIG_THRESHOLD, publicRootKeys, path);
  }

  /**
   * This creates a SignTx CommandRequest to begin a transaction.
   * After you call startTransaction and have cold storage sign the request, you'll pass that
   * to createTransaction.
   *
   * @param inputs The inputs to spend
   * @param outputs The outputs to receive coin
   * @param token A token reflected into the response for tracking
   * @return The SignTx command
   */
  public CommandRequest startTransaction(List<TxInput> inputs, List<TxOutput> outputs,
      String token) {
    return startTransaction(Constants.VERSION, inputs, outputs, token);
  }

  /**
   * Useful for tests.
   */
  public CommandRequest startTransaction(int version, List<TxInput> inputs, List<TxOutput> outputs,
      String token) {
    int lockTime = 0;
    return CommandRequest.newBuilder()
        .setOrClearToken(token)
        .setVersion(Constants.VERSION)
        .setWalletId(walletId)
        .setSignTx(
            CommandRequest.SignTxRequest.newBuilder()
                .addAllInputs(inputs)
                .addAllOutputs(outputs)
                .setLockTime(lockTime))
        .build();
  }

  /**
   * createTransaction takes the responses from all participants and returns a transaction suitable
   * for broadcasting to the network.  You must call it with the same inputs and outputs used to
   * call startTransaction.  An exception will be thrown if they don't match.
   *
   * @param inputs This needs to match the inputs passed to startTransaction
   * @param outputs This needs to match the inputs passed to startTransaction
   * @param signatures The signatures from the Plutus responses to the startTransaction CommandRequests
   */
  public byte[] createTransaction(List<TxInput> inputs, List<TxOutput> outputs,
      List<List<Signature>> signatures) {
    int lockTime = 0;
    int sequence = 0xfffffffe;
    return createWrappedSegwitMultisigTransaction(publicRootKeys, inputs, outputs,
        gateway, signatures, lockTime, sequence);
  }

  /**
   * Per OP_CHECKMULTISIG's documentation:
   * "Compares the first signature against each public key until it finds an ECDSA match. Starting
   * with the subsequent public key, it compares the second signature against each remaining public
   * key until it finds an ECDSA match. The process is repeated until all signatures have been
   * checked or not enough public keys remain to produce a successful result. All signatures need to
   * match a public key. Because public keys are not checked again if they fail any signature
   * comparison, signatures must be placed in the scriptSig using the same order as their
   * corresponding public keys were placed in the scriptPubKey or redeemScript. If all signatures
   * are valid, 1 is returned, 0 otherwise. Due to a bug, one extra unused value is removed from
   * the stack."
   */
  public byte[] createWrappedSegwitMultisigTransaction(
      List<DeterministicKey> publicRootKeys,
      List<TxInput> inputs,
      List<TxOutput> outputs,
      DeterministicKey gateway,
      List<List<Signature>> signatures,
      int lock_time,
      int sequence) {
    try {
      if (publicRootKeys.size() != Constants.MULTISIG_PARTICIPANTS) {
        throw new RuntimeException(format("Expected %d signers", Constants.MULTISIG_PARTICIPANTS));
      }
      if (signatures.size() != Constants.MULTISIG_THRESHOLD) {
        throw new RuntimeException(format("Expected %d signatures", Constants.MULTISIG_THRESHOLD));
      }
      if (signatures.get(0).size() != inputs.size()) {
        throw new RuntimeException("Expected signature and input counts to be equal");
      }
      if (signatures.get(0).size() != signatures.get(1).size()) {
        throw new RuntimeException("Expected same number of signatures from both signers");
      }

      // Segwit serialization format:
      // https://bitcoincore.org/en/segwit_wallet_dev/#transaction-serialization
      // 1.      |2.    |3.  |4.   |5.    |6.     |7.
      // nVersion|marker|flag|txins|txouts|witness|nLockTime
      ByteArrayOutputStream out = new ByteArrayOutputStream();

      // 1. nVersion:
      Utils.uint32ToByteStreamLE(1, out);
      // 2. The marker MUST be a 1-byte zero value: 0x00.
      out.write(0x00);
      // 3. The flag MUST be a 1-byte non-zero value. Currently, 0x01 MUST be used.
      out.write(0x01);

      // This uses a LinkedHashMap to preserve insertion order, so the iteration below is in
      // the same order as the inputs are stored here.
      List<Pair<List<ECKey>, Script>> witnesses = new ArrayList<>();

      // 4. txins
      // hashPrevouts is the double SHA256 of the serialization of all input outpoints
      MessageDigest hashPrevouts = Sha256Hash.newDigest();
      // hashSequence is the double SHA256 of the serialization of nSequence of all inputs
      MessageDigest hashSequence = Sha256Hash.newDigest();

      out.write(inputs.size()); // number of inputs
      for (TxInput input : inputs) {
        byte[] prevHash = Utils.reverseBytes(input.getPrevHashOrThrow().toByteArray());
        Integer prevIndex = input.getPrevIndexOrThrow();
        out.write(prevHash);
        Utils.uint32ToByteStreamLE(prevIndex, out);
        hashPrevouts.update(prevHash);
        PlutusUtils.hashUint32LE(prevIndex, hashPrevouts);

        // serialize the script
        // Derive the public keys from the roots
        List<ECKey> publicKeys = new ArrayList<>();
        for (DeterministicKey publicRootKey : publicRootKeys) {
          Path path = input.getPathOrThrow();
          DeterministicKey publicKey = derivePublicKey(publicRootKey, path);
          publicKeys.add(publicKey);
        }

        Script witnessScript =
            ScriptBuilder.createRedeemScript(2 /* required multisig participants */, publicKeys);
        witnesses.add(new Pair(publicKeys, witnessScript));
        byte[] scriptHash = Sha256Hash.of(witnessScript.getProgram()).getBytes();

        // checkArgument(scriptHash.length == 20);
        Script redeemScript = new ScriptBuilder()
            .addChunk(0, new ScriptChunk(ScriptOpCodes.OP_0, null))
            .data(scriptHash)
            .build();

        out.write(new VarInt(redeemScript.getProgram().length + 1).encode());
        out.write(new VarInt(redeemScript.getProgram().length).encode());
        out.write(redeemScript.getProgram());

        Utils.uint32ToByteStreamLE(sequence, out);
        PlutusUtils.hashUint32LE(sequence, hashSequence);
      }

      // 5. txouts
      MessageDigest hashOutputs = Sha256Hash.newDigest();
      out.write(outputs.size()); // number of outputs
      for (TxOutput output : outputs) {
        // Each output starts with the amount:
        Long amount = output.getAmountOrThrow();
        Utils.uint64ToByteStreamLE(BigInteger.valueOf(amount), out);
        PlutusUtils.hashUint64LE(amount, hashOutputs);

        Address address;
        switch (output.getDestination()) {
          case GATEWAY:
            DeterministicKey to = derivePublicKey(gateway, output.getPathOrThrow());
            address = new Address(params, to.getPubKeyHash());
            break;
          case CHANGE:
            address = PlutusUtils.deriveP2SHP2WSH(params, Constants.MULTISIG_THRESHOLD,
                publicRootKeys, output.getPathOrThrow());
            break;
          default:
            throw new IllegalStateException("unreachable");
        }
        byte[] dest = ScriptBuilder.createOutputScript(address).getProgram();
        out.write(new VarInt(dest.length).encode());
        out.write(dest);
        hashOutputs.update(new VarInt(dest.length).encode());
        hashOutputs.update(dest);
      }

      // Double sha256:
      byte[] prevoutsDoubleSha = Sha256Hash.hash(hashPrevouts.digest());
      byte[] sequenceDoubleSha = Sha256Hash.hash(hashSequence.digest());
      byte[] outputsDoubleSha = Sha256Hash.hash(hashOutputs.digest());

      // 6. witness
      // Each txin is associated with a witness field.
      for (int i=0; i< witnesses.size(); i++) {
        Pair<List<ECKey>, Script> witness = witnesses.get(i);
        byte[] witnessProgram = witness.right.getProgram();
        TxInput input = inputs.get(i);

        byte[] hash = PlutusUtils.bip0143hash(prevoutsDoubleSha, sequenceDoubleSha, outputsDoubleSha, input,
            witnessProgram, sequence, lock_time);

        // Each witness field starts with a compactSize integer to indicate the number of stack items
        // for the corresponding txin
        // Always 4 for 2-signature multisig scripts: 2 signatures, script, and extra 0 for CHECKMULTISIG bug
        out.write(4);

        // Each witness stack item starts with a compactSize integer to indicate the number of bytes of the item.

        // Stack item 1.
        // I think this is the CHECKMULTISIG bug empty stack item
        out.write(0);

        // We need the signatures sorted in the same order as the public keys.
        // This function validates the signatures and sorts them.
        List<byte[]> sortedSigs = PlutusUtils.validateAndSort(witness.left, hash, signatures.get(0).get(i),
            signatures.get(1).get(i));

        // Write two signatures out.  It's length + 1 because of the extra byte for the signature type
        // Stack item 2 & 3
        for (byte[] sig : sortedSigs) {
          out.write(new VarInt(sig.length + 1).encode());
          out.write(sig);
          out.write(Transaction.SigHash.ALL.value);
        }

        // And finally the script
        // Stack item 4.
        out.write(new VarInt(witnessProgram.length).encode());
        out.write(witnessProgram);
      }
      // 7. nLocktime
      Utils.uint32ToByteStreamLE(lock_time, out);

      return out.toByteArray();
    } catch (IOException e) {
      // Can't happen: We're writing to a memory stream which won't IOException
      throw new RuntimeException(e);
    }
  }

}
