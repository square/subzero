set(CMAKE_MODULE_PATH ${CMAKE_CURRENT_SOURCE_DIR}/../nanopb/extra/)
find_package(Nanopb REQUIRED)
include_directories(${NANOPB_INCLUDE_DIRS})

file(GLOB protos "squareup/subzero/*.proto")

nanopb_generate_cpp(PROTO_SRCS PROTO_HDRS ${protos} RELPATH ${CMAKE_CURRENT_SOURCE_DIR})
include_directories(${CMAKE_CURRENT_BINARY_DIR})
#add_custom_target(generate_proto_sources DEPENDS ${PROTO_SRCS} ${PROTO_HDRS})
set_source_files_properties(${PROTO_SRCS} ${PROTO_HDRS}
    PROPERTIES GENERATED TRUE)

add_library(SubzeroProtos STATIC ${PROTO_SRCS} ${PROTO_HDRS})
