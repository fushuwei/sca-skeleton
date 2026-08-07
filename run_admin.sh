#!/bin/bash

set -e

# 进入项目根目录
cd /data/projects/sca-skeleton

# 设置系统代理
export http_proxy=http://127.0.0.1:7890 https_proxy=http://127.0.0.1:7890 all_proxy=socks5://127.0.0.1:7890

# 构建 JAR 包
mvn clean package

# 部署 gateway 服务
rm -rf ./bin/sca-skeleton-gateway-1.0.0.jar && cp ./sca-skeleton-backend/sca-skeleton-gateway/target/sca-skeleton-gateway-1.0.0.jar ./bin/

# 部署 auth 服务
rm -rf ./bin/sca-skeleton-auth-1.0.0.jar && cp ./sca-skeleton-backend/sca-skeleton-auth/target/sca-skeleton-auth-1.0.0.jar ./bin/

# 部署 system 服务
rm -rf ./bin/sca-skeleton-system-biz-1.0.0.jar && cp ./sca-skeleton-backend/sca-skeleton-system/sca-skeleton-system-biz/target/sca-skeleton-system-biz-1.0.0.jar ./bin/

# 部署 datasource 服务
rm -rf ./bin/sca-skeleton-datasource-biz-1.0.0.jar && cp ./sca-skeleton-backend/sca-skeleton-datasource/sca-skeleton-datasource-biz/target/sca-skeleton-datasource-biz-1.0.0.jar ./bin/

# 启动 gateway 服务
pkill -9 -f "./bin/sca-skeleton-gateway-1.0.0.jar" || true
env $(cat ./bin/.env | grep -v '^#' | xargs) nohup java -jar "./bin/sca-skeleton-gateway-1.0.0.jar" > ./logs/gateway.log 2>&1 &

# 启动 auth 服务
pkill -9 -f "./bin/sca-skeleton-auth-1.0.0.jar" || true
env $(cat ./bin/.env | grep -v '^#' | xargs) nohup java -jar "./bin/sca-skeleton-auth-1.0.0.jar" > ./logs/auth.log 2>&1 &

# 启动 system 服务
pkill -9 -f "./bin/sca-skeleton-system-biz-1.0.0.jar" || true
env $(cat ./bin/.env | grep -v '^#' | xargs) nohup java -jar "./bin/sca-skeleton-system-biz-1.0.0.jar" > ./logs/system.log 2>&1 &

# 启动 datasource 服务
pkill -9 -f "./bin/sca-skeleton-datasource-biz-1.0.0.jar" || true
env $(cat ./bin/.env | grep -v '^#' | xargs) nohup java -jar "./bin/sca-skeleton-datasource-biz-1.0.0.jar" > ./logs/datasource.log 2>&1 &

echo "=========================================="
echo "后端服务部署并启动成功！"
echo "=========================================="
