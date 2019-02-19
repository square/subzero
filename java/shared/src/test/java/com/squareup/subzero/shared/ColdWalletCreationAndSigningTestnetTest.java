package com.squareup.subzero.shared;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.squareup.protos.subzero.service.Common.Destination;
import com.squareup.protos.subzero.service.Common.EncryptedPubKey;
import com.squareup.protos.subzero.service.Common.Path;
import com.squareup.protos.subzero.service.Common.Signature;
import com.squareup.protos.subzero.service.Common.TxInput;
import com.squareup.protos.subzero.service.Common.TxOutput;
import com.squareup.protos.subzero.service.Service.CommandRequest;
import com.squareup.protos.subzero.service.Service.CommandResponse;
import java.util.LinkedList;
import java.util.List;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Test;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.spongycastle.util.encoders.Hex.toHexString;

/**
 * This tests show cases how to initialize, finalize and sign a transaction.
 *
 * Compared to ColdWalletCreationAndSigningMainnetTest, we broadcast the transaction on testnet.
 * This test is also slightly more complicated, there are two transactions:
 *
 * transaction 1: creates two change outputs.
 * - 1 input from cold wallet (1 non-change)
 * - 2 outputs (2 change, 0 gateway)
 *
 * transaction 2:
 * - 4 inputs from cold wallet (2 non-change, 2 change)
 * - 3 outputs (1 change, 2 gateway)
 */
public class ColdWalletCreationAndSigningTestnetTest {
  private final int walletId = 20190131;

  @Test public void createInitWalletRequest() {
    // The first QR code is the request to initialize a wallet
    CommandRequest commandRequest = CommandRequest.newBuilder()
        .setWalletId(walletId)
        .setInitWallet(CommandRequest.InitWalletRequest.newBuilder())
        .build();
    SubzeroUtils.validateCommandRequest(commandRequest);

    String initWalletRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(initWalletRequest).isEqualTo("ELOn0AkaAA==");

    // This QR code is scanned on each offline machine, and results in 4 responses:
    // initWalletResponse1, initWalletResponse2, initWalletResponse3, and initWalletResponse4
  }

  @Test public void createFinalizeWalletRequest() throws Exception {
    // We use the 4 initWalletResponses to create a finalize wallet QR code.

    String initWalletResponse1 = "CnMKcQpv3trfyO7rmejt3uTg75nF/PnL2cDS8uH6zMXm79Lp/fOd4ennn9PQkt7vzcPI0M3n8Mv455jFxfn9/v2fn9PQne783vvtk57o/OSd//PFx97Tw+7bwvjDydyTy97r5t7N0Pze7vvS/u/8/JjdwOTZIgA=";
    String initWalletResponse2 = "CnMKcQpv3trfyO6T35nh2ZL80sju2/PNy8X/zO79xPjzw+7QwPOSw97vnPzN4MHtx8jt0OHS+sH5yObF0sL54pP66cHO79zowZmf2Nvp787fnczs2u2c38yY/8efxenzxeCS5P/gm/3Nzc76mcHgmfmZ/PrzIgA=";
    String initWalletResponse3 = "CnMKcQpv3trfyO6SxPvtyfzm0/v++57k0vzC6/Pv3O/e//jFneif+sX+w+3Zm8/Qktzh4sj86PvO0JKZwtLHzdzA+MXw6dLJ4suTwpzzmeL7wsCc7P3w6e/w3pv4wc/d/5PDk9jYz5jCwpjh2fPrzvLPws/aIgA=";
    String initWalletResponse4 = "CnMKcQpv3trfyO7rn8jE8M3fzdjFzZLS3fvd//7k7e34xPn78OL5w9vPm8Dtw9zawfDg6ZL67Jj/ycXB4J/yxZ/Qku/c7p7o3cfQ6eLOmPzD2dLQyf/NneLBy9LT0sybnZ3O2vz63uf9mO+cxJnDwNn6zcHPIgA=";

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
        .setWalletId(walletId)
        .setFinalizeWallet(CommandRequest.FinalizeWalletRequest.newBuilder()
            .addEncryptedPubKeys(encPubKey1)
            .addEncryptedPubKeys(encPubKey2)
            .addEncryptedPubKeys(encPubKey3)
            .addEncryptedPubKeys(encPubKey4))
        .build();
    SubzeroUtils.validateCommandRequest(commandRequest);

    String finalizeWalletRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(finalizeWalletRequest).isEqualTo("ELOn0AkizAMKcQpv3trfyO7rmejt3uTg75nF/PnL2cDS8uH6zMXm79Lp/fOd4ennn9PQkt7vzcPI0M3n8Mv455jFxfn9/v2fn9PQne783vvtk57o/OSd//PFx97Tw+7bwvjDydyTy97r5t7N0Pze7vvS/u/8/JjdwOTZCnEKb97a38juk9+Z4dmS/NLI7tvzzcvF/8zu/cT488Pu0MDzksPe75z8zeDB7cfI7dDh0vrB+cjmxdLC+eKT+unBzu/c6MGZn9jb6e/O353M7NrtnN/MmP/Hn8Xp88XgkuT/4Jv9zc3O+pnB4Jn5mfz68wpxCm/e2t/I7pLE++3J/ObT+/77nuTS/MLr8+/c797/+MWd6J/6xf7D7dmbz9CS3OHiyPzo+87QkpnC0sfN3MD4xfDp0sniy5PCnPOZ4vvCwJzs/fDp7/Dem/jBz93/k8OT2NjPmMLCmOHZ8+vO8s/Cz9oKcQpv3trfyO7rn8jE8M3fzdjFzZLS3fvd//7k7e34xPn78OL5w9vPm8Dtw9zawfDg6ZL67Jj/ycXB4J/yxZ/Qku/c7p7o3cfQ6eLOmPzD2dLQyf/NneLBy9LT0sybnZ3O2vz63uf9mO+cxJnDwNn6zcHP");

    // This QR code is scanned on each offline machine, and results in 4 responses:
    // finalizeWalletResponse1, finalizeWalletResponse2, finalizeWalletResponse3, and finalizeWalletResponse4
  }

