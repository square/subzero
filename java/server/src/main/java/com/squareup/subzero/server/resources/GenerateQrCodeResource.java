package com.squareup.subzero.server.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.squareup.protos.subzero.service.Common;
import com.squareup.protos.subzero.service.Service;
import com.squareup.subzero.shared.Constants;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.spongycastle.util.encoders.Hex;

import static java.lang.String.format;

/**
 * Creates various QR codes. Also returns the base64-encoded data, which allows copy-pasting in
 * development.
 *
 * Uses the same underlying QR-code library as the GUI.
 */
@Path("/generate-qr-code")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class GenerateQrCodeResource {
  class QrCode {
    public String data;
    public int size;
    public boolean[] pixels;
  }



  @Path("/init-wallet-request")
  @GET
  public QrCode initRequest(@QueryParam("wallet") int wallet)
      throws WriterException {
    Service.CommandRequest.Builder builder = Service.CommandRequest.newBuilder();
    builder.setWalletId(wallet);
    builder.setInitWallet(Service.CommandRequest.InitWalletRequest.newBuilder());

    return generateQrCode(builder);
  }

  public static class SignTxRequest {
    public static class Input {
      @JsonProperty("prev_hash") public String prevHash;
      @JsonProperty("prev_index") public int prevIndex;
      @JsonProperty public long amount;
      @JsonProperty public boolean change;
      @JsonProperty public int index;
    }
    public static class Output {
      @JsonProperty public String destination;
      @JsonProperty public long amount;
      @JsonProperty public int index;
    }

    @JsonProperty public int wallet;
    @JsonProperty public List<Input> inputs;
    @JsonProperty public List<Output> outputs;
    @JsonProperty public int lockTime;
    @JsonProperty public String currency;
    @JsonProperty public double rate;
  }

  @Path("/finalize-wallet-request")
  @GET
  public QrCode finalizeRequest(@QueryParam("wallet") int wallet, @QueryParam("encPubKeys")
      List<String> encPubKeys)
      throws WriterException, InvalidProtocolBufferException {

    if (encPubKeys.size() != Constants.MULTISIG_PARTICIPANTS) {
      throw new IllegalArgumentException(format("Expecting %d encPubKeys, got %d",
          Constants.MULTISIG_PARTICIPANTS, encPubKeys.size()));
    }

    Service.CommandRequest.FinalizeWalletRequest.Builder finalizeWalletBuilder =
        Service.CommandRequest.FinalizeWalletRequest.newBuilder();

    for (String encPubKey : encPubKeys) {
      byte[] rawEncPubKey = BaseEncoding.base64().decode(encPubKey);
      Service.CommandResponse commandResponse = Service.CommandResponse.parseFrom(rawEncPubKey);
      finalizeWalletBuilder.addEncryptedPubKeys(commandResponse.getInitWallet().getEncryptedPubKey());
    }

    Service.CommandRequest.Builder builder = Service.CommandRequest.newBuilder();
    builder.setWalletId(wallet);
    builder.setFinalizeWallet(finalizeWalletBuilder);

    return generateQrCode(builder);
  }

  @Path("/sign-tx-request")
  @POST
  public QrCode signTxRequest(SignTxRequest request) throws WriterException {
    Service.CommandRequest.SignTxRequest.Builder signTxBuilder =
        Service.CommandRequest.SignTxRequest.newBuilder();
    for (SignTxRequest.Input input : request.inputs) {
      signTxBuilder.addInputs(Common.TxInput.newBuilder()
          .setPrevHash(ByteString.copyFrom(Hex.decode(input.prevHash)))
          .setPrevIndex(input.prevIndex)
          .setAmount(input.amount)
          .setPath(Common.Path.newBuilder().setIsChange(input.change).setIndex(input.index)));
    }
    for (SignTxRequest.Output output : request.outputs) {
      Common.Destination destination;
      switch (output.destination) {
        case "Gateway":
          destination = Common.Destination.GATEWAY;
          break;
        case "Change":
          destination = Common.Destination.CHANGE;
          break;
        default:
          throw new IllegalArgumentException(format("expecting Gateway or Change, got %s", output.destination));
      }
      signTxBuilder.addOutputs(Common.TxOutput.newBuilder()
          .setDestination(destination)
          .setAmount(output.amount)
          .setPath(Common.Path.newBuilder()
              .setIsChange(destination == Common.Destination.CHANGE)
              .setIndex(output.index)));
    }
    signTxBuilder.setLockTime(request.lockTime);
    signTxBuilder.setLocalCurrency(request.currency);
    signTxBuilder.setLocalRate(request.rate);

    Service.CommandRequest.Builder builder = Service.CommandRequest.newBuilder();
    builder.setWalletId(request.wallet);
    builder.setSignTx(signTxBuilder);

    return generateQrCode(builder);
  }

  protected QrCode generateQrCode(Service.CommandRequest.Builder builder) throws WriterException {
    QrCode r = new QrCode();
    r.data = BaseEncoding.base64().encode(builder.build().toByteArray());

    QRCodeWriter qrCodeWriter = new QRCodeWriter();
    BitMatrix matrix = qrCodeWriter.encode(r.data, BarcodeFormat.QR_CODE, 1, 1);
    if (matrix.getWidth() != matrix.getHeight()) {
      throw new IllegalStateException("width != height");
    }
    r.size = matrix.getWidth();
    r.pixels = new boolean[r.size * r.size];
    int i = 0;
    for (int y=0; y<r.size; y++) {
      for (int x = 0; x < r.size; x++) {
        r.pixels[i++] = matrix.get(x, y);
      }
    }
    return r;
  }
}
