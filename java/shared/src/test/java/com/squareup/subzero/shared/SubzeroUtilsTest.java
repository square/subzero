package com.squareup.subzero.shared;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.squareup.subzero.proto.service.Common.Destination;
import com.squareup.subzero.proto.service.Common.EncryptedMasterSeed;
import com.squareup.subzero.proto.service.Common.EncryptedPubKey;
import com.squareup.subzero.proto.service.Common.Path;
import com.squareup.subzero.proto.service.Common.TxInput;
import com.squareup.subzero.proto.service.Common.TxOutput;
import com.squareup.subzero.proto.service.Internal.InternalCommandRequest;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_ENCRYPTED_MASTER_SEED_SIZE;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_ENCRYPTED_PUB_KEYS_COUNT;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_ENCRYPTED_PUB_KEY_SIZE;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_INCONSISTENT_IS_CHANGE;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_INPUTS_COUNT;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_INVALID_DESTINATION;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_MASTER_SEED_ENCRYPTION_KEY_TICKET_SIZE;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_OUTPUTS_COUNT;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_PUB_KEY_ENCRYPTION_KEY_TICKET_SIZE;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_RANDOM_BYTES_SIZE;
import static com.squareup.subzero.shared.SubzeroUtils.ERROR_TXINPUT_PREV_HASH_SIZE;
import static com.squareup.subzero.shared.SubzeroUtils.validateCommandRequest;
import static com.squareup.subzero.shared.SubzeroUtils.validateFees;
import static com.squareup.subzero.shared.SubzeroUtils.validateInternalCommandRequest;


public class SubzeroUtilsTest {
  private static ByteString shortByteString;
  private static ByteString longByteString;
  private static List<EncryptedPubKey> pubKeys;

