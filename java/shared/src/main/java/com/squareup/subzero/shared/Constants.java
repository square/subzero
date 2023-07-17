package com.squareup.subzero.shared;

/**
 * This class holds a bunch of constants, many of which must be kept in sync with subzero core.
 */
public class Constants {
  /** Everything in this class is static, so don't allow construction. */
  private Constants() {}

  /** Magic number, must be kept in sync with core/include/config.h. */
  public static final int MAGIC = 0x20de;

  /** Current version, must be kept in sync with core/include/config.h. */
  public static final int VERSION = 210;

  /** Probably used for testing, or may be unused. */
  public static final int TEMP_DEFAULT_WALLET_ID = 9999;

  /** Total number of participants in multisig transactions. */
  public static final int MULTISIG_PARTICIPANTS = 4;

  /** Number of required signatures from participants in a multisig transaction. */
  public static final int MULTISIG_THRESHOLD = 2;

  // These constants need to be kept in sync with the following two files. The Java constants can be
  // more restrictive than the C ones.
  // https://github.com/square/subzero/blob/master/core/proto/squareup/subzero/common.options
  // https://github.com/square/subzero/blob/master/core/proto/squareup/subzero/internal.options

  /** Max size of an encrypted master seed, in bytes. */
  public static final int ENCRYPTED_MASTER_SEED_MAX_SIZE = 92;

  /** Max size of a master seed encryption key ticket, in bytes. */
  public static final int MASTER_SEED_ENCRYPTION_KEY_TICKET_MAX_SIZE = 256;

  /** Max size of a pub key encryption key ticket, in bytes. */
  public static final int PUB_KEY_ENCRYPTION_KEY_TICKET_MAX_SIZE = 256;

  /** Max number of encrypted pub keys. */
  public static final int ENCRYPTED_PUB_KEYS_MAX_COUNT = 4;

  /** Max size of an encrypted pub key, in bytes. */
  public static final int ENCRYPTED_PUB_KEY_MAX_SIZE = 156;

  /** Max number of transaction inputs. */
  public static final int INPUTS_COUNT_MAX = 128;

  /** Max number of transaction outputs. */
  public static final int OUTPUTS_COUNT_MAX = 128;

  /** Max size of a TxInput prev_hash, in bytes. */
  public static final int TXINPUT_PREV_HASH_MAX_SIZE = 32;

  /** Max size of an InitializeWallet.random_bytes field, in bytes. */
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
   * See INPUTS_COUNT_AT_LEAST for details.
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
