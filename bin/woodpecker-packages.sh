#!/bin/bash

# woodpecker's target dir
WOODPECKER_TARGET_DIR=../target/wpclient-agent

# woodpecker's properties
WOODPECKER_PROPERTIES=../wpclient-agent/src/main/resources/woodpecker.properties

# exit shell with err_code
# $1 : err_code
# $2 : err_msg
exit_on_err()
{
    [[ ! -z "${2}" ]] && echo "${2}" 1>&2
    exit ${1}
}

mvn clean package -Dmaven.test.skip=true -f ../pom.xml \
|| exit_on_err 1 "package woodpecker failed."

# reset the target dir
mkdir -p ${WOODPECKER_TARGET_DIR}

# copy jar to TARGET_DIR
cp ../wpclient-agent/target/wpclient-agent-1.0-SNAPSHOT-jar-with-dependencies.jar ${WOODPECKER_TARGET_DIR}/wpclient-agent.jar
cp ../wpclient-agent-core/target/wpclient-agent-core-1.0-SNAPSHOT-jar-with-dependencies.jar ${WOODPECKER_TARGET_DIR}/wpclient-agent-core.jar

# copy woodpecker.properties to TARGET_DIR
chmod 777 ${WOODPECKER_PROPERTIES}
cp ${WOODPECKER_PROPERTIES} ${WOODPECKER_TARGET_DIR}/woodpecker.properties
chmod 777 ./woodpecker-run.sh
chmod 777 ./woodpecker-launch.sh
cp ./woodpecker-run.sh ${WOODPECKER_TARGET_DIR}
cp ./woodpecker-launch.sh ${WOODPECKER_TARGET_DIR}

cd ../target/
zip -r wpclient-agent.zip wpclient-agent/
cd -
