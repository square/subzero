set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} ${CMAKE_CURRENT_SOURCE_DIR}/../nanopb/extra/)
find_package(Nanopb REQUIRED)
include_directories(${NANOPB_INCLUDE_DIRS})

set(protos
    "${CMAKE_CURRENT_LIST_DIR}/squareup/subzero/internal.proto"
    "${CMAKE_CURRENT_LIST_DIR}/squareup/subzero/common.proto"
    "${CMAKE_CURRENT_LIST_DIR}/squareup/subzero/service.proto"
)

# The next 2 lines are needed to build with nanopb 0.4.5. Can be removed if we update to 0.4.7.
set(NANOPB_GENERATE_CPP_APPEND_PATH FALSE)
set(NANOPB_OPTIONS "-I${CMAKE_CURRENT_LIST_DIR}/")
nanopb_generate_cpp(PROTO_SRCS PROTO_HDRS ${protos} RELPATH ${CMAKE_CURRENT_SOURCE_DIR})
include_directories(${CMAKE_CURRENT_BINARY_DIR})
set_source_files_properties(${PROTO_SRCS} ${PROTO_HDRS}
    PROPERTIES GENERATED TRUE)

add_library(SubzeroProtos STATIC ${PROTO_SRCS} ${PROTO_HDRS})
