#!/bin/bash

clear

printf "\n\n >> Generating Java source from proto \n\n\n"

protoc -I=proto --java_out=src/main/java --grpc-java_out=src/main/java proto/*.proto

if [ $? -ne 0 ]; then
  printf "Cannot generate source from proto file"
  exit 1
fi

python3 replace_code.py 2> /dev/null

if [ $? -ne 0 ]; then
  printf "Cannot perform replacing old javax packages of generated code by gRPC\nYou can download python and try again"
  exit 1
fi

export JAVA_HOME="/home/loc/dev-kit/jdk-11"
export MAVEN_HOME="/home/loc/dev-kit/maven"
export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/games:/usr/local/games:/snap/bin:$JAVA_HOME/bin:$MAVEN_HOME/bin"

printf "\n\n >> Checking java version \n\n\n"

java -version

printf "\n\n >> Checking maven version \n\n\n"

mvn --version

mvn \
 -Dmaven.wagon.http.ssl.insecure=true \
 -Dmaven.wagon.http.ssl.allowall=true \
 -Dmaven.wagon.http.ssl.ignore.validity.dates=true \
 -Dmaven.resolver.transport=wagon \
 dependency:resolve \
 clean \
 install \
 -DskipTests=true \
 -Dfile.encoding=UTF8 \
 -f pom.xml
