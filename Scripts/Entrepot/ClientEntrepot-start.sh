#!/bin/bash

/usr/bin/tvservice -o           #Désalimente le port HDMI

cd /home/pi/ProjetNepal/

pi4j -r ClientEntrepot.java 192.168.4.1 2228        #Démarre le code

