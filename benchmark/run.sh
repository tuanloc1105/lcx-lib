#!/bin/bash

clear

export JAVA_HOME="/home/loc/dev-kit/jdk-8"
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin:$JAVA_HOME/bin"

printf "\n\n >> Checking java version \n\n\n"

java -version

java \
 -jar \
 target/example-1.0.0.jar
