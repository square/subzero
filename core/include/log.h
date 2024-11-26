#pragma once

#include <stdio.h>
#include <stdlib.h>

// Part 2/2 of magic to drop the full path from __FILE__
#define __FILENAME__ (&__FILE__[SOURCE_PATH_SIZE])

#ifdef FUZZING_BUILD_MODE_UNSAFE_FOR_PRODUCTION
  // When fuzzing, suppress all log output except FATAL()
  #define ENABLE_INFO_AND_ERROR_LOGGING 0
  #define ENABLE_DEBUG_LOGGING 0
#else
  // When not fuzzing, always output INFO and ERROR logs
  #define ENABLE_INFO_AND_ERROR_LOGGING 1

  // When not fuzzing, output DEBUG logs if built with BTC_TESTNET
  #ifdef BTC_TESTNET
    #define ENABLE_DEBUG_LOGGING 1
  #else // must be BTC_MAINNET
    #define ENABLE_DEBUG_LOGGING 0
  #endif
#endif

#if ENABLE_DEBUG_LOGGING == 1
  // Print DEBUG to stdout in cyan
  #define DEBUG(...)                                    \
    do {                                                \
      printf("\033[0;36m");                             \
      printf("[DEBUG] %s:%d ", __FILENAME__, __LINE__); \
      printf(__VA_ARGS__);                              \
      printf("\033[0m\n");                              \
    } while (0)

  #define DEBUG_(...)       \
    do {                    \
      printf("\033[0;36m"); \
      printf(__VA_ARGS__);  \
      printf("\033[0m");    \
    } while (0)
#else
  #define DEBUG(...)                  \
    do {                              \
      snprintf(NULL, 0, __VA_ARGS__); \
    } while (0)
  #define DEBUG_(...)                 \
    do {                              \
      snprintf(NULL, 0, __VA_ARGS__); \
    } while (0)
#endif

#if ENABLE_INFO_AND_ERROR_LOGGING == 1
  // Print INFO to stdout in green
  #define INFO(...)                                    \
    do {                                               \
      printf("\033[0;32m");                            \
      printf("[INFO] %s:%d ", __FILENAME__, __LINE__); \
      printf(__VA_ARGS__);                             \
      printf("\033[0m\n");                             \
    } while (0)

  // Print ERROR to stdout in red
  #define ERROR(...)                                    \
    do {                                                \
      printf("\033[0;31m");                             \
      printf("[ERROR] %s:%d ", __FILENAME__, __LINE__); \
      printf(__VA_ARGS__);                              \
      printf("\033[0m\n");                              \
    } while (0)
#else
  #define INFO(...)                   \
    do {                              \
      snprintf(NULL, 0, __VA_ARGS__); \
    } while (0)
  #define ERROR(...)                  \
    do {                              \
      snprintf(NULL, 0, __VA_ARGS__); \
    } while (0)
#endif

// FATAL(...) will print the fatal error and immediately crash via abort().
// It uses the same color as ERROR output.
#define FATAL(...)                                    \
  do {                                                \
    printf("\033[0;31m");                             \
    printf("[FATAL] %s:%d ", __FILENAME__, __LINE__); \
    printf(__VA_ARGS__);                              \
    printf("\033[0m\n");                              \
    fflush(stdout);                                   \
    abort();                                          \
  } while (0)

#undef ENABLE_DEBUG_LOGGING
#undef ENABLE_INFO_LOGGING
#undef ENABLE_ERROR_LOGGING
