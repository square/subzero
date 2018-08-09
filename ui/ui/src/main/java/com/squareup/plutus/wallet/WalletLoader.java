package com.squareup.plutus.wallet;

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.JsonFormat;
import com.squareup.protos.plutus.wallet.WalletProto.Wallet;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
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
  private final static String HD_PATH = "/data/app/plutus/wallets/";
  protected final static String MARKER_FILE = ".plutus_702e63a9";

  private Path directory;

  public WalletLoader() {
    directory =  FileSystems.getDefault().getPath(HD_PATH);
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
    JsonFormat.print(wallet, osw);
    osw.flush();
    fos.getFD().sync();

    // TODO: save to USB backup drive
  }

  public Wallet load(int walletId) throws IOException {
    Path path = getWalletPath(walletId);
    System.out.println(format("Loading wallet file from: %s", path));
    FileReader fr = new FileReader(path.toFile());
    Wallet.Builder wallet = Wallet.newBuilder();
    JsonFormat.merge(fr, wallet);

    // TODO: copy and load from USB backup drive if the file is not found.

    return wallet.build();
  }

  /**
   * Path to folder which contains the wallet files.
   */
  private Path getWalletPath(int walletId) {
    // Check that the marker file exists.
    Path marker = directory.resolve(MARKER_FILE);
    if (!marker.toFile().exists()) {
      throw new IllegalStateException(format("marker file not found: %s", marker));
    }

    Path wallet = directory.resolve(format("plutus-%d.wallet", walletId));
    return wallet;
  }
}
