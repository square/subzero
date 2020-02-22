// TODO: implement some kind of error handling.
// TODO: add an input listener and save form state.
//       needs to handle the tricky case of variable length inputs/outputs.

// The statefulness aspect of this code has made it quite monstrous. Might be worth investigating
// using a real framework...

function pretty_print_request() {
  $('#pretty_print_error').hide();
  $('#pretty_print_response').hide();
  $.ajax("/pretty-print/request", {
    data: {"raw": pretty_print_raw.value}
  })
  .fail((jqXhr, status) => {
    $('#pretty_print_error').text(status + ": " + jqXhr.responseText);
    $('#pretty_print_error').slideDown();
  })
  .done(result => {
    pretty_print_json.value = result;
    $('#pretty_print_response').slideDown();
  });
}

function pretty_print_response() {
  $('#pretty_print_error').hide();
  $('#pretty_print_response').hide();
  $.ajax("/pretty-print/response", {
    data: {"raw": pretty_print_raw.value}
  })
  .fail((jqXhr, status) => {
    $('#pretty_print_error').text(status + ": " + jqXhr.responseText);
    $('#pretty_print_error').slideDown();
  })
  .done(result => {
    pretty_print_json.value = result;
    $('#pretty_print_response').slideDown();
  });
}

function init_wallet_request() {
  $('#init_wallet_response').slideDown();
  $.ajax("/generate-qr-code/init-wallet-request", {
    data: {"wallet": wallet.value|0}
  })
  .done(result => {
    init_wallet_response_data.innerText = result.data;
    var r = Math.floor(init_wallet_response_qr.width / result.size);
    init_wallet_response_qr.width = r * result.size;
    init_wallet_response_qr.height = r * result.size;

    var ctx = init_wallet_response_qr.getContext('2d');
    var i = 0;
    for (var x=0; x<result.size; x++) {
      for (var y=0; y<result.size; y++) {
        if (result.pixels[i++]) {
          ctx.fillRect(x * r, y * r, r, r);
        }
      }
    }
  });
}

function finalize_wallet_request() {
  $('#finalize_wallet_error').hide();
  $('#finalize_wallet_response').hide();

  var data = {"wallet": wallet.value|0, "encPubKeys": []};
  for (var i=1; i<=N.innerText; i++) {
    data.encPubKeys.push($('#init_response_' + i).val())
  }

  $.ajax("/generate-qr-code/finalize-wallet-request", {traditional: true, data: data})
  .fail((jqXhr, status) => {
    $('#finalize_wallet_error').text(status + ": " + jqXhr.responseText);
    $('#finalize_wallet_error').slideDown();
  })
  .done(result => {
    finalize_wallet_response_data.innerText = result.data;
    var r = Math.floor(finalize_wallet_response_qr.width / result.size);
    finalize_wallet_response_qr.width = r * result.size;
    finalize_wallet_response_qr.height = r * result.size;

    var ctx = finalize_wallet_response_qr.getContext('2d');
    var i = 0;
    for (var x=0; x<result.size; x++) {
      for (var y=0; y<result.size; y++) {
        if (result.pixels[i++]) {
          ctx.fillRect(x * r, y * r, r, r);
        }
      }
    }
    $('#finalize_wallet_response').slideDown();
  });
}

function reveal_xpubs() {
  $('#reveal_xpubs_error').hide();
  $('#reveal_xpubs_response').hide();

  var data = {"finalizeResponses": []};
  for (var i=1; i<=N.innerText; i++) {
    data.finalizeResponses.push($('#finalize_response_' + i).val())
  }

  $.ajax("/compute/xpubs", {traditional: true, data: data})
  .fail((jqXhr, status) => {
    $('#reveal_xpubs_error').text(status + ": " + jqXhr.responseText);
    $('#reveal_xpubs_error').slideDown();
  })
  .done(results => {
    reveal_xpubs_response_data.value = results.join("\n");
    $('#reveal_xpubs_response').slideDown();
  });
}

function derive_address() {
  $('#derive_address_error').hide();
  $('#derive_address_response').hide();

  var data = {"finalizeResponses": []};
  for (var i=1; i<=N.innerText; i++) {
    data.finalizeResponses.push($('#derive_address_finalize_response_' + i).val())
  }
  data.change = $('#derive_address_change').prop('checked');
  data.index = $('#derive_address_index').val();

  $.ajax("/compute/address", {traditional: true, data: data})
  .fail((jqXhr, status) => {
    $('#derive_address_error').text(status + ": " + jqXhr.responseText);
    $('#derive_address_error').slideDown();
  })
  .done(results => {
    derive_address_response_data.value = results;
    $('#derive_address_response').slideDown();
  });
}

