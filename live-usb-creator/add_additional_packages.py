#!/usr/bin/env python2.7

# /vagrant/rhel7-livemedia.ks + additional packages from /vagrant/additional_packages = /tmp/rhel7-livemedia.with_additional_packages.ks

# make a copy of /vagrant/rhel7-livemedia.ks at /tmp/rhel7-livemedia.with_additional_packages.ks,
# after adding the additional packages listed in /vagrant/additional_packages

import pykickstart
import pykickstart.parser
import pykickstart.version

ksparser = pykickstart.parser.KickstartParser(pykickstart.version.makeVersion(pykickstart.version.DEVEL))
ksparser.readKickstart('/vagrant/rhel7-livemedia.ks')

with open('/vagrant/additional_packages', 'r') as f:
  for line in f:
    line = line.partition('#')[0]
    line = line.rstrip()
    if len(line) == 0:
      continue
    ksparser.handler.packages.add([line])

with open('/tmp/rhel7-livemedia.with_additional_packages.ks', 'w') as f:
  f.write(str(ksparser.handler))
