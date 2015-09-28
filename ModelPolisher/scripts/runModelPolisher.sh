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
--log-file=model_polisher.log 2>/dev/null

exit 0
