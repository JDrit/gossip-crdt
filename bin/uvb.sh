#!/bin/bash
##
## UVB server
##
VERSION="1.0"

if [ -z "$UVB_HOME" ]; then
    echo "UVB_HOME is not set, aborting.";
    exit 1;
fi

# We want to make sure we're using a predictable java
# environment. Look for JAVA_HOME, and then within it
# grab the java executable.
#
if [ -z "$JAVA_HOME" ]; then
    echo "JAVA_HOME is not set, aborting.";
    exit 1;
else
    for java in "$JAVA_HOME/bin/java"; do
        if [ -x "$java" ]; then
            JAVA="$java"
            break;
        fi
    done
fi

launch_pulse()
{
    fn_path="$UVB_HOME/build/runtime.classpath"
    quasar_path="$UVB_HOME/build/quasar-core.path"

    if [ -f "$fn_path" ]; then
        fn_classpath_content=$(cat "$fn_path")
    else
        echo "Unable to find runtime.classpath, maybe run gradle writeClasspath?"
	exit 1
    fi

    if [ -f "$quasar_path" ]; then
	quasar_path_content=$(cat "$quasar_path")
    else
	echo "Unable to find quasar.path, maybe run gradle writeClasspath?"
	exit 1
    fi

    UVB_CLASSPATH="$fn_classpath_content"

    JVM_OPTS+=" -server -Xmx1g -javaagent:$quasar_path_content"
    JVM_OPTS+=" -Dco.paralleluniverse.fibers.verifyInstrumentation=true"

    "$JAVA" $JVM_OPTS -classpath $UVB_CLASSPATH net.batchik.crdt.Main "$@"
}

launch_pulse "$@"
