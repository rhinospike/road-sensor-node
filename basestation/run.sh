#!/bin/bash

HOME=/home/alarm
BINDIR=$HOME/repos/road-sensor-node/basestation
VENVDIR=$BINDIR/venv

cd $BINDIR
source $VENVDIR/bin/activate
$BINDIR/basestation.py