  @Test public void deriveColdWalletAddress() throws Exception {
    // We use the 4 finalizeWalletResponses to derive a cold wallet address
    String finalizeWalletResponse1 = "EnEKb3RwdWJEQTNCR3ROSkUzb1ZTYXNqeFhLUGZvTEV4Q1dZN0tDTTV5ejh0RWdpYnpnTVphUk0yb29TV1RXNTV5ejdEVnRRRzk0QlZON1VZb210eWlEcWhSaWN2OWF0QUx0Z3pWdERReFRFVlYyd2pOcyIA";
    String finalizeWalletResponse2 = "EnEKb3RwdWJEOXUzS3M4VnhiRHFZZ2FvVWZEV25SWWlEempZOGl0RTZWZ0prR21iR3pLeFBrU2JMb3hoU0g5UENrZEV2QmszNXJxQ0VkdTdmRnBHNnVmMlVtNW9DWW9KOE5VSjFXZ2dkUDNrSjNTM1ZQWSIA";
    String finalizeWalletResponse3 = "EnEKb3RwdWJEOG5RR2NWTHlRVFE0TnhWaEFZRXZFdFVSbzdCNVBvVGlHczFlejh2S0hiVkJRZHo4M2h4bWd2alJvWkN4Y0hhOWg2WTNIUWhqNkZXWkNFWnQxUmtld1U5aTlycmUyaGgyS3NZQWRYZWhlcCIA";
    String finalizeWalletResponse4 = "EnEKb3RwdWJEQTViblpndWdyb2c4eHdRd1VUTkdHUm5TUVpIU2lxZTFqR2l2cGtaSkM4UEYyVWNva0o1WG81ejhFdkQ0QndtekNIZDJWaXN4emNVZzdIa2F4eXhmMTc3ZHBWUHRNVzJFNm4zaWpzUGdrZSIA";

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

    assertThat(pubKey1).isEqualTo("tpubDA3BGtNJE3oVSasjxXKPfoLExCWY7KCM5yz8tEgibzgMZaRM2ooSWTW55yz7DVtQG94BVN7UYomtyiDqhRicv9atALtgzVtDQxTEVV2wjNs");
    assertThat(pubKey2).isEqualTo("tpubD9u3Ks8VxbDqYgaoUfDWnRYiDzjY8itE6VgJkGmbGzKxPkSbLoxhSH9PCkdEvBk35rqCEdu7fFpG6uf2Um5oCYoJ8NUJ1WggdP3kJ3S3VPY");
    assertThat(pubKey3).isEqualTo("tpubD8nQGcVLyQTQ4NxVhAYEvEtURo7B5PoTiGs1ez8vKHbVBQdz83hxmgvjRoZCxcHa9h6Y3HQhj6FWZCEZt1RkewU9i9rre2hh2KsYAdXehep");
    assertThat(pubKey4).isEqualTo("tpubDA5bnZgugrog8xwQwUTNGGRnSQZHSiqe1jGivpkZJC8PF2UcokJ5Xo5z8EvD4BwmzCHd2VisxzcUg7Hkaxyxf177dpVPtMW2E6n3ijsPgke");

    // Path was picked randomly. It's important to never reuse addresses!
    Path path = Path.newBuilder()
        .setIsChange(false)
        .setIndex(8011)
        .build();

    List<String> addresses = ImmutableList.of(pubKey1, pubKey2, pubKey3, pubKey4);
    String gateway = "tpubD9dXq2iRd5PppWdv38THfp9LLoBTU94RdpZohhsN4XohgVXykzhzvZWsbJa8k6yq21tnaB6w4wCMJPZ9wF8ed677CRTw8cvBw15XTLB3KHg";

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    String coldWalletAddress = coldWallet.address(path).toBase58();
    assertThat(coldWalletAddress).isEqualTo("2MuvzgSMJ5N8nVcosVQxLXjCmNsUwiNyPVE");

    // You can see the transaction which funds this address here:
    // https://live.blockcypher.com/btc-testnet/tx/21cc9d1a2d699726a1a54266c76683352c223cc5c6ed458fdf1448650aca081d/
  }

