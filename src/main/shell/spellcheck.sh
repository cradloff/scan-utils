#!/bin/sh
# Path where script resides
SU_PATH=$(dirname $0)
# Path to jar archive
JAR=$(ls $SU_PATH/scan-utils-*-jar-with-dependencies.jar)

java -classpath $SU_PATH:$JAR org.github.cradloff.scanutils.SpellCheck "$@"

