#include "rpc.h"
#include "squareup/subzero/internal.pb.h"

Result pre_execute_command(const InternalCommandRequest* const in) {
  (void)in;
  return Result_SUCCESS;
}

void post_execute_command(void) {}