  @Test public void createSignTxRequest1() {
    Path inputPath = Path.newBuilder()
        .setIsChange(false)
        .setIndex(8011)
        .build();

    Path outputPath1 = Path.newBuilder()
        .setIsChange(true)
        .setIndex(4284)
        .build();

    Path outputPath2 = Path.newBuilder()
        .setIsChange(true)
        .setIndex(4285)
        .build();

    List<String> addresses = new LinkedList<>();
    addresses.add("tpubDA3BGtNJE3oVSasjxXKPfoLExCWY7KCM5yz8tEgibzgMZaRM2ooSWTW55yz7DVtQG94BVN7UYomtyiDqhRicv9atALtgzVtDQxTEVV2wjNs");
    addresses.add("tpubD9u3Ks8VxbDqYgaoUfDWnRYiDzjY8itE6VgJkGmbGzKxPkSbLoxhSH9PCkdEvBk35rqCEdu7fFpG6uf2Um5oCYoJ8NUJ1WggdP3kJ3S3VPY");
    addresses.add("tpubD8nQGcVLyQTQ4NxVhAYEvEtURo7B5PoTiGs1ez8vKHbVBQdz83hxmgvjRoZCxcHa9h6Y3HQhj6FWZCEZt1RkewU9i9rre2hh2KsYAdXehep");
    addresses.add("tpubDA5bnZgugrog8xwQwUTNGGRnSQZHSiqe1jGivpkZJC8PF2UcokJ5Xo5z8EvD4BwmzCHd2VisxzcUg7Hkaxyxf177dpVPtMW2E6n3ijsPgke");

    String gateway = "tpubD9dXq2iRd5PppWdv38THfp9LLoBTU94RdpZohhsN4XohgVXykzhzvZWsbJa8k6yq21tnaB6w4wCMJPZ9wF8ed677CRTw8cvBw15XTLB3KHg";

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    CommandRequest commandRequest = coldWallet.startTransaction(
        ImmutableList.of(TxInput.newBuilder()
            .setPrevHash(ByteString.copyFrom(
                Hex.decode("21cc9d1a2d699726a1a54266c76683352c223cc5c6ed458fdf1448650aca081d")))
            .setPrevIndex(0)
            .setAmount(130000000)
            .setPath(inputPath)
            .build()),
        ImmutableList.of(
            TxOutput.newBuilder()
                .setAmount(43000000)
                .setDestination(Destination.CHANGE)
                .setPath(outputPath1)
                .build(),
            TxOutput.newBuilder()
                .setAmount(86000000)
                .setDestination(Destination.CHANGE)
                .setPath(outputPath2)
                .build()
            ),
        null, null);
    SubzeroUtils.validateCommandRequest(commandRequest);

    String signTxRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(signTxRequest).isEqualTo("ELOn0AkqVAowCiAhzJ0aLWmXJqGlQmbHZoM1LCI8xcbtRY/fFEhlCsoIHRAAGIDJ/j0iBRAAGMs+Eg4IwMHAFBABGgUQARi8IRIOCICDgSkQARoFEAEYvSEYAA==");

    // This QR code is scanned on two offline machine, and results in 2 responses:
    // signTxResponse1, and signTxResponse2
  }