function sign_tx_request() {
  $('#sign_tx_error').hide();
  $('#sign_tx_response').hide();

  var data = {
    "wallet": wallet.value|0,
    inputs: serializeSignTxInputs(),
    outputs: serializeSignTxOutputs(),
    lockTime: lock_time.value|0,
    currency: currency.value,
    rate: exchange_rate.value
  };
  $.ajax("/generate-qr-code/sign-tx-request", {type: "POST", contentType: "application/json", processData: false, data: JSON.stringify(data)})
  .fail((jqXhr, status) => {
    $('#sign_tx_error').text(status + ": " + jqXhr.responseText);
    $('#sign_tx_error').slideDown();
  })
  .done(result => {
    sign_tx_response_data.innerText = result.data;
    var r = Math.floor(sign_tx_response_qr.width / result.size);
    sign_tx_response_qr.width = r * result.size;
    sign_tx_response_qr.height = r * result.size;

    var ctx = sign_tx_response_qr.getContext('2d');
    var i = 0;
    for (var x=0; x<result.size; x++) {
      for (var y=0; y<result.size; y++) {
        if (result.pixels[i++]) {
          ctx.fillRect(x * r, y * r, r, r);
        }
      }
    }
    $('#sign_tx_response').slideDown();
  });
}

function show_final_transaction() {
  $('#show_final_transaction_error').hide();
  $('#show_final_transaction_response').hide();
  var data = {
    signTxRequest: initial_sign_tx_request.value,
    finalizeResponses: [],
    gateway: gateway.value,
    signTxResponses: []
  };
  for (var i=1; i<=N.innerText; i++) {
    data.finalizeResponses.push($('#initial_finalize_response_' + i).val());
  }
  for (var i=1; i<=M.innerText; i++) {
    data.signTxResponses.push($('#sign_tx_response_' + i).val());
  }
  $.ajax("/show-final-transaction", {traditional: true, data: data})
  .fail((jqXhr, status) => {
    $('#show_final_transaction_error').text(status + ": " + jqXhr.responseText);
    $('#show_final_transaction_error').slideDown();
  })
  .done(results => {
    show_final_transaction_response_data.value = results;
    $('#show_final_transaction_response').slideDown();
  });
}

function addInput(is_loading) {
  var group = $('<div class="group"></div>');
  group.append('<p>prev hash: <input class="prev_hash" type="text" size="64"></p>');
  group.append('<p>output index: <input class="prev_index" type="text" size="4"> (0-indexed)</p>');
  group.append('<p>amount: <input class="amount" type="text"> (in Satoshi)</p>');
  group.append('<p>change: <input class="change" type="checkbox"></p>');
  group.append('<p>index: <input class="index" type="text" size="10"> (BIP-32)</p>');
  var button = $('<button class="small">remove</button>');
  button.click(_ => {
    group.fadeOut(400, _ => {
      group.remove();
      saveState();
    });
  });
  group.append(button);
  group.hide().appendTo('#inputs').fadeIn();
  if (!is_loading) {
    saveState();
  }
  return group;
}

function addOutput(is_loading) {
  var group = $('<div class="group"></div>');
  group.append('<p>destination: <select class="destination"><option>Gateway</option><option>Change</option></select></p>');
  group.append('<p>amount: <input class="amount" type="text"> (in Satoshi)</p>');
  group.append('<p>index: <input class="index" type="text" size="10"> (BIP-32)</p>');
  var button = $('<button class="small">remove</button>');
  button.click(_ => {
    group.fadeOut(400, _ => {
      group.remove();
      saveState();
    });
  });
  group.append(button);
  group.hide().appendTo('#outputs').fadeIn();
  if (!is_loading) {
    saveState();
  }
  return group;
}

function serializeSignTxInputs() {
  var inputs = [];
  $('#inputs .group').each((_, e) => {
    var input = {};
    input.prev_hash = $(e).find('.prev_hash').val();
    input.prev_index = $(e).find('.prev_index').val();
    input.amount = $(e).find('.amount').val()|0;
    input.change = $(e).find('.change').prop('checked');
    input.index = $(e).find('.index').val()|0;
    inputs.push(input);
  });
  return inputs;
}

function serializeSignTxOutputs() {
  var outputs = [];
  $('#outputs .group').each((_, e) => {
    var output = {};
    output.destination = $(e).find('.destination').val();
    output.amount = $(e).find('.amount').val()|0;
    output.index = $(e).find('.index').val()|0;
    outputs.push(output);
  });
  return outputs;
}

