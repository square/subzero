package com.squareup.plutus.shared;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.squareup.protos.plutus.service.Common.Destination;
import com.squareup.protos.plutus.service.Common.EncryptedPubKey;
import com.squareup.protos.plutus.service.Common.Path;
import com.squareup.protos.plutus.service.Common.Signature;
import com.squareup.protos.plutus.service.Common.TxInput;
import com.squareup.protos.plutus.service.Common.TxOutput;
import com.squareup.protos.plutus.service.Service.CommandRequest;
import com.squareup.protos.plutus.service.Service.CommandResponse;
import java.util.LinkedList;
import java.util.List;
import org.bitcoinj.params.MainNetParams;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.spongycastle.util.encoders.Hex.toHexString;

/**
 * This tests show cases how to initialize, finalize and sign a transaction.
 *
 * The data corresponds to the following real transaction on mainnet:
 * https://live.blockcypher.com/btc/tx/607cf3511f0fa527b67d1790fa35d54c0d045c6cb65da1343fe103679e85dd9b/
 */
public class ColdWalletCreationAndSigningMainnetTest {
  private final int version = 204;
  private final int walletId = 1;

  @Test public void createInitWalletQrCode() {
    // The first QR code is the request to initialize a wallet
    CommandRequest commandRequest = CommandRequest.newBuilder()
        .setVersion(version)
        .setWalletId(walletId)
        .setInitWallet(CommandRequest.InitWalletRequest.newBuilder())
        .build();
    PlutusUtils.validateCommandRequest(commandRequest);

    String initWalletRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(initWalletRequest).isEqualTo("CMwBEAEaAA==");

    // This QR code is scanned on each offline machine, and results in 4 responses:
    // initWalletResponse1, initWalletResponse2, initWalletResponse3, and initWalletResponse4
  }

  @Test public void createFinalizeWalletQrCode() throws Exception {
    // We use the 4 initWalletResponses to create a finalize wallet QR code.
    String initWalletResponse1 = "CpEBCo4BCosB78s9CNqIKdNo3ECg+3XAif2n1dEQuirZJvNJUc2MZzZYBS6IFYQWozXNQTJNJba1l/niuLPOu0LznUtwltaRhb/UtumUDAZC3qv/jxKvdiUZG7obMxh9iaKN3CWJ80pObzkSRT9Z+zWNHUoGYFK9dpcutrHPLu6xt7ERGaaaLk1iEcBdEhPZMhXk/CIA";
    String initWalletResponse2 = "CpEBCo4BCosB4VzWQwKqdIc05locckbRCA+4qbp5vHCfhhi/Wpgz3X3g5jcdL5nakwJiC8jFPc1q4XT4Z4Z2JVEtcaXTAOpjHtTxeNx3llt41tAJezo4R7gNof26qFMdJxQsArpCXOJTGFNlH1lxeagzBY9Mk6c4jzGQfQSczFILPW6QjFdNz3tS7TAxDI5aynVeaSIA";
    String initWalletResponse3 = "CpEBCo4BCosBmIxd84uGaYiFsTyPngkB17cpTqgONk9mmGTCAjwFQzp76ummP6JMY0RM0SJa2DuB7chFekYTX1U9aDP0u6Ux6wyKIIVzMg+p1zwJPLpHqym7zpqickLLO7plIqE1T8nudskWfRFwObSFGVnTh+Bjho/ZUbCT3TP0GRI/L8pY2eORrG1NptAliDCX2CIA";
    String initWalletResponse4 = "CpEBCo4BCosB1QsbfTK+htOG4AOQMm7HW2lv1IGMhsCqSRpP9TP3jfGk/XD7z0PlLBydypais7s8TZHn0x/4wn3wu/r8s0jT6qHzkoftcQBVFNHW/UI3NQzmyEMVoaSel0cyaELPYQ5aU7cziRlFzK9MVvEslU/2SjL6k8VZrywb4pDrHFaoYNvFbdGentm6+DFc2iIA";

    EncryptedPubKey encPubKey1 = CommandResponse.parseFrom(Base64.decode(initWalletResponse1))
            .getInitWalletOrThrow()
            .getEncryptedPubKeyOrThrow();
    EncryptedPubKey encPubKey2 = CommandResponse.parseFrom(Base64.decode(initWalletResponse2))
        .getInitWalletOrThrow()
        .getEncryptedPubKeyOrThrow();
    EncryptedPubKey encPubKey3 = CommandResponse.parseFrom(Base64.decode(initWalletResponse3))
        .getInitWalletOrThrow()
        .getEncryptedPubKeyOrThrow();
    EncryptedPubKey encPubKey4 = CommandResponse.parseFrom(Base64.decode(initWalletResponse4))
        .getInitWalletOrThrow()
        .getEncryptedPubKeyOrThrow();

    CommandRequest commandRequest = CommandRequest.newBuilder()
        .setVersion(version)
        .setWalletId(walletId)
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addEncryptedPubKeys(encPubKey1)
            .addEncryptedPubKeys(encPubKey2)
            .addEncryptedPubKeys(encPubKey3)
            .addEncryptedPubKeys(encPubKey4))
        .build();
    PlutusUtils.validateCommandRequest(commandRequest);