  @Test public void createTransaction1() throws Exception {
    // We use signTxResponse1 and signTxResponse2 to create the transaction.
    // The gateway address we use here must match what's hardcoded in Subzero's CodeSafe module or
    // the signature will fail to verify.
    CommandRequest signTxRequest = CommandRequest.parseFrom(Base64.decode(
        "ELOn0AkqVAowCiAhzJ0aLWmXJqGlQmbHZoM1LCI8xcbtRY/fFEhlCsoIHRAAGIDJ/j0iBRAAGMs+Eg4IwMHAFBABGgUQARi8IRIOCICDgSkQARoFEAEYvSEYAA=="));

    String signTxResponse1 = "GmwKagpGMEQCICzE1US9DJ788oeUkdDtr+swTzhoCyfi2kHElIIVrzWGAiBN4NDW5iPMxRoyFRBB4PqKtKwhFxBrpLtpwBMhviSxJxIgT1ukmqKbDPGIC8feem9Z+V8KL/DSrRocHM31PbAK4+EiAA==";
    String signTxResponse2 = "Gm0KawpHMEUCIQCEKXu9afndhI2aVHXHr1EMHXgKfyJ3BfY1CgLOSsHH8gIgA/DvlFq0iDzpmWyZ3samAJ3z+9vGYvb+wI8ocPkiO84SIE9bpJqimwzxiAvH3npvWflfCi/w0q0aHBzN9T2wCuPhIgA=";

    CommandResponse sig1 = CommandResponse.parseFrom(Base64.decode(signTxResponse1));
    CommandResponse sig2 = CommandResponse.parseFrom(Base64.decode(signTxResponse2));

    List<String> addresses = new LinkedList<>();
    addresses.add("tpubDA3BGtNJE3oVSasjxXKPfoLExCWY7KCM5yz8tEgibzgMZaRM2ooSWTW55yz7DVtQG94BVN7UYomtyiDqhRicv9atALtgzVtDQxTEVV2wjNs");
    addresses.add("tpubD9u3Ks8VxbDqYgaoUfDWnRYiDzjY8itE6VgJkGmbGzKxPkSbLoxhSH9PCkdEvBk35rqCEdu7fFpG6uf2Um5oCYoJ8NUJ1WggdP3kJ3S3VPY");
    addresses.add("tpubD8nQGcVLyQTQ4NxVhAYEvEtURo7B5PoTiGs1ez8vKHbVBQdz83hxmgvjRoZCxcHa9h6Y3HQhj6FWZCEZt1RkewU9i9rre2hh2KsYAdXehep");
    addresses.add("tpubDA5bnZgugrog8xwQwUTNGGRnSQZHSiqe1jGivpkZJC8PF2UcokJ5Xo5z8EvD4BwmzCHd2VisxzcUg7Hkaxyxf177dpVPtMW2E6n3ijsPgke");

    String gateway = "tpubD9dXq2iRd5PppWdv38THfp9LLoBTU94RdpZohhsN4XohgVXykzhzvZWsbJa8k6yq21tnaB6w4wCMJPZ9wF8ed677CRTw8cvBw15XTLB3KHg";

    List<List<Signature>> signatures = ImmutableList.of(sig1.getSignTx().getSignaturesList(), sig2.getSignTx().getSignaturesList());

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    String transaction =
        toHexString(coldWallet.createTransaction(signTxRequest.getSignTxOrThrow().getInputsList(),
            signTxRequest.getSignTxOrThrow().getOutputsList(), signatures));
    assertThat(transaction).isEqualTo(
        "010000000001011d08ca0a654814df8f45edc6c53c222c358366c76642a5a12697692d1a9dcc21000000002322002089d07657310b60a144a5c6bab92083f69eff86650dda61d287d632c68cf12b68feffffff02c02090020000000017a914c0989ed5ab5a3c6568b41c902f37a5b1fdc3793887804120050000000017a914260cd6a84fcf10bf0d4101ede0452ff5265ea59187040048304502210084297bbd69f9dd848d9a5475c7af510c1d780a7f227705f6350a02ce4ac1c7f2022003f0ef945ab4883ce9996c99dec6a6009df3fbdbc662f6fec08f2870f9223bce0147304402202cc4d544bd0c9efcf2879491d0edafeb304f38680b27e2da41c4948215af358602204de0d0d6e623ccc51a32151041e0fa8ab4ac2117106ba4bb69c01321be24b127018b5221020a6e06b48f1e4806ba94435aaf7b55190bc8295ff63ca40264dfcae3148451a321024826a65c1db382cebc2fa93be6bac859d1dc74f5374eaa016c41cdb08c17a45721038858f3df69e3aa0289129ffea09c3819700eb926b7e09ae36bac3e50c19a72172103c83e35763df6941fcd3b9ef0f754f222ce95c8adc5deef65cf968af9e1162c4b54ae00000000");

    // We used https://testnet.smartbit.com.au/txs/pushtx to broadcast the transaction. You can see
    // this transaction here:
    // https://live.blockcypher.com/btc-testnet/tx/d8b2819b24ea90042e6381763027fffbd66d7e8ab4e1c00371d7d9040303d1bc/
  }

