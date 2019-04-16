#!/bin/bash
/opt/vc/bin/tvservice -o

cd /home/pi/ProjetNepal/
echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/unbind

sudo java Serveur
