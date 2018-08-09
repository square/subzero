#include <stdint.h>

#include "memzero.h"

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
