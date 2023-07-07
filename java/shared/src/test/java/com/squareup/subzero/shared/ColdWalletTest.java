package com.squareup.subzero.shared;

import com.google.common.collect.ImmutableList;
import com.google.protobuf.ByteString;
import com.squareup.subzero.proto.service.Common.Destination;
import com.squareup.subzero.proto.service.Common.Path;
import com.squareup.subzero.proto.service.Common.Signature;
import com.squareup.subzero.proto.service.Common.TxInput;
import com.squareup.subzero.proto.service.Common.TxOutput;
import com.squareup.subzero.proto.service.Service.CommandRequest;
import com.squareup.subzero.proto.service.Service.CommandResponse;
import java.util.ArrayList;
import java.util.List;
import org.bitcoinj.params.TestNet3Params;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static java.lang.String.format;

public class ColdWalletTest {
  private ColdWallet coldWallet;


  @Before public void setup() {
    List<String> pubkeys = ImmutableList.of(
        "tpubD9jBarsLCKot45kvTTu7yWxmNnkBPbpn2CgS1F3yuxZGgohTkamYwJJKenZHrsYwPRJY66dk3ZUt3aZwZqFf7QGsgUUUcNSvvb9NXHFt5Vb",
        "tpubD8u5eHk8B6q63G4ekfBB2eVCeBYTrW2uCvi7Z3BF6bY2z33jW14Uzna8f5cFQWS3HFwdAzUWxqESq7j5x2CUQ7uBPgpXnpf8X9FPnh63XYd",
        "tpubD9dkteiWZqa3jL17meqVvy1RSUQsmJvU3hBfAosMzrSa69DacRku8yHy3E2fma1Q4Den25ukcsBL3bYTFyKjKbF8CEWHu86Xg9YXiY6CkeC",
        "tpubD8GrNWdYHDjdJryH5tng8LUzHSrEo4gToaguKnzthEqTxyfF13jTsp4sMtmso4n1VC58R5Wvt4Ua4npZTecR1xaGGYJgLLQj5sQGdD2xh2N");

    String gateway = "tpubD8GrNWdYHDjdJryH5tng8LUzHSrEo4gToaguKnzthEqTxyfF13jTsp4sMtmso4n1VC58R5Wvt4Ua4npZTecR1xaGGYJgLLQj5sQGdD2xh2N";
    coldWallet = new ColdWallet(TestNet3Params.get(), 0x7FFFFFFF, pubkeys, gateway);
  }

  /**
   * MaxSizeTest verifies we can create a transaction with the documented number of inputs and
   * outputs, with the generated proto under the QR code size limit.
   */
  @Test public void MaxSizeTest() {
    Path.Builder path = Path.newBuilder()
        .setIsChange(false)
        .setIndex(0x7FFFFFFF);

    // This input is as big as possible.
    TxInput input = TxInput.newBuilder()
        .setPrevHash(ByteString.copyFrom(Hex.decode("d37365c6a3c089576b1ea1cb8bee2baa005afc96d24080515635f852c680d127")))
        .setPrevIndex(0x7FFFFFFF)
        .setAmount(0x7FFFFFFF7FFFFFFFL)
        .setPath(path)
        .build();

    TxOutput output = TxOutput.newBuilder()
        .setAmount(0x7FFFFFFF7FFFFFFFL)
        .setDestination(Destination.GATEWAY)
        .setPath(path)
        .build();

    List<TxInput> inputs = new ArrayList<>();
    for(int i = 0; i < Constants.INPUTS_COUNT_AT_LEAST; i++) {
      inputs.add(input);
    }

    List<TxOutput> outputs = new ArrayList<>();
    for(int i = 0; i < Constants.OUTPUTS_COUNT_AT_LEAST; i++) {
      outputs.add(output);
    }

    String token = "TokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenToken";
    CommandRequest request = coldWallet.startTransaction(inputs, outputs, token, null);
    assertTrue(request.getSerializedSize() < Constants.MAX_QR_PROTO_BYTES);

    // The response consists of signatures.  We know bitcoin signatures are, in worst-case, 73 bytes
    // So we verify a CommandResponse.SignTxResponse with 73 byte signatures fits.
    List<Signature> signatures = new ArrayList<>();

    for(int i = 0; i < Constants.INPUTS_COUNT_AT_LEAST; i++) {
      signatures.add(Signature.newBuilder()
          .setDer(ByteString.copyFrom(Hex.decode(
                  "3045022100c822d9b754a87ef25a0c9a9ad44eba41bd00822ddab60f759e77e99e80427feb02207edc74ca82fafc6d7b42ef4815d4142ff49fca0bac533449f15543c19264c13901FF"
           ))).build());
    }

    CommandResponse response = CommandResponse.newBuilder()
        .setToken(token)
        .setSignTx(CommandResponse.SignTxResponse.newBuilder()
            .addAllSignatures(signatures))
        .build();

    System.out.println(format("the serialized size is %d (of %d)", response.getSerializedSize(), Constants.MAX_QR_PROTO_BYTES));
    assertTrue(response.getSerializedSize() < Constants.MAX_QR_PROTO_BYTES);
  }

  @Test public void testExchangeRate() {
    Path.Builder path = Path.newBuilder()
        .setIsChange(false)
        .setIndex(0x7FFFFFFF);

    // This input is as big as possible.
    TxInput input = TxInput.newBuilder()
        .setPrevHash(ByteString.copyFrom(Hex.decode("d37365c6a3c089576b1ea1cb8bee2baa005afc96d24080515635f852c680d127")))
        .setPrevIndex(0x7FFFFFFF)
        .setAmount(0x7FFFFFFF7FFFFFFFL)
        .setPath(path)
        .build();

    TxOutput output = TxOutput.newBuilder()
        .setAmount(0x7FFFFFFF7FFFFFFFL)
        .setDestination(Destination.GATEWAY)
        .setPath(path)
        .build();

    List<TxInput> inputs = new ArrayList<>();
    for(int i = 0; i < Constants.INPUTS_COUNT_AT_LEAST; i++) {
      inputs.add(input);
    }

    List<TxOutput> outputs = new ArrayList<>();
    for(int i = 0; i < Constants.OUTPUTS_COUNT_AT_LEAST; i++) {
      outputs.add(output);
    }

    String token = "TokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenTokenToken";
    double localRate = 0.00123;
    CommandRequest request = coldWallet.startTransaction(inputs, outputs, token, localRate);
    assertEquals(localRate, request.getSignTx().getLocalRate(), 0.0);
  }
}
