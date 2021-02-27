#!/bin/bash

# mount the SecWorld (12.50.2) + CodeSafe (12.63.0) image so we can copy stuff off it to our target image
sudo mkdir /media/SecWorld && sudo mount -t iso9660 -o ro /vagrant/CodeSafe-linux64-dev-12.50.2.iso /media/SecWorld
sudo mkdir /media/CodeSafe && sudo mount -t iso9660 -o ro /vagrant/Codesafe_Lin64-12.63.0.iso  /media/CodeSafe
