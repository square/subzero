#include "checks.h"
#include "log.h"
#include "sign.h"
#include "squareup/subzero/common.pb.h"
#include "squareup/subzero/internal.pb.h"

#include <stdbool.h>

int verify_validate_fees(void) {
  int r = 0;

  // test case where the fee is within both limits (under 1BTC, under 10% of
  // total)
  InternalCommandRequest_SignTxRequest tx1 = InternalCommandRequest_SignTxRequest_init_default;
  tx1.inputs_count = 1;
  tx1.inputs[0].has_amount = true;
  tx1.inputs[0].amount = 100000;
  tx1.outputs_count = 1;
  tx1.outputs[0].has_amount = true;
  tx1.outputs[0].amount = 95000;
  tx1.outputs[0].destination = Destination_GATEWAY;
  bool t = validate_fees(&tx1);
  if (!t) {
    r = -1;
    ERROR("verify_validate_fees: tx1 failed fee validation, but fee is valid.");
  }

  // test case where fee is under 1BTC but over 10%
  InternalCommandRequest_SignTxRequest tx2 = InternalCommandRequest_SignTxRequest_init_default;
  tx2.inputs_count = 1;
  tx2.inputs[0].has_amount = true;
  tx2.inputs[0].amount = 1000000;
  tx2.outputs_count = 1;
  tx2.outputs[0].has_amount = true;
  tx2.outputs[0].amount = 500000;
  tx2.outputs[0].destination = Destination_GATEWAY;
  t = validate_fees(&tx2);
  if (!t) {
    r = -1;
    ERROR("verify_validate_fees: tx2 failed fee validation, but fee is valid.");
  }

  // test case where fee is over 1BTC but under 10%
  InternalCommandRequest_SignTxRequest tx3 = InternalCommandRequest_SignTxRequest_init_default;
  tx3.inputs_count = 1;
  tx3.inputs[0].has_amount = true;
  tx3.inputs[0].amount = 2000000000;
  tx3.outputs_count = 1;
  tx3.outputs[0].has_amount = true;
  tx3.outputs[0].amount = 1890000000;
  tx3.outputs[0].destination = Destination_GATEWAY;
  t = validate_fees(&tx3);
  if (!t) {
    r = -1;
    ERROR("verify_validate_fees: tx3 failed fee validation, but fee is valid.");
  }

  // test case where fee is over 1BTC and over 10%
  InternalCommandRequest_SignTxRequest tx4 = InternalCommandRequest_SignTxRequest_init_default;
  tx4.inputs_count = 1;
  tx4.inputs[0].has_amount = true;
  tx4.inputs[0].amount = 10000000000L;
  tx4.outputs_count = 1;
  tx4.outputs[0].has_amount = true;
  tx4.outputs[0].amount = 8000000000L;
  tx4.outputs[0].destination = Destination_GATEWAY;
  t = validate_fees(&tx4);
  if (t) {
    r = -1;
    ERROR(
        "verify_validate_fees: tx4 did not fail fee validation, but fee is "
        "over limits.");
  }

  // test case where fee is over 1BTC and over 10% if you don't count the amount
  // going to the change address in the total (which is how it should be
  // calculated) this case uses input=10,000,000,000,
  // output(gateway)=8,000,000,000 and output(change)=1,150,000,000. the fee is
  // 850,000,000, which is 8.5 BTC, and 10.63% this should fail validation. if
  // we miscalculated the fee % by including the amount going to the change
  // address as part of the total, it would be 9.29%, which would pass
  // validation
  InternalCommandRequest_SignTxRequest tx5 = InternalCommandRequest_SignTxRequest_init_default;
  tx5.inputs_count = 1;
  tx5.inputs[0].has_amount = true;
  tx5.inputs[0].amount = 10000000000L;
  tx5.outputs_count = 2;
  tx5.outputs[0].has_amount = true;
  tx5.outputs[0].amount = 8000000000L;
  tx5.outputs[0].destination = Destination_GATEWAY;
  tx5.outputs[1].has_amount = true;
  tx5.outputs[1].amount = 1150000000L;
  tx5.outputs[1].destination = Destination_CHANGE;
  t = validate_fees(&tx5);
  if (t) {
    r = -1;
    ERROR(
        "verify_validate_fees: tx5 did not fail fee validation, but fee is "
        "over limits.");
  }

  // test case with negative fee
  InternalCommandRequest_SignTxRequest tx6 = InternalCommandRequest_SignTxRequest_init_default;
  tx6.inputs_count = 1;
  tx6.inputs[0].has_amount = true;
  tx6.inputs[0].amount = 1000;
  tx6.outputs_count = 2;
  tx6.outputs[0].has_amount = true;
  tx6.outputs[0].amount = 900;
  tx6.outputs[0].destination = Destination_GATEWAY;
  tx6.outputs[1].has_amount = true;
  tx6.outputs[1].amount = 200;
  tx6.outputs[1].destination = Destination_CHANGE;
  ERROR("(next line is expected to show red text...)");
  t = validate_fees(&tx6);
  if (t) {
    r = -1;
    ERROR(
        "verify_validate_fees: tx6 did not fail fee validation, but fee is "
        "negative.");
  }

  return r;
}
