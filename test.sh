#!/bin/bash

if [ "x$JBOSS_HOME" == "x" ]; then
  echo "Please set JBOSS_HOME"
  exit 1
fi

java -cp $JBOSS_HOME/bin/client/jboss-client.jar:target/test-classes:target/classes com.redhat.gss.Client
