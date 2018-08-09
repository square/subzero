#pragma once

#include <stdio.h>

// Part 2/2 of magic to drop the full path from __FILE__
#define __FILENAME__ (__FILE__ + SOURCE_PATH_SIZE)

#ifdef BTC_TESTNET
  // Print DEBUG to stdout in green
  #define DEBUG(...)                                                           \
    do {                                                                       \
      printf("\033[0;32m");                                                    \
      printf("[DEBUG] %s:%d ", __FILENAME__, __LINE__);                        \
      printf(__VA_ARGS__);                                                     \
      printf("\033[0m\n");                                                     \
    } while (0)

  #define DEBUG_(...)                                                          \
    do {                                                                       \
      printf("\033[0;32m");                                                    \
      printf(__VA_ARGS__);                                                     \
      printf("\033[0m");                                                       \
    } while (0)

#else
  #define DEBUG(...) do {snprintf(NULL, 0, __VA_ARGS__);} while(0)
  #define DEBUG_(...) do {snprintf(NULL, 0, __VA_ARGS__);} while(0)
#endif

// Print INFO to stdout in red
#define INFO(...)                                                              \
  do {                                                                         \
    printf("\033[0;33m");                                                      \
    printf("[INFO] %s:%d ", __FILENAME__, __LINE__);                           \
    printf(__VA_ARGS__);                                                       \
    printf("\033[0m\n");                                                       \
  } while (0)

// Print ERROR to stdout in red
#define ERROR(...)                                                             \
  do {                                                                         \
    printf("\033[0;31m");                                                      \
    printf("[ERROR] %s:%d ", __FILENAME__, __LINE__);                          \
    printf(__VA_ARGS__);                                                       \
    printf("\033[0m\n");                                                       \
  } while (0)
