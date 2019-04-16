#!/bin/bash
# Grabs and kill a process from the pidlist that has the word myapp

echo '1-1' |sudo tee /sys/bus/usb/drivers/usb/bind

pid=`ps aux | grep Serveur | awk '{print $2}'`
kill -9 $pid
