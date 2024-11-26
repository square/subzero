#include "pb_decode.h"
#include "pb_encode.h"
#include "rpc.h"

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

extern int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size);

static uint8_t response_buf[4096] = { 0 };

int LLVMFuzzerTestOneInput(const uint8_t* data, size_t size) {
  pb_istream_t istream = pb_istream_from_buffer(data, size);
  pb_ostream_t ostream = pb_ostream_from_buffer(response_buf, sizeof(response_buf));

  handle_incoming_message(&istream, &ostream);

  return 0; // Values other than 0 and -1 are reserved for future use.
}
