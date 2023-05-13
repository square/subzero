#include "init.h"

#include "log.h"
#include "module_certificate.h"
#include "squareup/subzero/internal.pb.h"

#include <nfastapp.h>

NFastApp_Connection conn;
NFast_AppHandle app;
M_CertificateList cert_list;
M_Certificate certs[SEE_CERT_SIGNER_SIZE];
static int initialized = false;

/**
 * Connect to the nCipher.
 *
 * returns -1 if nCipher init fails.
 * returns -2 if nCipher connect fails.
 */
 //TODO: convert to Result
int init(void) {
  NFastAppInitArgs ia = {0};
  M_Status rc;

  if ((rc = NFastApp_InitEx(&app, &ia, NULL))) {
    ERROR("NFastApp_InitEx failed: %s", NF_Lookup(rc, NF_Status_enumtable));
    return -1;
  }

  if ((rc = NFastApp_Connect(app, &conn, 0, NULL))) {
    ERROR("NFastApp_Connect failed: %s", NF_Lookup(rc, NF_Status_enumtable));
    NFastApp_Finish(app, NULL);
    return -2;
  }

  Result r = module_certificate_init(&cert_list, certs);
  if (r != Result_SUCCESS) {
    ERROR("get_module_certificate_list failed (%d)", r);
    NFastApp_Finish(app, NULL);
    return -3;
  }

  initialized = true;
  return 0;
}

void cleanup(void) {
  if (initialized) {
    NFastApp_Disconnect(conn, NULL);
    NFastApp_Finish(app, NULL);
    initialized = false;
    module_certificate_cleanup(&cert_list);
  }
}
