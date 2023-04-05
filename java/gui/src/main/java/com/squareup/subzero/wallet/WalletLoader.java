package com.squareup.subzero.wallet;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import com.squareup.subzero.proto.wallet.WalletProto.Wallet;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.format;

/**
 * Loads/saves wallet files.
 *
 * In order to make sure that we are loading/saving files to the right location, we use a marker
 * file.
 */
public class WalletLoader {
  protected final static String MARKER_FILE = ".subzero_702e63a9";

  private Path directory;

  /**
   * Creates a new WalletLoader and sets the wallet file location to the path provided.
   * Will throw runtime exceptions if the path provided is invalid.
   */
  public WalletLoader(String walletFilePath) {
    directory =  FileSystems.getDefault().getPath(walletFilePath);
  }

  @VisibleForTesting
  protected WalletLoader(Path directory) {
    this.directory = directory;
  }

  /**
   * Throws IllegalStateException if a given wallet file exists.
   */
  public void ensureDoesNotExist(int walletId) {
    Path path = getWalletPath(walletId);
    if (path.toFile().exists()) {
      throw new IllegalStateException(format("%s already exists.", path));
    }

    // TODO: check whether file exists on USB backup drive
  }

  /**
   * Saves wallet and calls fsync to ensure the OS flushes its disk cache.
   */
  public void save(int walletId, Wallet wallet) throws IOException {
    Path path = getWalletPath(walletId);
    System.out.println(format("Saving wallet file to: %s", path));
    FileOutputStream fos = new FileOutputStream(path.toFile());
    OutputStreamWriter osw = new OutputStreamWriter(fos, UTF_8);

    // We save the wallet in JSON format, because it's simpler if we need to fix anything manually.
    JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace().appendTo(wallet, osw);
    osw.flush();
    fos.getFD().sync();

    // TODO: save to USB backup drive
  }
  /**
   * Utility method for generate wallet test.
   * Note: Only for testing.
   * @param walletId Integer representation of wallet number.
   * @param wallet data structure to be written.
   * @param hsmNumber hsmNumber representing which HSM out of the 4 generated this wallet.
   * @param prefix Directory name to save the wallet file in.
   * @throws IOException
   */
  public void saveNumbered(int walletId, Wallet wallet, int hsmNumber, String prefix) throws IOException{
    Path path = directory.resolve(format("%s/subzero-%d.wallet-%d", prefix, walletId, hsmNumber));
    System.out.println(format("Saving wallet file to: %s", path));
    Files.createDirectories(directory.resolve(prefix));
    FileOutputStream fos = new FileOutputStream(path.toFile());
    OutputStreamWriter osw = new OutputStreamWriter(fos, UTF_8);

    // We save the wallet in JSON format, because it's simpler if we need to fix anything manually.
    JsonFormat.printer().preservingProtoFieldNames().omittingInsignificantWhitespace().appendTo(wallet, osw);
    osw.flush();
    fos.getFD().sync();
  }

  /**
   * Utility method to load a specific wallet with for a specific hsm number.
   * Note: Only for testing.
   * @param walletId
   * @param hsmNumber
   * @param prefix Directory name to load the wallet file from.
   * @throws IOException
   */
  public Wallet loadNumbered(int walletId, int hsmNumber, String prefix)  throws IOException{
    Path path = directory.resolve(format("%s/subzero-%d.wallet-%d", prefix, walletId, hsmNumber));
    System.out.println(format("Loading wallet file from: %s", path));
    FileReader fr = new FileReader(path.toFile());
    Wallet.Builder wallet = Wallet.newBuilder();
    JsonFormat.parser().merge(fr, wallet);
    return wallet.build();
  }

  public Wallet load(int walletId) throws IOException {
    Path path = getWalletPath(walletId);
    System.out.println(format("Loading wallet file from: %s", path));
    FileReader fr = new FileReader(path.toFile());
    Wallet.Builder wallet = Wallet.newBuilder();
    JsonFormat.parser().merge(fr, wallet);

    // TODO: copy and load from USB backup drive if the file is not found.

    return wallet.build();
  }

  /**
   * Load hardcoded wallet for regression transaction signing testing
   * @param isNcipher
   * @return
   * @throws InvalidProtocolBufferException
   */
  public Wallet loadTestWallet(Boolean isNcipher) throws InvalidProtocolBufferException {
    String s = new String(isNcipher ? TestWallets.ncipherTestWallet: TestWallets.devTestWallet);
    Wallet.Builder wallet = Wallet.newBuilder();
    JsonFormat.parser().merge(s, wallet);

    return wallet.build();
  }
  /**
   * Path to folder which contains the wallet files.
   */
  public Path getWalletPath(int walletId) {
    // Check that the marker file exists.
    Path marker = directory.resolve(MARKER_FILE);
    if (!marker.toFile().exists()) {
      throw new IllegalStateException(format("marker file not found: %s", marker));
    }

    Path wallet = directory.resolve(format("subzero-%d.wallet", walletId));
    return wallet;
  }
}