    String finalizeWalletRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(finalizeWalletRequest).isEqualTo("CMwBEAEixAQKjgEKiwHvyz0I2ogp02jcQKD7dcCJ/afV0RC6Ktkm80lRzYxnNlgFLogVhBajNc1BMk0ltrWX+eK4s867QvOdS3CW1pGFv9S26ZQMBkLeq/+PEq92JRkbuhszGH2Joo3cJYnzSk5vORJFP1n7NY0dSgZgUr12ly62sc8u7rG3sREZppouTWIRwF0SE9kyFeT8Co4BCosB4VzWQwKqdIc05locckbRCA+4qbp5vHCfhhi/Wpgz3X3g5jcdL5nakwJiC8jFPc1q4XT4Z4Z2JVEtcaXTAOpjHtTxeNx3llt41tAJezo4R7gNof26qFMdJxQsArpCXOJTGFNlH1lxeagzBY9Mk6c4jzGQfQSczFILPW6QjFdNz3tS7TAxDI5aynVeaQqOAQqLAZiMXfOLhmmIhbE8j54JAde3KU6oDjZPZphkwgI8BUM6e+rppj+iTGNETNEiWtg7ge3IRXpGE19VPWgz9LulMesMiiCFczIPqdc8CTy6R6spu86aonJCyzu6ZSKhNU/J7nbJFn0RcDm0hRlZ04fgY4aP2VGwk90z9BkSPy/KWNnjkaxtTabQJYgwl9gKjgEKiwHVCxt9Mr6G04bgA5AybsdbaW/UgYyGwKpJGk/1M/eN8aT9cPvPQ+UsHJ3KlqKzuzxNkefTH/jCffC7+vyzSNPqofOSh+1xAFUU0db9Qjc1DObIQxWhpJ6XRzJoQs9hDlpTtzOJGUXMr0xW8SyVT/ZKMvqTxVmvLBvikOscVqhg28Vt0Z6e2br4MVza");

