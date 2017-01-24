#!/bin/bash
#
# Launches the ModelPolisher with the options given below.
#
# Author: Andreas Draeger, University of California, San Diego.
#
## Find location of this script ##
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# Default argument values, all other values have to be provided as 
# arguments for the given mode
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
    --omit-generic-terms=*) OMIT_GENERIC_TERMS="${OPT#*=}";;
    --log-level=*) LOG_LEVEL="${OPT#*=}";;
    --log-file=*) LOG_FILE="${OPT#*=}";;
    --include-any-uri=*) INCLUDE_ANY_URI="${OPT#*=}";;
    --flux-coefficients=*) FLUX_COEFFICIENTS="${OPT#*=}";;
    --flux-objectives=*) FLUX_OBJECTIVES="${OPT#*=}";;
    --document-title-pattern=*) DOCUMENT_TITLE_PATTERN="${OPT#*=}";;
    --model-notes-file=*) MODEL_NOTES_FILE="{OPT#*=}";;
    --document-notes-file=*) DOCUMENT_NOTES_FILE="{OPT#*=}";;	
  esac
done

if $ANNOTATE
then
# Completion with annotation
ARGS="\
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
--log-file=$LOG_FILE"
else
# Only completion
ARGS="\
--input=$INPUT \
--output=$OUTPUT \
--compression-type=$COMPRESSION_TYPE \
--check-mass-balance=$CHECK_MASS_BALANCE \
--sbml-validation=$SBML_VALIDATION \
--omit-generic-terms=$OMIT_GENERIC_TERMS \
--log-level=$LOG_LEVEL \
--log-file=$LOG_FILE"
fi


# Optional Arguments
if [ -n "$INCLUDE_ANY_URI" ]
then
    ARGS+=" --include-any-uri=$INCLUDE_ANY_URI"
fi
if [ -n "$FLUX_COEFFICENTS" ]
then
    ARGS+=" --flux-coefficients=$FLUX_COEFFICIENTS"
fi
if [ -n "$FLUX_OBJECTIVES" ]
then
    ARGS+=" --flux-objectives=$FLUX_OBJECTIVES"
fi
if [ -n "$DOCUMENT_TITLE_PATTERN" ]
then
    ARGS+=" --document-title-pattern=$DOCUMENT_TILE_PATTERN"
fi
if [ -n "$MODEL_NOTES_FILE" ]
then
    ARGS+=" --model-notes-file=$MODEL_NOTES_FILE" 
fi
if [ -n "$DOCUMENT_NOTES_FILE" ]
then
    ARGS+=" --document-notes-file=$DOCUMENT_NOTES_FILE" 
fi

"${DIR}/ModelPolisher.sh" $ARGS 

if [ -e "${DIR}/ModelPolisherTemplate.sh" ] && [ ! -e "${DIR}/ModelPolisherTemplate.bckp" ];
then
  mv "${DIR}/ModelPolisherTemplate.sh" "${DIR}/ModelPolisherTemplate.bckp"
fi
echo "${DIR}/ModelPolisher.sh $ARGS" > "${DIR}/ModelPolisherTemplate.sh"

exit 0
