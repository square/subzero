#pragma once

#include <seelib.h>

#include "config.h"
#include <squareup/plutus/internal.pb.h>

Result module_certificate_init(M_CertificateList *cert_list, M_Certificate certs[static SEE_CERT_SIGNER_SIZE]);
void module_certificate_cleanup(M_CertificateList *cert_list);