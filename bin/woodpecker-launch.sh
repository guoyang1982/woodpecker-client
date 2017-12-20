#!/bin/bash
${JAVA_HOME}/bin/java -Xbootclasspath/a:${JAVA_HOME}/lib/tools.jar -jar wpclient-agent-core.jar -pid $1 -config woodpecker.properties