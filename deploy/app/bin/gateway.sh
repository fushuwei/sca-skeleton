#!/bin/bash
cd /data/projects/sca-skeleton || exit 1
JAR="./bin/sca-skeleton-gateway-1.0.0.jar"
pkill -9 -f "$JAR"
sleep 1
set -a
source ./bin/.env
set +a
nohup java -jar "$JAR" &
