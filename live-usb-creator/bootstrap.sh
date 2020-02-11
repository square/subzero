#!/bin/bash

# mount the CodeSafe image so we can copy stuff off it to our target image
sudo mkdir /media/CodeSafe && sudo mount -t iso9660 -o ro /vagrant/CodeSafe-linux64-dev-12.50.2.iso /media/CodeSafe
