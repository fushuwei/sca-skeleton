#!/bin/bash
cd /data/projects/sca-skeleton || exit 1

# 构建：在项目根目录编译网关模块及其依赖模块
mvn clean package -pl sca-skeleton-backend/sca-skeleton-gateway -am -DskipTests

# 部署：将构建产物拷贝到 bin/dev 目录
cp sca-skeleton-backend/sca-skeleton-gateway/target/sca-skeleton-gateway-1.0.0.jar bin/dev/

# 停止旧进程
pkill -9 -f "bin/dev/sca-skeleton-gateway-1.0.0.jar"
sleep 1

# 加载环境变量并启动
set -a
source ./bin/dev/.env
set +a
nohup java -jar bin/dev/sca-skeleton-gateway-1.0.0.jar > bin/dev/gateway.log 2>&1 &