// TODO: use an array instead, so we can handle M or N changing. There's also some code that
// can be abstracted away / refactored for better re-use.
function saveState() {
  var state = {};
  // Pretty print
  state.pretty_print = pretty_print_raw.value;

  // Configuration section
  state.wallet = wallet.value|0;
  state.currency = currency.value;
  state.exchange_rate = exchange_rate.value;

  // Finalize section
  var encPubKeys = [];
  for (var i=1; i<=N.innerText; i++) {
    encPubKeys.push($('#init_response_' + i).val());
  }
  state.encPubKeys = encPubKeys;

  // Reveal section
  var finalizeResponses = [];
  for (var i=1; i<=N.innerText; i++) {
    finalizeResponses.push($('#finalize_response_' + i).val());
  }
  state.finalizeResponses = finalizeResponses;

  // Derive address section
  var deriveAddressFinalizeResponses = [];
  for (var i=1; i<=N.innerText; i++) {
    deriveAddressFinalizeResponses.push($('#derive_address_finalize_response_' + i).val());
  }
  state.deriveAddressFinalizeResponses = deriveAddressFinalizeResponses;
  state.deriveAddressChange = $('#derive_address_change').prop('checked');
  state.deriveAddressIndex = $('#derive_address_index').val();

  // Sign transaction section
  state.lock_time = lock_time.value;
  state.inputs = serializeSignTxInputs();
  state.outputs = serializeSignTxOutputs();

  // Merge signatures
  state.sign_tx_request = initial_sign_tx_request.value;
  var initial_finalize_responses = [];
  for (var i=1; i<=N.innerText; i++) {
    initial_finalize_responses.push($('#initial_finalize_response_' + i).val());
  }
  state.initial_finalize_responses = initial_finalize_responses;
  state.gateway = gateway.value;

  var sign_tx_responses = [];
  for (var i=1; i<=M.innerText; i++) {
    sign_tx_responses.push($('#sign_tx_response_' + i).val());
  }
  state.sign_tx_responses = sign_tx_responses;

  console.log("saving state");
  alok = state;
  window.localStorage.setItem('subzero_form_state', JSON.stringify(state));
}

function loadState() {
  var state = window.localStorage.getItem('subzero_form_state');
  state = JSON.parse(state);
  if (state == null) {
    // default state
    addInput(true);
    addOutput(true);
    return;
  }

  // Pretty print
  pretty_print_raw.value = state.pretty_print;

  // Configuration section
  wallet.value = state.wallet;
  currency.value = state.currency;
  exchange_rate.value = state.exchange_rate;

  // Finalize section
  for (var i=1; i<=N.innerText; i++) {
    $('#init_response_' + i).val(state.encPubKeys[i-1]);
  }

  // Reveal section
  for (var i=1; i<=N.innerText; i++) {
    $('#finalize_response_' + i).val(state.finalizeResponses[i-1]);
  }

  // Derive address section
  for (var i=1; i<=N.innerText; i++) {
    $('#derive_address_finalize_response_' + i).val(state.deriveAddressFinalizeResponses[i-1]);
  }
  $('#derive_address_change').prop('checked', state.deriveAddressChange);
  $('#derive_address_index').val(state.deriveAddressIndex);

  // Sign transaction section
  lock_time.value = state.lock_time;
  for (var i=0; i<state.inputs.length; i++) {
    var input = state.inputs[i];
    var el = addInput(true);
    el.find('.prev_hash').val(input.prev_hash);
    el.find('.prev_index').val(input.prev_index);
    el.find('.amount').val(input.amount);
    el.find('.change').prop('checked', input.change);
    el.find('.index').val(input.index);
  }
  for (var i=0; i<state.outputs.length; i++) {
    var output = state.outputs[i];
    var el = addOutput(true);
    el.find('.destination').val(output.destination);
    el.find('.amount').val(output.amount);
    el.find('.index').val(output.index);
  }

  // Merge signatures
  initial_sign_tx_request.value = state.sign_tx_request;
  for (var i=1; i<=N.innerText; i++) {
    $('#initial_finalize_response_' + i).val(state.initial_finalize_responses[i-1]);
  }
  gateway.value = state.gateway;

  for (var i=1; i<=M.innerText; i++) {
    $('#sign_tx_response_' + i).val(state.sign_tx_responses[i-1]);
  }
}

$(document).ready(_ => {
  $.ajax("/constants").done(result => {
    M.innerText = result.m;
    N.innerText = result.n;

    for (var i=1; i<=result.n; i++) {
      var e = $('<p>Initialize response ' + i + ': <textarea id="init_response_' + i + '"></textarea></p>');
      $('#init_responses').append(e);

      e = $('<p>Finalize response ' + i + ': <textarea id="finalize_response_' + i + '"></textarea></p>');
      $('#finalize_response').append(e);

      e = $('<p>Finalize response ' + i + ': <textarea id="derive_address_finalize_response_' + i + '"></textarea></p>');
      $('#derive_address_finalize_response').append(e);

      e = $('<p>Initial finalize response ' + i + ': <textarea id="initial_finalize_response_' + i + '"></textarea></p>');
      $('#initial_finalize_responses').append(e);
    }
    for (var i=1; i<=result.m; i++) {
      e = $('<p>Sign transaction response ' + i + ': <textarea id="sign_tx_response_' + i + '"></textarea></p>');
      $('#merge_responses').append(e);
    }
    loadState();
    document.onchange = (_ => saveState());
  })
});
