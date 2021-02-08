#!/usr/bin/env python3

"""An expect script compiler for subzero regression test.

This is a utility that creates an expect script that in turn generates
random test cases for subzero tranaction signing regression testing.
You may think of this script as a compiler that generates code that powers
a robot to interact with the subzero GUI to capture request/response
messages during transaction signing.
"""

import random
import requests
import json
from string import Template

# Due to the QR code capacity limit (2953 bytes), we can only guarantee to fit
# 17 signatures in a single QR code, and thus have the transaction input count
# capped at 17
MAX_INPUT_COUNT = 17
MAX_PREV_INDEX = 6
MAX_INPUT_INDEX = 10
# 1 million BTC, 10 BTC, 0.000001 BTC, respectively
MAX_INPUT_AMOUNT_SATOSHI_LARGE = 10**14
MAX_INPUT_AMOUNT_SATOSHI_MEDIUM = 10**9
MAX_INPUT_AMOUNT_SATOSHI_SMALL = 100
MAX_INPUT_AMOUNT_SATOSHI = [MAX_INPUT_AMOUNT_SATOSHI_LARGE,
                            MAX_INPUT_AMOUNT_SATOSHI_MEDIUM,
                            MAX_INPUT_AMOUNT_SATOSHI_SMALL]
MAX_OUTPUT_INDEX = 10
MAX_OUTPUT_GATEWAY_NUM = 5
WALLET_ID = 1492

# Generate valid tranactions
def generate_json_requests():
  """Generate valid, json formatted transactions.

  Return a list of dict objects representing the json contents
  """
  random.seed(1492)
  request_json_list = []

  for i in range(1, MAX_INPUT_COUNT+1):
    for j in range(1, MAX_OUTPUT_GATEWAY_NUM+1):
      for k in range(len(MAX_INPUT_AMOUNT_SATOSHI)):
        # Generate inputs
        prev_hash = ["%064x" % random.randrange(256**32) for ii in range(i)]
        prev_index = [str(random.randrange(MAX_PREV_INDEX)) for ii in range(i)]
        input_amounts = random.sample(range(MAX_INPUT_AMOUNT_SATOSHI[k]), i)
        is_change = [random.randrange(2) for ii in range(i)]
        input_index = [str(random.randrange(MAX_INPUT_INDEX)) for ii in range(i)]
        inputs = [{"prev_hash":prev_hash[ii],
                   "prev_index":prev_index[ii],
                   "amount":input_amounts[ii],
                   "change":is_change[ii],
                   "index":input_index[ii]} for ii in range(i)]

	# Generate outputs. Fees must be less than 1 BTC or less than 10% of
	# the total GATEWAY output amount. We just make the fee below 10% of 
        # GATEWAY output totals
        input_total = sum(input_amounts)
        has_change = random.randrange(2)
        output_change = random.randrange(input_total) if has_change else 0
        fee = int(random.uniform(0, 1./11) * (input_total - output_change))
        output_gateway_total = input_total - output_change - fee
        assert (output_gateway_total > 0), "Gateway output total not positive"
        output_amounts = []

        for jj in range(j):
          remainder = output_gateway_total - sum(output_amounts)
          if jj == j-1:
            output_amounts.append(remainder)
          else:
            output_amounts.append(random.randrange(remainder) if remainder > 0 else 0)

        output_index = random.sample(range(MAX_OUTPUT_INDEX), j)
        outputs = [{"destination":"Gateway",
                    "amount":output_amounts[jj],
                    "index":str(output_index[jj])} for jj in range(j)]
        if has_change:
            outputs.append({"destination":"Change",
                            "amount":output_change,
                            "index":0})

        assert sum(input_amounts) == sum(output_amounts) + output_change + fee,\
            "Input amounts do not equal output amounts"

        request_json = {"wallet":WALLET_ID,
                        "inputs":inputs,
                        "outputs":outputs,
                        "locktTime":0,
                        "currency":"",
                        "rate":""}

        request_json_list.append(request_json)

  return request_json_list

def get_b64protobuf_from_json(jsn):
  """Get a base64-encoded protobuf from a json description.

  Make a POST request to subzero server running on localhost to get back a based-64 encoded
  string containing the proto buffer for the request
  """
  headers = {"Content-Type": "application/json"}
  url = "http://localhost:8080/generate-qr-code/sign-tx-request"
  request = requests.post(url, json=jsn, headers=headers)
  return json.loads(request.text)["data"]

def generate_qr_requests():
  """Create a list of QR-encoded transaction requests.

  Generate a pseudo-random list of base64-encoded transaction protobufs as QR inputs for subzero
  to consume/sign
  """
  return [get_b64protobuf_from_json(jsn) for jsn in generate_json_requests()]

def generate_expect_script(qr_requests):
  """Generate an expect script to be fed into subzero GUI.

  Return a string that can be used as the expect script for (raw) test vector collection
  """
  preamble = """#!/usr/bin/expect -f

  if {![llength $argv]} {
    puts "Usage $argv0 /path/to/subzero/gui.jar"
    exit
  }

  set subzero_jar [lindex $argv 0]
  log_file expect_subzero.log
  set timeout -1
  spawn "$subzero_jar"
  """

  expect_template = """
  expect "Please scan the printed QR-Code using the red scanner"
  send -- "$request\\r"

  expect "Type exactly \\"yes\\" + <enter> to approve or \\"no\\" + <enter> to abort."
  send -- "yes\\r"

  expect "Then type 'exit' + <enter> or 'restart' + <enter>."
  send -- "restart\\r"
  """

  # Construct expect script from qr_request
  es = preamble
  for qrr in qr_requests:
    es = es + Template(expect_template).substitute(request=qrr)

  return es

if __name__ == "__main__":
  qr_requests = generate_qr_requests()
  print(generate_expect_script(qr_requests))
