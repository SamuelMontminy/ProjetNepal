#!/bin/bash

/opt/vc/bin/tvservice --off

cd /home/pi/ProjetNepal/

pi4j -r ClientCentrifugeuse.java 192.168.4.1 2228
