#!/bin/bash

set -e

# 进入前端目录
cd /data/projects/sca-skeleton/sca-skeleton-frontend

# 设置系统代理
export http_proxy=http://127.0.0.1:7890 https_proxy=http://127.0.0.1:7890 all_proxy=socks5://127.0.0.1:7890

# 安装前端依赖包
pnpm install

# 进入项目根目录
cd /data/projects/sca-skeleton

# 部署后台 dist
pnpm build:admin
rm -rf /data/docker/containers/nginx/html/admin/* && mv sca-skeleton-frontend/apps/admin/dist/* /data/docker/containers/nginx/html/admin/

# 部署前台 dist
pnpm build:portal
rm -rf /data/docker/containers/nginx/html/portal/* && mv sca-skeleton-frontend/apps/portal/dist/* /data/docker/containers/nginx/html/portal/

# 重启 nginx
docker exec nginx nginx -s reload

# 检查 nginx 配置文件语法
docker exec nginx nginx -t

echo "=========================================="
echo "前端部署成功！"
echo "=========================================="
