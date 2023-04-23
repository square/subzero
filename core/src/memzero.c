#include "memzero.h"

#ifdef CONFIG_HAVE_MEMSET_S
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "log.h"

void memzero(void* s, size_t n) {
  int res = memset_s(s, n, 0, n);
  if (res != 0) {
    ERROR("memset_s() failed with error code %d: %s", (int) res, strerror(res));
    abort(); // unsafe to continue, terminate the program
  }
}

#else

#include <stdint.h>

/**
 * This function MUST NEVER be included in a file with any other functions.
 * Security relies on the fact that the linker cannot optimize function calls
 * between separate files and so cannot strip this functionality from the
 * execution flow.
 *
 * Note: Trezor's crypto calls this function, so we can't change the name.
 */
void memzero(void *s, size_t n) {
  volatile uint8_t *ptr;

  ptr = (uint8_t *)s;

  for (size_t i=0; i<n; i++) {
    *ptr = 0;
    ptr++;
  }
}

#endif
