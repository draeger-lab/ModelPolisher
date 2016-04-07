#!/bin/bash
###############################################
# This script installs the required java libraries not available via maven repositories from the 
# lib folder in the local maven repository
# 
# Must be run/updated when dependencies change.
###############################################

# lib directory
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )

# JSBML
mvn install:install-file -DgroupId=org.sbml -DartifactId=JSBML-incl-libs -Dversion=2492 -Dfile=$DIR/JSBML-incl-libs-2492.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true

# SysBio
mvn install:install-file -DgroupId=de.zbit -DartifactId=SysBio -Dversion=1390 -Dfile=$DIR/SysBio-1390.jar -Dpackaging=jar -DgeneratePom=true -DlocalRepositoryPath=$DIR -DcreateChecksum=true