    // This QR code is scanned on each offline machine, and results in 4 responses:
    // finalizeWalletResponse1, finalizeWalletResponse2, finalizeWalletResponse3, and finalizeWalletResponse4
  }

  @Test public void deriveColdWalletAddress() throws Exception {
    // We use the 4 finalizeWalletResponses to derive a cold wallet address
    String finalizeWalletResponse1 = "EnEKb3hwdWI2OVljTjRiWTNLNHIzTlFNbWFWWkdOcnBzOTZjQ1ZSYUZES2RWUTVjS0xtOGNhUWVXZDhCYnVwUUd3WjVWODJ4R1NDN1VXbWkyQkFHZkdRVTh2cGFnMVhGaFBoRGRpcHNCQ3I0MTVldTNCaCIA";
    String finalizeWalletResponse2 = "EnEKb3hwdWI2OUttZ0R4cEhDYnRjM2tzY3BONE1LMnlYRVVhNDZ2c1hjZlp5aW9HYUpSMUF6M1ZpcHNwY3NYWG1tR1I0Mzc3S3ZMZEtRVHBZejgyZFgySjVybXAzYjgxRlM4YWVQMlZMNFlyUEFadUhmdSIA";
    String finalizeWalletResponse3 = "EnEKb3hwdWI2OFRuRUhnNjVuYUtrRkJ6UFBja0p2Y3Y1Zm9DWDdoUVh5bU5xNGRkRUVtd2M0WnBLZnR5S2VLeHZmUk15OE5UZk42U1dWTXNSa2dRQkRyVFVFTU5KZVBiQU1mbU1HeldjSjR4cTJqTjFMcSIA";
    String finalizeWalletResponse4 = "EnEKb3hwdWI2OGU3VkJ5ZXU2WU1YdExvdmd5TUJNQ2VxUkVBYTFkRVdOQWIydmJVUlBnSnNiNEtFWWd5YXFtUXAzNTFOVjlvdzQ5blNaODNSSFdVVG9iUUJYZDZzdmpKbnc0dkxiYUo3bXE1TkN1eFBnUiIA";

    String pubKey1 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse1))
        .getFinalizeWalletOrThrow()
        .getPubKeyOrThrow()
        .toStringUtf8();

    String pubKey2 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse2))
        .getFinalizeWalletOrThrow()
        .getPubKeyOrThrow()
        .toStringUtf8();

    String pubKey3 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse3))
        .getFinalizeWalletOrThrow()
        .getPubKeyOrThrow()
        .toStringUtf8();

    String pubKey4 = CommandResponse.parseFrom(Base64.decode(finalizeWalletResponse4))
        .getFinalizeWalletOrThrow()
        .getPubKeyOrThrow()
        .toStringUtf8();

    assertThat(pubKey1).isEqualTo("xpub69YcN4bY3K4r3NQMmaVZGNrps96cCVRaFDKdVQ5cKLm8caQeWd8BbupQGwZ5V82xGSC7UWmi2BAGfGQU8vpag1XFhPhDdipsBCr415eu3Bh");
    assertThat(pubKey2).isEqualTo("xpub69KmgDxpHCbtc3kscpN4MK2yXEUa46vsXcfZyioGaJR1Az3VipspcsXXmmGR4377KvLdKQTpYz82dX2J5rmp3b81FS8aeP2VL4YrPAZuHfu");
    assertThat(pubKey3).isEqualTo("xpub68TnEHg65naKkFBzPPckJvcv5foCX7hQXymNq4ddEEmwc4ZpKftyKeKxvfRMy8NTfN6SWVMsRkgQBDrTUEMNJePbAMfmMGzWcJ4xq2jN1Lq");
    assertThat(pubKey4).isEqualTo("xpub68e7VByeu6YMXtLovgyMBMCeqREAa1dEWNAb2vbURPgJsb4KEYgyaqmQp351NV9ow49nSZ83RHWUTobQBXd6svjJnw4vLbaJ7mq5NCuxPgR");

    // Path was picked randomly. It's important to never reuse addresses!
    Path path = Path.newBuilder()
        .setAccount(6211)
        .setIsChange(false)
        .setIndex(3172)
        .build();

    List<String> addresses = ImmutableList.of(pubKey1, pubKey2, pubKey3, pubKey4);
    String gateway = "xpub69kTAN2XXad5k39pCzEwBRwu76pX3kNUyPAgxe3vbgoZ4M4FzqxyeMe8FgsCSkTbT797YuP2u4Hhc4kgV8tAeiH7hFnk9obqxyJ7bzeuroC";

    ColdWallet coldWallet = new ColdWallet(MainNetParams.get(), walletId, addresses, gateway);
    String coldWalletAddress = coldWallet.address(path).toBase58();
    assertThat(coldWalletAddress).isEqualTo("3ASqAsrWKzV2qsGdKwJh7m9idjRtL3cKee");

    // You can see the transaction which funds this address here:
    // https://live.blockcypher.com/btc/tx/f9e21886a6b8216ec0717728ecc4433a0b765b0f81ea1d4cd5b982fcbf293f8d/
  }

  @Test public void createSignTxQrCode() {
    Path inputPath = Path.newBuilder()
        .setAccount(6211)
        .setIsChange(false)
        .setIndex(3172)
        .build();

    Path outputPath = Path.newBuilder()
        .setAccount(0)
        .setIsChange(false)
        .setIndex(0)
        .build();

    List<String> addresses = new LinkedList<>();
    addresses.add(
        "xpub69YcN4bY3K4r3NQMmaVZGNrps96cCVRaFDKdVQ5cKLm8caQeWd8BbupQGwZ5V82xGSC7UWmi2BAGfGQU8vpag1XFhPhDdipsBCr415eu3Bh");
    addresses.add(
        "xpub69KmgDxpHCbtc3kscpN4MK2yXEUa46vsXcfZyioGaJR1Az3VipspcsXXmmGR4377KvLdKQTpYz82dX2J5rmp3b81FS8aeP2VL4YrPAZuHfu");
    addresses.add(
        "xpub68TnEHg65naKkFBzPPckJvcv5foCX7hQXymNq4ddEEmwc4ZpKftyKeKxvfRMy8NTfN6SWVMsRkgQBDrTUEMNJePbAMfmMGzWcJ4xq2jN1Lq");
    addresses.add(
        "xpub68e7VByeu6YMXtLovgyMBMCeqREAa1dEWNAb2vbURPgJsb4KEYgyaqmQp351NV9ow49nSZ83RHWUTobQBXd6svjJnw4vLbaJ7mq5NCuxPgR");

    String gateway = "xpub69kTAN2XXad5k39pCzEwBRwu76pX3kNUyPAgxe3vbgoZ4M4FzqxyeMe8FgsCSkTbT797YuP2u4Hhc4kgV8tAeiH7hFnk9obqxyJ7bzeuroC";

    ColdWallet coldWallet = new ColdWallet(MainNetParams.get(), walletId, addresses, gateway);
    CommandRequest commandRequest = coldWallet.startTransaction(version,
        ImmutableList.of(TxInput.newBuilder()
            .setPrevHash(ByteString.copyFrom(
                Hex.decode("f9e21886a6b8216ec0717728ecc4433a0b765b0f81ea1d4cd5b982fcbf293f8d")))
            .setPrevIndex(1)
            .setAmount(100000)
            .setPath(inputPath)
            .build()),
        ImmutableList.of(TxOutput.newBuilder()
            .setAmount(93200)
            .setDestination(Destination.GATEWAY)
            .setPath(outputPath)
            .build()),
        null);
    PlutusUtils.validateCommandRequest(commandRequest);

    String signTxRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(signTxRequest).isEqualTo("CMwBEAEqRgoyCiD54hiGprghbsBxdyjsxEM6C3ZbD4HqHUzVuYL8vyk/jRABGKCNBiIICMMwEAAY5BgSDgiQ2AUQAhoGCAAQABgAGAA=");

    // This QR code is scanned on two offline machine, and results in 2 responses:
    // signTxResponse1, and signTxResponse2
  }

  @Test public void createTransaction() throws Exception {
    // We use signTxResponse1 and signTxResponse2 to create the transaction.
    // The gateway address we use here must match what's hardcoded in Plutus' CodeSafe module or
    // the signature will fail to verify.
    CommandRequest signTxRequest = CommandRequest.parseFrom(Base64.decode(
        "CMwBEAEqRgoyCiD54hiGprghbsBxdyjsxEM6C3ZbD4HqHUzVuYL8vyk/jRABGKCNBiIICMMwEAAY5BgSDgiQ2AUQAhoGCAAQABgAGAA="));

    String signTxResponse1 =
        "Gm0KawpHMEUCIQCJJWmmh+4E+crFreeG5wfO4T8YDxpkVQa6UraQGGZt6AIgNfWa50vuoEwtLd1nc5O6yNpMOmU+WVrGMFkOU5SWStwSIK6oB2Egliv9dn0Zu7s5ZBwaTLkEkYZ8/Y5C7uCY0vnKIgA=";
    String signTxResponse2 =
        "Gm0KawpHMEUCIQDHKMVUWwHGFj7ed9qA2EeHmQ3hsUffFCqOf1KuxeUC/gIgevf1GKX0PuYuzEJhgwOtTUwCZtaPJqljfodbmgGcFUsSIK6oB2Egliv9dn0Zu7s5ZBwaTLkEkYZ8/Y5C7uCY0vnKIgA=";

    CommandResponse sig1 = CommandResponse.parseFrom(Base64.decode(signTxResponse1));
    CommandResponse sig2 = CommandResponse.parseFrom(Base64.decode(signTxResponse2));

    List<String> addresses = new LinkedList<>();
    addresses.add(
        "xpub69YcN4bY3K4r3NQMmaVZGNrps96cCVRaFDKdVQ5cKLm8caQeWd8BbupQGwZ5V82xGSC7UWmi2BAGfGQU8vpag1XFhPhDdipsBCr415eu3Bh");
    addresses.add(
        "xpub69KmgDxpHCbtc3kscpN4MK2yXEUa46vsXcfZyioGaJR1Az3VipspcsXXmmGR4377KvLdKQTpYz82dX2J5rmp3b81FS8aeP2VL4YrPAZuHfu");
    addresses.add(
        "xpub68TnEHg65naKkFBzPPckJvcv5foCX7hQXymNq4ddEEmwc4ZpKftyKeKxvfRMy8NTfN6SWVMsRkgQBDrTUEMNJePbAMfmMGzWcJ4xq2jN1Lq");
    addresses.add(
        "xpub68e7VByeu6YMXtLovgyMBMCeqREAa1dEWNAb2vbURPgJsb4KEYgyaqmQp351NV9ow49nSZ83RHWUTobQBXd6svjJnw4vLbaJ7mq5NCuxPgR");

    String gateway = "xpub69kTAN2XXad5k39pCzEwBRwu76pX3kNUyPAgxe3vbgoZ4M4FzqxyeMe8FgsCSkTbT797YuP2u4Hhc4kgV8tAeiH7hFnk9obqxyJ7bzeuroC";

    List<List<Signature>> signatures = ImmutableList.of(sig1.getSignTx().getSignaturesList(), sig2.getSignTx().getSignaturesList());

    ColdWallet coldWallet = new ColdWallet(MainNetParams.get(), walletId, addresses, gateway);
    String transaction =
        toHexString(coldWallet.createTransaction(signTxRequest.getSignTxOrThrow().getInputsList(),
            signTxRequest.getSignTxOrThrow().getOutputsList(), signatures));
    assertThat(transaction).isEqualTo(
        "010000000001018d3f29bffc82b9d54c1dea810f5b760b3a43c4ec287771c06e21b8a68618e2f90100000023220020dcf584a95e6a494dcb5649c7509e85d3f6636666be4c4bd5a9364da7de5c3aa8feffffff01106c0100000000001976a914549d588666a4e07bd6f51a7668d6053edc75b80388ac0400483045022100c728c5545b01c6163ede77da80d84787990de1b147df142a8e7f52aec5e502fe02207af7f518a5f43ee62ecc42618303ad4d4c0266d68f26a9637e875b9a019c154b01483045022100892569a687ee04f9cac5ade786e707cee13f180f1a645506ba52b69018666de8022035f59ae74beea04c2d2ddd677393bac8da4c3a653e595ac630590e5394964adc018b522102da4cf48efc2850c65ba457b2a8061eea39055fbdcc874cb949eccdf535127f4e2102fceb5a84868c4247c7c75cf376cc102aa930f33e65f40d3694aab5fb874f574e21033a866b1b0b3466a5d121201f2bdd853b50bdbe8cf447dbf5b4cd20068c9d3c13210363481eaac39ca765d6262bc63ec47baded5c6d7bfaf539e8eca4c6de6bec447254ae00000000");

    // You can see this transaction here:
    // https://live.blockcypher.com/btc/tx/607cf3511f0fa527b67d1790fa35d54c0d045c6cb65da1343fe103679e85dd9b/
  }
}
