#!/bin/bash
#
# Launches the ModelPolisher with the options given below.
#
# Author: Andreas Draeger, University of California, San Diego.
#
## Find location of this script ##
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )


#Default argument values
ANNOTATE=false
COMPRESSION_TYPE=GZIP
CHECK_MASS_BALANCE=true
SBML_VALIDATION=true
OMIT_GENERIC_TERMS=false
LOG_LEVEL=INFO
LOG_FILE=${DIR}/model_polisher.log 2>/dev/null

for OPT in $*; do
  case $OPT in
    --user=*) USER="${OPT#*=}";;
    --dbname=*) DBNAME="${OPT#*=}";;
    --annotate-with-bigg=*) ANNOTATE="${OPT#*=}";;
    --passwd=*) PASSWD="${OPT#*=}";;
    --host=*) HOST="${OPT#*=}";;
    --port=*) PORT="${OPT#*=}";;
    --input=*) INPUT="${OPT#*=}";;
    --output=*) OUTPUT="${OPT#*=}";;
    --compression-type=*) COMPRESSION_TYPE="${OPT#*=}";;
    --check-mass-balance=*) CHECK_MASS_BALANCE="${OPT#*=}";;
    --sbml-validation=*) SBML_VALIDATION="${OPT#*=}";;
    --sbml-generic-terms=*) SBML_GENERIC_TERMS="${OPT#*=}";;
    --log-level=*) LOG_LEVEL="${OPT#*=}";;
    --log-file=*) LOG_FILE="${OPT#*=}";;
  esac
done

if [ $ANNOTATE ] 
then
${DIR}/ModelPolisher.sh \
--user=$USER \
--dbname=$DBNAME \
--annotate-with-bigg=$ANNOTATE \
--passwd=$PASSWD \
--host=$HOST \
--port=$PORT \
--input=$INPUT \
--output=$OUTPUT \
--compression-type=$COMPRESSION_TYPE \
--check-mass-balance=$CHECK_MASS_BALANCE \
--sbml-validation=$SBML_VALIDATION \
--omit-generic-terms=$OMIT_GENERIC_TERMS \
--log-level=$LOG_LEVEL \
--log-file=$LOG_FILE
else
${DIR}/ModelPolisher.sh \
--input=$INPUT \
--output=$OUTPUT \
--compression-type=$COMPRESSION_TYPE \
--check-mass-balance=$CHECK_MASS_BALANCE \
--sbml-validation=$SBML_VALIDATION \
--omit-generic-terms=$OMIT_GENERIC_TERMS \
--log-level=$LOG_LEVEL \
--log-file=$LOG_FILE
fi
exit 0
