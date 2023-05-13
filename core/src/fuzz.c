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

/* This is useful for debugging the fuzz driver. from nanopb docs. */
static bool fwrite_callback(pb_ostream_t *stream, const uint8_t *buf, size_t count) {
   FILE *file = (FILE*) stream->state;
   return fwrite(buf, 1, count, file) == count;
}

/**
 * Entry point for AFL fuzzing
 * It takes a single argument: A file to read with an InternalCommandRequest
 * protobuf.  It does not write the output anywhere.
 */
int main(int argc, char **argv) {
  if (argc != 2) {
    printf("usage: fuzz <input>\n");
    return -1;
  }

  int fd = open(argv[1], O_RDONLY);
  if (fd < 0) {
    printf("open %s failed: %s\n", argv[1], strerror(errno));
    return -2;
  }

  struct stat s;
  if (fstat(fd, &s) < 0) {
    printf("stat %s failed: %s\n", argv[1], strerror(errno));
    return -3;
  }

  void* buf = malloc(s.st_size);
  if(buf == NULL) {
    printf("malloc %lld bytes failed\n", s.st_size);
    return -4;
  }

  ssize_t count = read(fd, buf, s.st_size);
  if(count != s.st_size) {
    printf("Failed to read full amount: %zd is not expected %lld\n", count, s.st_size);
    return -5;
  }

  pb_istream_t istream = pb_istream_from_buffer(buf, s.st_size);

  /* Callback may be null to drop the output */
  //pb_ostream_t ostream = {0, 0, SIZE_MAX, 0, 0};

  pb_ostream_t ostream = {&fwrite_callback, stderr, SIZE_MAX, 0, 0};

  handle_incoming_message(&istream, &ostream);

  printf("output size %zu\n", ostream.bytes_written);

  return 0;
}