  @Test public void createSignTxRequest2() {
    // Additional transactions to fund the cold wallet:
    // - 1.3 BTC to 2My2hPyZ4scGqyXGSrAsjun85V8SszMimJ5
    //   https://live.blockcypher.com/btc-testnet/tx/7de5842a856d77e1a13fb4fdcc65b0ce6b16f8af339c9d6c7f764bfbdd159610/
    // - 0.65 BTC to 2N4fS6UC4ffMmhEc42Yv6rfuLLLKmN9uB4K
    //   https://live.blockcypher.com/btc-testnet/tx/afff3f91c9d1d96c98e82ec1eb0ac9abc573b95f0c705777cf5ca9164372ea31/


    Path inputPath1 = Path.newBuilder()
        .setIsChange(false)
        .setIndex(8012)
        .build();

    Path inputPath2 = Path.newBuilder()
        .setIsChange(false)
        .setIndex(8013)
        .build();

    Path inputPath3 = Path.newBuilder()
        .setIsChange(true)
        .setIndex(4284)
        .build();

    Path inputPath4 = Path.newBuilder()
        .setIsChange(true)
        .setIndex(4285)
        .build();

    Path outputPath1 = Path.newBuilder()
        .setIsChange(false)
        .setIndex(7836)
        .build();

    Path outputPath2 = Path.newBuilder()
        .setIsChange(false)
        .setIndex(7837)
        .build();


    List<String> addresses = new LinkedList<>();
    addresses.add("tpubDA3BGtNJE3oVSasjxXKPfoLExCWY7KCM5yz8tEgibzgMZaRM2ooSWTW55yz7DVtQG94BVN7UYomtyiDqhRicv9atALtgzVtDQxTEVV2wjNs");
    addresses.add("tpubD9u3Ks8VxbDqYgaoUfDWnRYiDzjY8itE6VgJkGmbGzKxPkSbLoxhSH9PCkdEvBk35rqCEdu7fFpG6uf2Um5oCYoJ8NUJ1WggdP3kJ3S3VPY");
    addresses.add("tpubD8nQGcVLyQTQ4NxVhAYEvEtURo7B5PoTiGs1ez8vKHbVBQdz83hxmgvjRoZCxcHa9h6Y3HQhj6FWZCEZt1RkewU9i9rre2hh2KsYAdXehep");
    addresses.add("tpubDA5bnZgugrog8xwQwUTNGGRnSQZHSiqe1jGivpkZJC8PF2UcokJ5Xo5z8EvD4BwmzCHd2VisxzcUg7Hkaxyxf177dpVPtMW2E6n3ijsPgke");

    String gateway = "tpubD9dXq2iRd5PppWdv38THfp9LLoBTU94RdpZohhsN4XohgVXykzhzvZWsbJa8k6yq21tnaB6w4wCMJPZ9wF8ed677CRTw8cvBw15XTLB3KHg";

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    CommandRequest commandRequest = coldWallet.startTransaction(
        ImmutableList.of(
            TxInput.newBuilder()
                .setPrevHash(ByteString.copyFrom(Hex.decode("7de5842a856d77e1a13fb4fdcc65b0ce6b16f8af339c9d6c7f764bfbdd159610")))
                .setPrevIndex(0)
                .setAmount(130000000)
                .setPath(inputPath1)
                .build(),
            TxInput.newBuilder()
                .setPrevHash(ByteString.copyFrom(Hex.decode("afff3f91c9d1d96c98e82ec1eb0ac9abc573b95f0c705777cf5ca9164372ea31")))
                .setPrevIndex(1)
                .setAmount(65000000)
                .setPath(inputPath2)
                .build(),
            TxInput.newBuilder()
                .setPrevHash(ByteString.copyFrom(Hex.decode("d8b2819b24ea90042e6381763027fffbd66d7e8ab4e1c00371d7d9040303d1bc")))
                .setPrevIndex(0)
                .setAmount(43000000)
                .setPath(inputPath3)
                .build(),
            TxInput.newBuilder()
                .setPrevHash(ByteString.copyFrom(Hex.decode("d8b2819b24ea90042e6381763027fffbd66d7e8ab4e1c00371d7d9040303d1bc")))
                .setPrevIndex(1)
                .setAmount(86000000)
                .setPath(inputPath4)
                .build()),
        ImmutableList.of(
            TxOutput.newBuilder()
                .setAmount(107666666)
                .setDestination(Destination.GATEWAY)
                .setPath(outputPath1)
                .build(),
            TxOutput.newBuilder()
                .setAmount(215333332)
                .setDestination(Destination.GATEWAY)
                .setPath(outputPath2)
                .build()
        ),
        null, null);
    SubzeroUtils.validateCommandRequest(commandRequest);

    String signTxRequest = Base64.toBase64String(commandRequest.toByteArray());
    assertThat(signTxRequest).isEqualTo("ELOn0Akq6gEKMAogfeWEKoVtd+GhP7T9zGWwzmsW+K8znJ1sf3ZL+90VlhAQABiAyf49IgUQABjMPgowCiCv/z+RydHZbJjoLsHrCsmrxXO5XwxwV3fPXKkWQ3LqMRABGMCk/x4iBRAAGM0+CjAKINiygZsk6pAELmOBdjAn//vWbX6KtOHAA3HX2QQDA9G8EAAYwMHAFCIFEAEYvCEKMAog2LKBmyTqkAQuY4F2MCf/+9Ztfoq04cADcdfZBAMD0bwQARiAg4EpIgUQARi9IRIOCOq5qzMQAhoFEAAYnD0SDgjU89ZmEAIaBRAAGJ09GAA=");

    // This QR code is scanned on two offline machine, and results in 2 responses:
    // signTxResponse1, and signTxResponse2
  }

