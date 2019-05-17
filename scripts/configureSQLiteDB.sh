#!/bin/bash
#---------------------------------------------------------------------------
# Setup database
#---------------------------------------------------------------------------
# Download and prepare the correct version of BiGG Models database
# from Dropbox.
#---------------------------------------------------------------------------

SCRIPTS_PATH="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"
cd "$SCRIPTS_PATH"

# check if all necessary programs are present
if [ ! $(which curl) ]; then
    echo "curl is missing!"
    exit 1
elif [ ! $(which unzip) ]; then
    echo "unzip is missing!"
    exit 1
elif [ ! $(which python3) ]; then
    echo "python3 is missing!"
    exit 1
fi

function check_md5sum() {
    WORKDIR=$(pwd)
    cd ../resources/edu/ucsd/sbrg/bigg/
    md5sum -c bigg.zip.md5
    # invert return value with XOR, we want to jump into condition if this fails
    VAL=$(($? ^ 1))
    cd "$WORKDIR"
    return $VAL
}

echo "Fetching checksum file"
curl -L -o bigg.zip.md5 https://www.dropbox.com/s/a7y8ag0rkgzs0y6/bigg.zip.md5?dl=0
cp bigg.zip.md5 ../resources/edu/ucsd/sbrg/bigg/

# check if db exists and is valid
if [ ! -f ../resources/edu/ucsd/sbrg/bigg/bigg.zip ] ||  check_md5sum; then
    # retry 3 times, if validation fails
    counter=0
    while [ ${counter} -lt 4 ] && { [ ! -f bigg.zip ] || ! md5sum -c bigg.zip.md5; }; do
        if [ ${counter} -gt 0 ]; then
            echo "Failed validating integrity of bigg.zip, retrying ${counter}/3"
        else
            echo "Fetching bigg.zip"
        fi
        curl -L -o bigg.zip https://www.dropbox.com/s/yxti7sba6hrhukb/bigg.zip?dl=0
        counter=$((counter+1))
    done
    # cleanup
    if [ -f bigg.zip.md5 ]; then
        rm bigg.zip.md5
    fi
    if [ -f ../resources/edu/ucsd/sbrg/bigg/bigg.zip.md5 ]; then
        rm ../resources/edu/ucsd/sbrg/bigg/bigg.zip.md5
    fi
    if [ ${counter} -eq 4 ]; then
        echo "Failed to verify integrity of bigg.zip, aborting"
        exit 2
    fi
    mv bigg.zip ../resources/edu/ucsd/sbrg/bigg/
fi

unzip -od ../resources/edu/ucsd/sbrg/bigg ../resources/edu/ucsd/sbrg/bigg/bigg.zip 
python3 createIndices.py
