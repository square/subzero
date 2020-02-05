package com.squareup.subzero.shared;

public class Constants {
  public static final int MAGIC = 0x20de; // must be kept in sync with core/include/config.h
  public static final int VERSION = 207; // must be kept in sync with core/include/config.h
  public static final int TEMP_DEFAULT_WALLET_ID = 9999;

  public static final int MULTISIG_PARTICIPANTS = 4;
  public static final int MULTISIG_THRESHOLD = 2;

  // These constants need to be kept in sync with the following two files. The Java constants can be
  // more restrictive than the C ones.
  // https://github.com/square/subzero/blob/master/core/proto/squareup/subzero/common.options
  // https://github.com/square/subzero/blob/master/core/proto/squareup/subzero/internal.options
  public static final int ENCRYPTED_MASTER_SEED_MAX_SIZE = 92;
  public static final int MASTER_SEED_ENCRYPTION_KEY_TICKET_MAX_SIZE = 256;
  public static final int PUB_KEY_ENCRYPTION_KEY_TICKET_MAX_SIZE = 256;
  public static final int ENCRYPTED_PUB_KEYS_MAX_COUNT = 4;
  public static final int ENCRYPTED_PUB_KEY_MAX_SIZE = 156;
  public static final int INPUTS_COUNT_MAX = 128;
  public static final int OUTPUTS_COUNT_MAX = 128;
  public static final int TXINPUT_PREV_HASH_MAX_SIZE = 32;
  public static final int RANDOM_BYTES_MAX_SIZE = 64;

  /**
   * INPUTS_COUNT_AT_LEAST is the number of inputs that can be guaranteed to be supported
   * This differs from the COUNT_MAX values which are the maximum supported values. They differ
   * because you could have more inputs if you have less outputs, for example, or smaller varint
   * values (like account, index and amount).
   */
  public static final int INPUTS_COUNT_AT_LEAST = 27;
  /**
   * OUTPUTS_COUNT_AT_LEAST is the number of inputs that can be guaranteed to be supported
   * See INPUTS_COUNT_AT_LEAST for details.[
   */
  public static final int OUTPUTS_COUNT_AT_LEAST = 9;

  /**
   * MAX_QR_PROTO_BYTES is the maximum size a protobuf can be for encoding into a QR code sent to
   * or from Subzero.
   * 2,953 (40, L, Binary) * 3 / 4 (Base64 is 3 bytes in 4 chars)
   * http://www.qrcode.com/en/about/version.html contains the sizes for each type of QR code.
   */
  public static final int MAX_QR_PROTO_BYTES = 2214;
}
