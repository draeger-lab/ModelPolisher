#!/bin/bash
#
# Launches the ModelPolisher with the options given below.
#
# Author: Andreas Draeger, University of California, San Diego.
#
## Find location of this script ##
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

${DIR}/ModelPolisher.sh \
--user=[USER NAME] \
[
OPTIONAL:
--host=[HOST NAME] \
--port=[PORT] \
]
--dbname=[DATABASE NAME] \
--passwd=[DB USER'S PASSWORD] \
--input=[INPUT FILE or DIRECTORY] \
--output=[OUTPUT FILE or DIRECTORY] \
--compression-type=GZIP \
--check-mass-balance=true \
--sbml-validation=true \
--omit-generic-terms=false \
--log-level=INFO \
--log-file=${DIR}/model_polisher.log 2>/dev/null \
--annotate-with-bigg=false [USE BIGG DB FOR ANNOTATION?] \
--include-any-uri=false \
--flux-coefficients=[COMMA SEPARATED LIST] \
--flux-objectives=[COLON SEPARATED LIST] \
--document-title-pattern="[biggId] - [organism]" \
--document-notes-file="SBMLDocumentNotes.html" \
--model-notes-file="ModelNotes.html"
exit 0
