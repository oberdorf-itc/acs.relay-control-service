#!/bin/sh
#############################################################################
# Michael Oberdorf IT-Consulting - ACS Relay Control Service
#----------------------------------------------------------------------------
# Author: Michael Oberdorf <info@oberdorf-itc.de>
# Daten: 2023-07-25
# Last modified by: Michael Oberdorf
# Last modified on: 2026-04-09
#############################################################################

#----------------------------------------------------------------------------
# Definition of global parameters
#----------------------------------------------------------------------------
# defining central directories
CONF_DIR='/app/etc'
LIB_DIR='/app/lib'

# Some program specific parameters
JAVA='/usr/bin/java'
file_encoding='UTF-8'
log4jConf="${CONF_DIR}/log4j2.xml"

mainClass='de.oberdorf_itc.acs.RelayControlService'
class_path=''

ulimit -c unlimited

# Generation of java classpath
for jarFile in `find ${LIB_DIR} -type f -name "*.jar" | sort`
do
  if [ -z "${class_path}" ]; then
    class_path="-classpath ${jarFile}"
  else
    class_path="${class_path}:${jarFile}"
  fi
done

# combine java options
JAVA_OPTS="--enable-native-access=ALL-UNNAMED -Xms128m -Xmx512m -verbose:gc -Dfile.encoding=${file_encoding} -Dlog4j.configurationFile=${log4jConf} -Djava.io.tmpdir=/tmp ${class_path}"
#JAVA_OPTS="${JAVA_OPTS} -XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:${gc_log}"

#############################################################################
# M A I N
#############################################################################

#----------------------------------------------------------------------------
# Start server
#----------------------------------------------------------------------------
echo "Starting OITC ACS Relay control service ..."
CMD="${JAVA} ${JAVA_OPTS} ${mainClass} ${generalConf}"
echo "${CMD}"
${CMD}
