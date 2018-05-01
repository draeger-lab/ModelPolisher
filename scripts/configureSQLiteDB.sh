#!/bin/bash

if [ ! -f ../resources/edu/ucsd/sbrg/bigg/bigg.zip ]; then
    curl -L -o bigg.zip https://www.dropbox.com/s/0j3pozc7a0p2b94/bigg.zip\?dl\=0
    mv bigg.zip ../resources/edu/ucsd/sbrg/bigg
fi

unzip -od ../resources/edu/ucsd/sbrg/bigg ../resources/edu/ucsd/sbrg/bigg/bigg.zip 
python3 createIndices.py
