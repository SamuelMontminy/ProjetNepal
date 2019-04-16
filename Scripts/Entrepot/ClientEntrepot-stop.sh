#!/bin/bash
# Grabs and kill a process from the pidlist that has the word myapp

pid=`ps aux | grep ClientEntrepot | awk '{print $2}'`
kill -9 $pid
