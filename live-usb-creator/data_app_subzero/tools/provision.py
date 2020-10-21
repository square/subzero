#!/opt/nfast/python/bin/python

# this script provisions an nCipher HSM, assuming /opt/nfast/kmdata/local points
# to the corresponding world files

import nfpython
import nfkm
import os
import sys
import logging


def color_reset():
    print(u"\u001b[0m")

def color_cyan():
    print(u"\u001b[36m")

def display_user(message):
    logging.debug("display_user: " + message)
    color_cyan()
    print(message)
    color_reset()

def prompt_user(message):
    # this is to homogenize python 2/3
    try:
        input = raw_input
    except NameError:
        pass
    color_cyan()
    ret = input(message)
    color_reset()
    logging.debug("prompt_user: " + message)
    logging.debug("read from user: " + ret)
    return ret

def prompt_user_yes_no(message):
    ret = False
    while ret != 'yes' and ret != 'no':
        ret = prompt_user(message + "\n  Please type exactly 'yes' or 'no', followed by <ENTER>: ")
    return ret == 'yes'

def check_unrecoverable_condition(assertion, msg):
    if not assertion:
        logging.error("Unrecoverable error: " + msg)
        prompt_user("Press <ENTER> to continue...")
        sys.exit(1)

def check_recoverable_condition(f, msg):
    result = f()
    while not result:
        display_user(msg)
        prompt_user("Press <ENTER> to retry...")
        result = f()

def get_info():
    logging.debug("get_info")
    nfconn = nfpython.connection(needworldinfo=1)
    ret = nfkm.getinfo(nfconn)
    logging.debug("get_info returns: " + str(ret))
    return ret

def module_exists():
    # only one module is supported
    module = get_info().modules[0]
    logging.debug("module: ", str(module))
    return module.module == 1

def module_state():
    # returns "Usable", "PreInitMode", ...
    hardserver_restart()
    return get_info().modules[0].state

def module_is_usable():
    return module_state() == 'Usable'

def module_is_preinit():
    return module_state() == 'PreInitMode'

# this is idempotent
def hardserver_start():
    logging.debug("starting nc_hardserver")
    os.system("/etc/init.d/nc_hardserver start")

def hardserver_stop():
    logging.debug("stopping nc_hardserver")
    os.system("/etc/init.d/nc_hardserver stop")

def hardserver_restart():
    hardserver_stop()
    hardserver_start()

def check_preconditions():
    # On a fresh boot hardserver will normally be running,
    # but this script may be called after manual maintenance operations
    # that may leave hardserver stopped.
    # Start hardserver to cover this case
    hardserver_restart()

    logging.debug("World information: \n" + str(get_info()))

    check_unrecoverable_condition(module_exists(), "Could not find a module connected.")

    # TODO: check if module is already enrolled, and bail if so

    # While in operational mode, we can check whether the
    # internal override switch D is active. Unfortunately
    # this does not work when the HSM is in I mode, and
    # fails with nfpybase.NFStatusError: CrossModule,#1-Mode

    check_recoverable_condition(module_is_preinit, "HSM rear switch is not in I mode. Switch to I mode.")

    logging.debug("All preconditions are OK")

def link_kmdata():
    _WORLD_FILE = "/opt/nfast/kmdata/local/world"
    worldfile_exists = os.path.exists(_WORLD_FILE)

    if not worldfile_exists:
        logging.error("%s not found. Dropping a shell to fix."%_WORLD_FILE)

    if not worldfile_exists or prompt_user_yes_no("Drop a shell to manually modify symlinks? "):
        os.system("/etc/init.d/nc_hardserver stop")
        os.system("/bin/bash")
        os.system("/etc/init.d/nc_hardserver start")

    # While in operational mode, we can check whether a card is inserted as
    # get_info().modules[0].slots[0].slotstate == 'Admin'. Unfortunately
    # this does not work when the HSM is in I mode.
    display_user("Prepare ACS cards")

    logging.debug("Indoctrinating HSM")
    os.system("/opt/nfast/bin/new-world -l")

    check_recoverable_condition(module_is_usable, "HSM rear switch is not in O mode. Switch to O mode.")

    logging.debug("HSM is enrolled, we're done")
    display_user("HSM successfully provisioned. You may need to initialize NVRAM now")


logging.basicConfig(format='%(asctime)s,%(msecs)d [%(filename)s:%(lineno)d] %(message)s',
    datefmt='%Y-%m-%d:%H:%M:%S',
    stream=sys.stderr,
    level=logging.DEBUG)

def provision_hsm():
    check_preconditions()
    link_kmdata()

if __name__ == "__main__":
    provision_hsm()