  @Before
  public void setUp() {
    pubKeys = new ArrayList<>();
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFromUtf8("test-pubkey1")).build());
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFromUtf8("test-pubkey2")).build());
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFromUtf8("test-pubkey3")).build());
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFromUtf8("test-pubkey4")).build());

    shortByteString = ByteString.copyFromUtf8("abcdefghijklmnop");

    String longString = String.join("", Collections.nCopies(100, "abcdefghijklmnopqrstuvwxyz"));
    longByteString = ByteString.copyFromUtf8(longString);
  }

  @Test public void deriveP2SHP2WSH() {
    // Using https://iancoleman.io/bip39/, I created 4 random keys. The random mnemonics were:
    // - smoke opinion phone story undo medal mobile slow scan faint magnet correct
    //   m/1'
    //   tpubD9xeStpGFRUVhE7c3dDsdqNmbPC6RjQhvSc7UetKRMYjjLusaJx6Nfb3bAGQXDmnY5iuBApU75yuLDDYDXvq3FnXX1zEQMficGXkrYn6i1L
    //
    // - agent erase unhappy theme eager blue obey flat pilot chalk grab melt
    //   m/1'
    //   tpubD9JLQCZyqpsY7L5dY86vJnJ1g1Acgb5MzpF7MgE29qQzUcM1LpxTuKeCRWwnCncVRhxYugumQqndARHqQ95c629h7bEciRH39LHXjpJEbsY
    //
    // - until weasel medal stick supply resource sail caught fork response turtle raw
    //   m/1'
    //   tpubD9F4zKZ1qkfAghtK8BCT5EeFJm5X3YP34M4xGi98KAok4rkwYBxoFdM9EaWxTK3PTRhe7Mwd7Mfm96ovKb64vDnENj84jfoN6gMzKx85uYm
    //
    // - rule clarify trouble pig spawn ring benefit fringe resemble eyebrow design sausage
    //   m/1'
    //   tpubD8YrhVDcGsTdyj4pZh2ZGJvcqpASFZLai9tRHPj4ikjyn4MZcoHjqeuzNBGntpkSxNg61FKo9yCWpCvUGVnMpJJUwGJcWUpxTdPQKVdgoSo
    //
    // I then randomly picked the following address: m/1'/0/3172
    //
    // The redeem script is:
    // 0 [sha256 of
    //   (2 [0310718e74f4a331bc76f933772712a1aa694d3332572355d459b7acacfdfb7b2e]
    //      [031f666d2e5b4cde9cc002b386abdb6fa0bcd6a2dda1a698d6e437e09afd8e16b0]
    //      [036f97a6f016b595062f05bb60bb680511b1df426b7072e002a7e943e59343d0be]
    //      [038c51f615a722387d540799d9ba10711214ba7b77c74e225f4f11f8d729a272bb] 4 checkmultisig)]
    //
    // which hashes to:
    // 336c78bfe541c4b4dd1cb5b3d3ebd5a346db6223 (i.e. 2Mww8Pp2ZZc7gUr1z2ddUHx7Sa9J9avA7hJ)
    Path path = SubzeroUtils.newPath(false, 3172);
    List<DeterministicKey> addresses = ImmutableList.of(
        "tpubD9xeStpGFRUVhE7c3dDsdqNmbPC6RjQhvSc7UetKRMYjjLusaJx6Nfb3bAGQXDmnY5iuBApU75yuLDDYDXvq3FnXX1zEQMficGXkrYn6i1L",
        "tpubD9JLQCZyqpsY7L5dY86vJnJ1g1Acgb5MzpF7MgE29qQzUcM1LpxTuKeCRWwnCncVRhxYugumQqndARHqQ95c629h7bEciRH39LHXjpJEbsY",
        "tpubD9F4zKZ1qkfAghtK8BCT5EeFJm5X3YP34M4xGi98KAok4rkwYBxoFdM9EaWxTK3PTRhe7Mwd7Mfm96ovKb64vDnENj84jfoN6gMzKx85uYm",
        "tpubD8YrhVDcGsTdyj4pZh2ZGJvcqpASFZLai9tRHPj4ikjyn4MZcoHjqeuzNBGntpkSxNg61FKo9yCWpCvUGVnMpJJUwGJcWUpxTdPQKVdgoSo")
        .stream().map(pub -> DeterministicKey.deserializeB58(pub, TestNet3Params.get())).collect(Collectors.toList());

    Address address = SubzeroUtils.deriveP2SHP2WSH(TestNet3Params.get(), 2, addresses, path);
    assertEquals("2MuAdStu2xZtRSyA5B6wRtj7SmaLjDyfm1H", address.toString());
  }

  @Test public void derivePublicKey() {
    // Using https://iancoleman.io/bip39/, I created a random key. The derivation we use for cold
    // wallet is m/coin_type'/change/address_index. The coin_type' is taken care of for us,
    // so we only care about change/address_index.
    //
    // To help future debugging efforts, the random mnemonic was:
    // net kit animal ball brass honey give drill robot camera timber shrimp
    //
    // At BIP32 path m/1', we get the extended public key:
    // tpubD9MkddWNiSU4tvVjoGryqunfjMkjMXhB4D17McCNrE7ZKd3R4kRPR1kB8gq1u2q8UKx9KqPRGyQW6Fhq7vZwDNZYZGbKd8AMSPtBhoumfVG
    //
    // I then looked up the keys for
    // m/1'/0/42: myobdmpEtNgLgFXCK4EBK3u11sfG6yJVCb / 0289ab6408ce23716bb5484ef31aae98d45013cea12920bd0d92c75ee318f84d28
    // and m/1'/1/62: n4HYVM4T5kCjYnL72DCvkKEkgZMSvUfQuY / 03739f4d23146ab04e007809339b39eb0d5ea34ec79e518b4639b2b1bf2fd744dd
    //
    // (where 42 and 62 are randomly chosen values).

    DeterministicKey extendedPublicKey = DeterministicKey.deserializeB58(
        "tpubD9MkddWNiSU4tvVjoGryqunfjMkjMXhB4D17McCNrE7ZKd3R4kRPR1kB8gq1u2q8UKx9KqPRGyQW6Fhq7vZwDNZYZGbKd8AMSPtBhoumfVG",
        TestNet3Params.get());

    DeterministicKey childKey = SubzeroUtils.derivePublicKey(extendedPublicKey, SubzeroUtils.newPath(false, 42));
    byte[] expected = Hex.decode("0289ab6408ce23716bb5484ef31aae98d45013cea12920bd0d92c75ee318f84d28");
    assertArrayEquals(expected, childKey.getPubKey());

    childKey = SubzeroUtils.derivePublicKey(extendedPublicKey, SubzeroUtils.newPath(true, 62));
    expected = Hex.decode("03739f4d23146ab04e007809339b39eb0d5ea34ec79e518b4639b2b1bf2fd744dd");
    assertArrayEquals(expected, childKey.getPubKey());
  }

  @Test
  public void testValidateFees() {
    String feeExceedsLimitString = "Transaction fee is greater than 1BTC and 10% of the total "
        + "amount transferred";
    String feeNegativeString = "Transaction fee is negative";

    // test case where the fee is within both limits (under 1BTC, under 10% of total)
    CommandRequest.SignTxRequest signTxRequest1 = CommandRequest.SignTxRequest.newBuilder()
        .addInputs(testInput(100000))
        .addOutputs(testOutput(95000))
        .build();
    validateFees(signTxRequest1);

    // test case where fee is under 1BTC but over 10%
    CommandRequest.SignTxRequest signTxRequest2 = CommandRequest.SignTxRequest.newBuilder()
        .addInputs(testInput(1000000))
        .addOutputs(testOutput(500000))
        .build();
    validateFees(signTxRequest2);

    // test case where fee is over 1BTC but under 10%
    CommandRequest.SignTxRequest signTxRequest3 = CommandRequest.SignTxRequest.newBuilder()
        .addInputs(testInput(2000000000))
        .addOutputs(testOutput(1890000000))
        .build();
    validateFees(signTxRequest3);

    // test case where fee is over 1BTC and over 10%
    CommandRequest.SignTxRequest signTxRequest4 = CommandRequest.SignTxRequest.newBuilder()
        .addInputs(testInput(10000000000L))
        .addOutputs(testOutput(8000000000L))
        .build();
    
    VerificationException e1 = assertThrows(VerificationException.class, () -> validateFees(signTxRequest4));
    assertTrue(e1.getMessage().contains(feeExceedsLimitString));

    // test case where fee is over 1BTC and over 10% if you don't count the amount going to
    // the change address in the total (which is how it should be calculated)
    // this case uses input=10,000,000,000, output(gateway)=8,000,000,000
    // and output(change)=1,150,000,000. the fee is 850,000,000, which is 8.5 BTC, and 10.63%
    // this should fail validation. if we miscalculated the fee % by including the amount going
    // to the change address as part of the total, it would be 9.29%, which would pass validation
    CommandRequest.SignTxRequest signTxRequest5 = CommandRequest.SignTxRequest.newBuilder()
        .addInputs(testInput(10000000000L))
        .addOutputs(testOutput(8000000000L, Destination.GATEWAY))
        .addOutputs(testOutput(1150000000L, Destination.CHANGE))
        .build();
    VerificationException e2 = assertThrows(VerificationException.class, () -> validateFees(signTxRequest5));
    assertTrue(e2.getMessage().contains(feeExceedsLimitString));

    // test case where fee is negative
    CommandRequest.SignTxRequest signTxRequest6 = CommandRequest.SignTxRequest.newBuilder()
        .addInputs(testInput(1000))
        .addOutputs(testOutput(900, Destination.GATEWAY))
        .addOutputs(testOutput(200, Destination.CHANGE))
        .build();
    VerificationException e3 = assertThrows(VerificationException.class, () -> validateFees(signTxRequest6));
    assertTrue(e3.getMessage().contains(feeNegativeString));
  }

  @Test
  public void testValidateInitWalletInternalCommandRequest() {
    // should succeed
    InternalCommandRequest request = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setMasterSeedEncryptionKeyTicket(shortByteString)
        .setPubKeyEncryptionKeyTicket(shortByteString)
        .setInitWallet(InternalCommandRequest.InitWalletRequest.newBuilder()
            .setRandomBytes(shortByteString))
        .build();
    validateInternalCommandRequest(request);

    // should fail because the master_seed encryption key ticket is too big
    InternalCommandRequest request2 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setMasterSeedEncryptionKeyTicket(longByteString)
        .setPubKeyEncryptionKeyTicket(shortByteString)
        .setInitWallet(InternalCommandRequest.InitWalletRequest.newBuilder()
            .setRandomBytes(shortByteString))
        .build();
    VerificationException e1 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request2));
    assertTrue(e1.getMessage().contains(ERROR_MASTER_SEED_ENCRYPTION_KEY_TICKET_SIZE));

    // should fail because the pub keys encryption key ticket is too big
    InternalCommandRequest request3 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setMasterSeedEncryptionKeyTicket(shortByteString)
        .setPubKeyEncryptionKeyTicket(longByteString)
        .setInitWallet(InternalCommandRequest.InitWalletRequest.newBuilder()
            .setRandomBytes(shortByteString))
        .build();
    VerificationException e2 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request3));
    assertTrue(e2.getMessage().contains(ERROR_PUB_KEY_ENCRYPTION_KEY_TICKET_SIZE));

    // should fail because the random bytes is too big
    InternalCommandRequest request4 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setMasterSeedEncryptionKeyTicket(shortByteString)
        .setPubKeyEncryptionKeyTicket(shortByteString)
        .setInitWallet(InternalCommandRequest.InitWalletRequest.newBuilder()
            .setRandomBytes(longByteString))
        .build();
    VerificationException e3 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request4));
    assertTrue(e3.getMessage().contains(ERROR_RANDOM_BYTES_SIZE));
  }

  @Test
  public void testValidateFinalizeWalletInternalCommandRequest() {
    // should succeed
    InternalCommandRequest request = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setFinalizeWallet(InternalCommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(shortByteString).build()))
        .build();
    validateInternalCommandRequest(request);

    // a long encrypted wallet should make it fail
    InternalCommandRequest request2 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setFinalizeWallet(InternalCommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(longByteString)))
        .build();
    VerificationException e1 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request2));
    assertTrue(e1.getMessage().contains(ERROR_ENCRYPTED_MASTER_SEED_SIZE));

    // should fail with longer key
    pubKeys.remove(3);
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(longByteString).build());
    InternalCommandRequest request3 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setFinalizeWallet(InternalCommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(longByteString)))
      .build();
    VerificationException e2 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request3));
    assertTrue(e2.getMessage().contains(ERROR_ENCRYPTED_PUB_KEY_SIZE));

    // adding an extra key should make this one fail
    pubKeys.remove(3);
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFromUtf8("test-pubkey4")).build());
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFromUtf8("test-pubkey5")).build());
    InternalCommandRequest request4 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setFinalizeWallet(InternalCommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(shortByteString).build()))
        .build();
    VerificationException e3 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request4));
    assertTrue(e3.getMessage().contains(ERROR_ENCRYPTED_PUB_KEYS_COUNT));
  }

  @Test
  public void testValidateSignTxInternalCommandRequest() {
    // should succeed
    InternalCommandRequest request = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setSignTx(InternalCommandRequest.SignTxRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .addInputs(testInput())
            .addOutputs(testOutput())
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(shortByteString).buildPartial())
            .setLockTime(0))
        .build();
    validateInternalCommandRequest(request);

    // should fail because of overly long wallet
    InternalCommandRequest request2 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setSignTx(InternalCommandRequest.SignTxRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .addInputs(testInput())
            .addOutputs(testOutput())
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(longByteString).build())
            .setLockTime(0))
            .build();
    VerificationException e1 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request2));
    assertTrue(e1.getMessage().contains(ERROR_ENCRYPTED_MASTER_SEED_SIZE));

    // should fail because of prev hash in TxInput being too long
    InternalCommandRequest request3 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setSignTx(InternalCommandRequest.SignTxRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .addInputs(testInput(100000, longByteString))
            .addOutputs(testOutput())
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(longByteString).build())
            .setLockTime(0))
        .build();
    VerificationException e2 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request3));
    assertTrue(e2.getMessage().contains(ERROR_TXINPUT_PREV_HASH_SIZE));

    // this should fail due to having too many inputs
    List<TxInput> inputs = new ArrayList<>();
    for (int i = 0; i < 150; i++) {
      inputs.add(testInput());
    }
    InternalCommandRequest request5 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setSignTx(InternalCommandRequest.SignTxRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .addAllInputs(inputs)
            .addOutputs(testOutput())
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(shortByteString).build())
            .setLockTime(0))
        .build();
    VerificationException e3 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request5));
    assertTrue(e3.getMessage().contains(ERROR_INPUTS_COUNT));

    // this should fail due to having too many outputs
    List<TxOutput> outputs = new ArrayList<>();
    for (int i = 0; i < 150; i++) {
      outputs.add(testOutput());
    }
    InternalCommandRequest request6 = InternalCommandRequest.newBuilder()
        .setVersion(Constants.VERSION)
        .setWalletId(0)
        .setSignTx(InternalCommandRequest.SignTxRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys)
            .addInputs(testInput())
            .addAllOutputs(outputs)
            .setEncryptedMasterSeed(EncryptedMasterSeed.newBuilder().setEncryptedMasterSeed(shortByteString).build())
            .setLockTime(0))
        .build();
    VerificationException e4 = assertThrows(VerificationException.class, () -> validateInternalCommandRequest(request6));
    assertTrue(e4.getMessage().contains(ERROR_OUTPUTS_COUNT));
  }

  @Test
  public void testEnsureValidateSignTxFailsWithUnknownDestinations() {
    CommandRequest request = CommandRequest.newBuilder()
        .setSignTx(CommandRequest.SignTxRequest.newBuilder()
            .addOutputs(TxOutput.newBuilder()
                .setAmount(1000)
                .setDestination(Destination.DEFAULT_DESTINATION_DO_NOT_USE)
                .setPath(Path.newBuilder()
                    .setIsChange(false)
                    .setIndex(2))
            ))
        .build();
    VerificationException e = assertThrows(VerificationException.class, () -> validateCommandRequest(request));
    assertTrue(e.getMessage().contains(ERROR_INVALID_DESTINATION));
  }

  @Test
  public void testEnsureValidateSignTxFailsWithInconsistentIsChange() {
    CommandRequest request1 = CommandRequest.newBuilder()
        .setSignTx(CommandRequest.SignTxRequest.newBuilder()
            .addOutputs(TxOutput.newBuilder()
                .setAmount(1000)
                .setDestination(Destination.GATEWAY)
                .setPath(Path.newBuilder()
                    .setIsChange(true)
                    .setIndex(2))
            ))
        .build();
    VerificationException e1 = assertThrows(VerificationException.class, () -> validateCommandRequest(request1));
    assertTrue(e1.getMessage().contains(ERROR_INCONSISTENT_IS_CHANGE));

    CommandRequest request2 = CommandRequest.newBuilder()
        .setSignTx(CommandRequest.SignTxRequest.newBuilder()
            .addOutputs(TxOutput.newBuilder()
                .setAmount(1000)
                .setDestination(Destination.CHANGE)
                .setPath(Path.newBuilder()
                    .setIsChange(false)
                    .setIndex(2))
            ))
        .build();
    VerificationException e2 = assertThrows(VerificationException.class, () -> validateCommandRequest(request2));
    assertTrue(e2.getMessage().contains(ERROR_INCONSISTENT_IS_CHANGE));
  }

  @Test
  public void testValidateFinalizeWalletCommandRequest() {
    // should succeed
    CommandRequest request = CommandRequest.newBuilder()
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys))
        .build();
    validateCommandRequest(request);

    // adding an extra key should make this one fail
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(ByteString.copyFromUtf8("test-pubkey5")).build());
    CommandRequest request2 = CommandRequest.newBuilder()
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys))
            .build();
    VerificationException e1 = assertThrows(VerificationException.class, () -> validateCommandRequest(request2));
    assertTrue(e1.getMessage().contains(ERROR_ENCRYPTED_PUB_KEYS_COUNT));

    // should fail with longer key
    pubKeys.remove(4);
    pubKeys.remove(3);
    pubKeys.add(EncryptedPubKey.newBuilder().setEncryptedPubKey(longByteString).build());
    CommandRequest request3 = CommandRequest.newBuilder()
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addAllEncryptedPubKeys(pubKeys))
        .build();
    VerificationException e2 = assertThrows(VerificationException.class, () -> validateCommandRequest(request3));
    assertTrue(e2.getMessage().contains(ERROR_ENCRYPTED_PUB_KEY_SIZE));
  }

  @Test
  public void testValidateSignTxCommandRequest() {
    // should succeed
    CommandRequest request = CommandRequest.newBuilder()
        .setSignTx(CommandRequest.SignTxRequest.newBuilder()
          .addInputs(testInput())
          .addOutputs(testOutput()))
        .build();
    validateCommandRequest(request);

    // should fail because of prev hash in TxInput being too long
    CommandRequest request2 = CommandRequest.newBuilder()
        .setSignTx(CommandRequest.SignTxRequest.newBuilder()
          .addInputs(testInput(100000, longByteString))
          .addOutputs(testOutput()))
        .build();
    VerificationException e1 = assertThrows(VerificationException.class, () -> validateCommandRequest(request2));
    assertTrue(e1.getMessage().contains(ERROR_TXINPUT_PREV_HASH_SIZE));

    // this should fail due to having too many inputs
    List<TxInput> inputs = new ArrayList<>();
    for (int i = 0; i < 150; i++) {
      inputs.add(testInput());
    }
    CommandRequest request4 = CommandRequest.newBuilder()
        .setSignTx(CommandRequest.SignTxRequest.newBuilder()
          .addAllInputs(inputs)
          .addOutputs(testOutput()))
        .build();
    VerificationException e2 = assertThrows(VerificationException.class, () -> validateCommandRequest(request4));
    assertTrue(e2.getMessage().contains(ERROR_INPUTS_COUNT));

    // this should fail due to having too many outputs
    List<TxOutput> outputs = new ArrayList<>();
    for (int i = 0; i < 150; i++) {
      outputs.add(testOutput());
    }
    CommandRequest request5 = CommandRequest.newBuilder()
        .setSignTx(CommandRequest.SignTxRequest.newBuilder()
          .addInputs(testInput())
          .addAllOutputs(outputs))
        .build();
    VerificationException e3 = assertThrows(VerificationException.class, () -> validateCommandRequest(request5));
    assertTrue(e3.getMessage().contains(ERROR_OUTPUTS_COUNT));
  }

  private TxInput testInput() {
    return testInput(1000L, ByteString.copyFromUtf8("test prev hash"));
  }

  private TxInput testInput(long amount) {
    return testInput(amount, ByteString.copyFromUtf8("test prev hash"));
  }

  private TxInput testInput(long amount, ByteString prevHash) {
    return TxInput.newBuilder()
        .setAmount(amount)
        .setPrevHash(prevHash)
        .build();
  }

  private TxOutput testOutput() {
    return testOutput(990L, Destination.GATEWAY);
  }

  private TxOutput testOutput(long amount) {
    return testOutput(amount, Destination.GATEWAY);
  }

  private TxOutput testOutput(long amount, Destination destination) {
    return TxOutput.newBuilder()
        .setAmount(amount)
        .setDestination(destination)
        .setPath(Path.newBuilder()
            .setIsChange(destination == Destination.CHANGE)
            .setIndex(2))
        .build();
  }
}
