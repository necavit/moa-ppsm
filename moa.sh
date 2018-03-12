#! /bin/bash

# run MOA with the necessary libraries in the Java classpath
PPSM_JAR=./target/moa-ppsm-0.0.1-SNAPSHOT.jar
MOA_JAR=./target/lib/moa-2013.11.jar
AGENT_JAR=./target/lib/sizeofag-1.0.0.jar
WEKA_JAR=./target/lib/weka-dev-3.9.2.jar

java -cp $WEKA_JAR:$MOA_JAR:$PPSM_JAR -javaagent:$AGENT_JAR moa.DoTask "$@"
