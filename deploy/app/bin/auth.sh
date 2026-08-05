#!/bin/bash
cd /data/projects/sca-skeleton || exit 1

# 构建：在项目根目录编译网关模块及其依赖模块
mvn clean package -pl sca-skeleton-backend/sca-skeleton-auth -am -DskipTests

# 停止旧进程（先停，避免占用 jar 文件导致覆盖异常）
pkill -9 -f "java -jar bin/dev/sca-skeleton-auth-1.0.0.jar"
sleep 1

# 部署：将构建产物拷贝到 bin/dev 目录（覆盖旧 jar）
cp sca-skeleton-backend/sca-skeleton-auth/target/sca-skeleton-auth-1.0.0.jar bin/dev/

# 加载环境变量并启动
set -a
source ./bin/dev/.env
set +a
nohup java -jar bin/dev/sca-skeleton-auth-1.0.0.jar > bin/dev/auth.log 2>&1 &