  @Test public void createTransaction2() throws Exception {
    // We use signTxResponse1 and signTxResponse2 to create the transaction.
    // The gateway address we use here must match what's hardcoded in Subzero's CodeSafe module or
    // the signature will fail to verify.
    CommandRequest signTxRequest = CommandRequest.parseFrom(Base64.decode("ELOn0Akq6gEKMAogfeWEKoVtd+GhP7T9zGWwzmsW+K8znJ1sf3ZL+90VlhAQABiAyf49IgUQABjMPgowCiCv/z+RydHZbJjoLsHrCsmrxXO5XwxwV3fPXKkWQ3LqMRABGMCk/x4iBRAAGM0+CjAKINiygZsk6pAELmOBdjAn//vWbX6KtOHAA3HX2QQDA9G8EAAYwMHAFCIFEAEYvCEKMAog2LKBmyTqkAQuY4F2MCf/+9Ztfoq04cADcdfZBAMD0bwQARiAg4EpIgUQARi9IRIOCOq5qzMQAhoFEAAYnD0SDgjU89ZmEAIaBRAAGJ09GAA="));

    String signTxResponse1 = "GrEDCmoKRjBEAiAxFZc1Rr9F5OwrnJoyt494QdmNs18WJyZYd8Jqnjl1fQIgPnPGfhJ8kuJfmoivQNIjK6brezizzwHeF2qfOmZ7G4wSIDRPcNKSVjwxVPduv4xNiVgO6LlEiCBnS48SZE6UyUuVCmoKRjBEAiAjnJD1b6Z4k6uU3orfbk2KQ3aWNE6gz1uAt1C/c5LstgIgFuG+xX1U4QdLxVTlxOXYoVBl67rILqzpE66d3cBFUjoSIHJRpKjutyaog8S3vY6mP9OpDKG7SKzs1CwZXLViPYm1CmoKRjBEAiAG/TbQW3wNLDPEPy+zm2NMuuAp2vGcy4O91xLf5eU9zQIgB5Os1Ls4M6Nd7kyVluRSODXAMDeWa5a7cIZ3K8oSlAgSIFZP1ltnnmP61mkgh078h+mgXqp0id45BuBFSlgJ5Xs7CmsKRzBFAiEAjg2pgeVx3Xc81cjz/73GyvN1P9ETDNA76q0vwnNxNA8CIG3xlWyiop9YO9iH0wvCXz6KavLv+expb2YW0RxkksHKEiBQsNc6p3Z0oizmCcqIMSjQM/syLPkjxzQCaP80L4KSZiIA";
    String signTxResponse2 = "GrEDCmoKRjBEAiBbOvoI5LxRhNT7yDQraVkK0gItwUbq7hvY9tvs0FvsEQIgFJU4ACEY8LjjY/n4kefzSi5rNYJfaG8P0E+G+XuRjMoSIDRPcNKSVjwxVPduv4xNiVgO6LlEiCBnS48SZE6UyUuVCmoKRjBEAiBUGb+Uto/nD9C7oYuKFgOTLNNdq2OvwyUNQL553TMGtwIgBZJxQ/UExZhij68pnzC123GJPEy+lq5nDfV/HuCae/YSIHJRpKjutyaog8S3vY6mP9OpDKG7SKzs1CwZXLViPYm1CmoKRjBEAiALzICK7ILuwcavKbVr2Y5PWaSqh51Gib8KvO8hRtLKAwIgLRN46DzTHkPnZvVUR9QqJ9IDn/C1uGDC6ENjGIlj5aYSIFZP1ltnnmP61mkgh078h+mgXqp0id45BuBFSlgJ5Xs7CmsKRzBFAiEA5YTxkG+rrh4ydN0L9ibYC8rXuUg5S9IHIm8H06eP3SoCICGxp3J2rwHb1fQhx7CAOS2aZlY2EmI0JjjHybT9rmbuEiBQsNc6p3Z0oizmCcqIMSjQM/syLPkjxzQCaP80L4KSZiIA";

    CommandResponse sig1 = CommandResponse.parseFrom(Base64.decode(signTxResponse1));
    CommandResponse sig2 = CommandResponse.parseFrom(Base64.decode(signTxResponse2));

    List<String> addresses = new LinkedList<>();
    addresses.add("tpubDA3BGtNJE3oVSasjxXKPfoLExCWY7KCM5yz8tEgibzgMZaRM2ooSWTW55yz7DVtQG94BVN7UYomtyiDqhRicv9atALtgzVtDQxTEVV2wjNs");
    addresses.add("tpubD9u3Ks8VxbDqYgaoUfDWnRYiDzjY8itE6VgJkGmbGzKxPkSbLoxhSH9PCkdEvBk35rqCEdu7fFpG6uf2Um5oCYoJ8NUJ1WggdP3kJ3S3VPY");
    addresses.add("tpubD8nQGcVLyQTQ4NxVhAYEvEtURo7B5PoTiGs1ez8vKHbVBQdz83hxmgvjRoZCxcHa9h6Y3HQhj6FWZCEZt1RkewU9i9rre2hh2KsYAdXehep");
    addresses.add("tpubDA5bnZgugrog8xwQwUTNGGRnSQZHSiqe1jGivpkZJC8PF2UcokJ5Xo5z8EvD4BwmzCHd2VisxzcUg7Hkaxyxf177dpVPtMW2E6n3ijsPgke");

    String gateway = "tpubD9dXq2iRd5PppWdv38THfp9LLoBTU94RdpZohhsN4XohgVXykzhzvZWsbJa8k6yq21tnaB6w4wCMJPZ9wF8ed677CRTw8cvBw15XTLB3KHg";

    List<List<Signature>> signatures = ImmutableList.of(sig1.getSignTx().getSignaturesList(), sig2.getSignTx().getSignaturesList());

    ColdWallet coldWallet = new ColdWallet(TestNet3Params.get(), walletId, addresses, gateway);
    String transaction =
        toHexString(coldWallet.createTransaction(signTxRequest.getSignTxOrThrow().getInputsList(),
            signTxRequest.getSignTxOrThrow().getOutputsList(), signatures));
    assertThat(transaction).isEqualTo(
        "01000000000104109615ddfb4b767f6c9d9c33aff8166bceb065ccfdb43fa1e1776d852a84e57d0000000023220020a1a6cbed6d48dec7b235a03abdb2b033d94a4e9b6f1be8d7ac3cd67f4916e0dbfeffffff31ea724316a95ccf7757700c5fb973c5abc90aebc12ee8986cd9d1c9913fffaf01000000232200201302962e6038910d8378af75da33aa0dfc79e7f39ed0535c36f99f37cc7e73b0feffffffbcd1030304d9d77103c0e1b48a7e6dd6fbff27307681632e0490ea249b81b2d80000000023220020af9952c5364afd0bc3f652b7475cf19955dffccf38b32b5956ff27b71b84892afeffffffbcd1030304d9d77103c0e1b48a7e6dd6fbff27307681632e0490ea249b81b2d801000000232200200802857270d28e8396cf4cf28e2048b562e281b272bf90c898dc2e5a8d25d3effeffffff02eadc6a06000000001976a914f209bea76345aff81f46e98dcfab7c82ef10bf0b88acd4b9d50c000000001976a91424ae16e1b25950e00309371c161fc12920bfc92488ac040047304402205b3afa08e4bc5184d4fbc8342b69590ad2022dc146eaee1bd8f6dbecd05bec110220149538002118f0b8e363f9f891e7f34a2e6b35825f686f0fd04f86f97b918cca0147304402203115973546bf45e4ec2b9c9a32b78f7841d98db35f1627265877c26a9e39757d02203e73c67e127c92e25f9a88af40d2232ba6eb7b38b3cf01de176a9f3a667b1b8c018b5221020e85c11cd89f76a154b3bcecb72a59061ddc42beadca3ce052f18f4a9eec29372102546e5e2b33127c7d4233fd1007babb9968b83a946637307397dd11cd57a180c82102774ac67e1d9f73e7014fa18b54684acf27315756f23f68d450d0566fc948b27e210386dcfc6e1cf571c8ed6a840cb129763b6db6aec3fbdb69623aa559ff6195ed8954ae04004730440220239c90f56fa67893ab94de8adf6e4d8a437696344ea0cf5b80b750bf7392ecb6022016e1bec57d54e1074bc554e5c4e5d8a15065ebbac82eace913ae9dddc045523a0147304402205419bf94b68fe70fd0bba18b8a1603932cd35dab63afc3250d40be79dd3306b7022005927143f504c598628faf299f30b5db71893c4cbe96ae670df57f1ee09a7bf6018b52210297171fe6aeb0d350e342536e1b087265f3f9bf04542832ecff09851d1e41ff172103652b3cdde4f21909a28d48dc352f7d5e611123d6ed4a76db74b88a517bdd9d432103ced7d6985d5ae5eebf1289f761cd020477b746c2b2ccc8d55dbac1ec53e200d52103dd1b3b6e5c368119c54025fd3b7715cc66862eb5b75573de058ca8401d51f63554ae0400473044022006fd36d05b7c0d2c33c43f2fb39b634cbae029daf19ccb83bdd712dfe5e53dcd02200793acd4bb3833a35dee4c9596e4523835c03037966b96bb7086772bca1294080147304402200bcc808aec82eec1c6af29b56bd98e4f59a4aa879d4689bf0abcef2146d2ca0302202d1378e83cd31e43e766f55447d42a27d2039ff0b5b860c2e84363188963e5a6018b52210253e108c2fdb6eb033183093f97d25920285b0cb79d97cb3eece566c234c6b4b72102c6b695e912f54a6fd5171b11b60b8a0d579b61e04537c1ad3bc7abb51dc2de5d2103a8233d9fc5bf8b67d8c912f9e040a8233876deef8b3d91a9049c392728b6bafb2103d7a4f8da1077ce8d1f4443625c3734a92f6f0b5d8d0f1a094e3c6e91feb0b3c054ae04004830450221008e0da981e571dd773cd5c8f3ffbdc6caf3753fd1130cd03beaad2fc27371340f02206df1956ca2a29f583bd887d30bc25f3e8a6af2eff9ec696f6616d11c6492c1ca01483045022100e584f1906fabae1e3274dd0bf626d80bcad7b948394bd207226f07d3a78fdd2a022021b1a77276af01dbd5f421c7b080392d9a6656361262342638c7c9b4fdae66ee018b522102b1942adb06295930976c080c4b3f51861ff882daea7e4cfcc7003ece166209952102c82ce4241450537b2d1cfeb89e10684d38516593ca7fb7a9cf33704a51b5f5642102d5a7a2947e9ab732d37530bbf66f063e9f8f7c1ad23200845c25b1ed395120c3210361a8486ffe1eae5020b53beb7ef62b71b60386f99b14c98b30dfbade4ee2dc6d54ae00000000");

    // You can see this transaction here:
    // https://live.blockcypher.com/btc-testnet/tx/7153adb85690cf359d028e8e7fb6c7a9aee013105a72388509ff608f1593f9cf/
  }
}
