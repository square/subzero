cmake_minimum_required(VERSION 2.8)

set(NFAST_PATH /opt/nfast)
if (DEFINED ENV{NCIPHER_TOOLS_LOCATION})
  set(NFAST_PATH $ENV{NCIPHER_TOOLS_LOCATION})
endif ()

set(NFAST_DEV ${NFAST_PATH}/c/csd)

find_program(
    CODESAFE_GCC
    powerpc-codesafe-linux-gnu-gcc
    PATHS ${NFAST_PATH}/gcc/bin
)

find_program(
    CODESAFE_LD
    powerpc-codesafe-linux-gnu-ld
    PATHS ${NFAST_PATH}/gcc/bin
)

include_directories(${NFAST_PATH}/c/csd/include-see/module ${NFAST_PATH}/c/csd/include-see/module/glibsee ${NFAST_PATH}/c/csd/include-see/hilibs ${NFAST_PATH}/c/csd/include-see/cutils/)

set(LIBPATH_GENERAL "${NFAST_DEV}/lib-ppc-linux-gcc/")
set(CMAKE_C_COMPILER ${CODESAFE_GCC})
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mpowerpc -mcpu=e5500 -mno-toc -mbig-endian -mhard-float -mno-multiple -mno-string -meabi -mprototype -mstrict-align -memb -fno-builtin -DNF_CROSSCC=1 -DNFAST_CONF_H='\"nfast-conf-auto.h\"' -DNF_CROSSCC_PPC_GCC=1 -D_THREAD_SAFE -DBYTE_ORDER=4321")

set(CMAKE_C_LINK_EXECUTABLE
    "${CODESAFE_GCC} -Wl,-wrap=read -Wl,-wrap=write -Wl,-wrap=socket -Wl,-wrap=send  -Wl,-wrap=sendto -Wl,-wrap=recv -Wl,-wrap=recvfrom -Wl,-wrap=listen -Wl,-wrap=connect -Wl,-wrap=bind -Wl,-wrap=setsockopt -Wl,-wrap=select -Wl,-wrap=accept -Wl,-wrap=poll -Bstatic -o <TARGET> <OBJECTS> <LINK_LIBRARIES> ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/objects-Debug/hoststdioeinetsocks/vfsextras/hoststdioeinetsocks.c.o ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libfaksys.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libcutils.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libvfsextras.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libseewrpr.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libipccore.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libsolotrace.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libnfstub.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libfaksys.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libseewrpr.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/seelib.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libipccore.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libnflog.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libcutils.a ${NFAST_PATH}/c/csd/lib-glibc-ppc-gcc/libgsexamples.a -lpthread -lrt")

add_custom_command(OUTPUT subzero-unsigned.ar
  DEPENDS subzero
  COMMAND ${NFAST_PATH}/bin/tct2 --pack --infile=subzero --outfile subzero-unsigned.ar
)

add_custom_command(OUTPUT subzero.cpio
  COMMAND echo dummy > dummy
  COMMAND ${NFAST_PATH}/bin/cpioc subzero.cpio dummy
)

add_custom_target(run-unsigned
  DEPENDS subzero-unsigned.ar subzero.cpio
  COMMAND sudo ${NFAST_PATH}/bin/nopclearfail -c -a
  COMMAND sudo ${NFAST_PATH}/bin/see-stdioesock-serv --machine subzero-unsigned.ar --userdata-raw subzero.cpio
)

add_custom_command(OUTPUT subzero-signed.sar
  DEPENDS subzero
  COMMAND ${NFAST_PATH}/bin/tct2 --sign-and-pack --key=subzerocodesigner --is-machine --machine-type=PowerPCELF --infile=subzero --outfile subzero-signed.sar
)

add_custom_command(OUTPUT subzero-userdata-signed.sar
  DEPENDS subzero.cpio
  COMMAND ${NFAST_PATH}/bin/tct2 --sign-and-pack --key=subzerodatasigner --machine-key-ident=subzerocodesigner --infile=subzero.cpio --outfile subzero-userdata-signed.sar
)

add_custom_target(run-signed
  DEPENDS subzero-signed.sar subzero-userdata-signed.sar
  COMMAND sudo ${NFAST_PATH}/bin/nopclearfail -c -a
  COMMAND sudo ${NFAST_PATH}/bin/see-stdioesock-serv --machine subzero-signed.sar --userdata-sar subzero-userdata-signed.sar
)

add_custom_target(prod
  DEPENDS subzero-signed.sar subzero-userdata-signed.sar
)
