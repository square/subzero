/**
 * nCipher wallet code.
 */

#include <arpa/inet.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <unistd.h>

#include "check_ver.h"
#include "checks.h"
#include "config.h"
#include "hash.h"
#include "init.h"
#include "log.h"
#include "memzero.h"
#include "nanopb_stream.h"
#include "pb_decode.h"
#include "pb_encode.h"
#include "print.h"
#include "rpc.h"

int main(int argc, char **argv) {
  int r;

  DEBUG("in main");

  r = init();
  if (r < 0) {
    ERROR("init() failed (%d).", r);
    cleanup();
    return r;
  }

  // check version and protect against rollback
  check_ver();

  INFO("running self checks.");
  r = run_self_checks();
  if (r != 0) {
    ERROR("run_self_checks failed");
    cleanup();
    return -1;
  }
  INFO("self-checks passed.");

  // This is ugly, but it's probably not worth parsing arguments for a single
  // case.
  if (argc > 1) {
    if (strcmp(argv[1], "--checks-only") == 0) {
      cleanup();
      return 0;
    }
    ERROR("the only valid argument is --checks-only");
    return -1;
  }

  // Spin up a server to handle requests
  struct sockaddr_in serveraddr;
  memzero(&serveraddr, sizeof(serveraddr));
  serveraddr.sin_family = AF_INET;
  serveraddr.sin_port = htons(PORT);
  serveraddr.sin_addr.s_addr = inet_addr("0.0.0.0");

  int server;
  server = socket(AF_INET, SOCK_STREAM, 0);
  if (server == -1) {
    ERROR("server socket creation failed.");
    cleanup();
    return -1;
  }

  int optval = 1;
  setsockopt(server, SOL_SOCKET, SO_REUSEADDR, &optval, sizeof(optval));

  if (bind(server, (struct sockaddr *)&serveraddr, sizeof(serveraddr)) != 0) {
    ERROR("server socket binding failed.");
    cleanup();
    return -1;
  }

  if (listen(server, SOMAXCONN) != 0) {
    ERROR("server socket listening failed.");
    cleanup();
    return -1;
  }

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wmissing-noreturn"
  while (1) {
    int client;
    INFO("waiting for client.");
    client = accept(server, 0, 0);
    if (client < 0) {
      ERROR("accept failed.");
      continue;
    }
    INFO("client connected.");
    pb_istream_t input = pb_istream_from_socket(client);
    pb_ostream_t output = pb_ostream_from_socket(client);
    handle_incoming_message(&input, &output);

    close(client);
  }
#pragma clang diagnostic pop
}
