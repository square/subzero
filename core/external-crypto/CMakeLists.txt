cmake_minimum_required(VERSION 2.8)

set(SOURCES
    modes/gcm.c
    modes/gf128mul.c
    aes/aescrypt.c
    aes/aeskey.c
    aes/aes_modes.c
    aes/aestab.c
)

add_library(GladmanGCM STATIC ${SOURCES})

target_include_directories(GladmanGCM PUBLIC aes)
target_include_directories(GladmanGCM PUBLIC modes)

target_compile_options(GladmanGCM PRIVATE "-std=c99")
add_definitions(-Wno-cast-qual -Wno-incompatible-pointer-types -Wno-missing-prototypes)
