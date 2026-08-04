#!/bin/bash
# ============================================================================
# SCA Skeleton 一键部署脚本（基于 deploy/app/安装步骤.md）
#
# 用法：
#   ./deploy.sh [阶段]
#
# 阶段（不传则依次执行全部）：
#   build-backend    构建 JAR 包并部署到 ./bin/
#   start-backend    启动 gateway / auth / system / datasource 四个后端服务
#   build-frontend   构建前端 admin / portal dist
#   deploy-frontend  部署前端 dist 到 nginx 容器目录并 reload
#   all              上述全部（默认）
#
# 注意：
#   - 脚本需在项目根目录 /data/projects/sca-skeleton 下执行（或通过 cd 切入）
#   - .env 由 deploy/app/bin/.env 提供，通过 set -a / source 加载
#   - 前端 dist 部署目录：/data/docker/containers/nginx/html/{admin,portal}
#   - nginx 容器名：nginx
# ============================================================================
set -euo pipefail

# ---------------- 路径与常量 ----------------
PROJECT_ROOT="/data/projects/sca-skeleton"
BACKEND_DIR="${PROJECT_ROOT}/sca-skeleton-backend"
FRONTEND_DIR="${PROJECT_ROOT}/sca-skeleton-frontend"
BIN_DIR="${PROJECT_ROOT}/bin"
LOG_DIR="${PROJECT_ROOT}/logs"
ENV_FILE="${PROJECT_ROOT}/bin/.env"
NGINX_HTML="/data/docker/containers/nginx/html"

GATEWAY_JAR="sca-skeleton-gateway-1.0.0.jar"
AUTH_JAR="sca-skeleton-auth-1.0.0.jar"
SYSTEM_JAR="sca-skeleton-system-biz-1.0.0.jar"
DATASOURCE_JAR="sca-skeleton-datasource-biz-1.0.0.jar"

# ---------------- 工具函数 ----------------
log()  { echo -e "\033[32m[deploy]\033[0m $*"; }
warn() { echo -e "\033[33m[warn]\033[0m $*" >&2; }
err()  { echo -e "\033[31m[error]\033[0m $*" >&2; }

ensure_dirs() {
  mkdir -p "${BIN_DIR}" "${LOG_DIR}"
}

# 加载 .env（与 auth.sh/gateway.sh/system.sh 风格一致：set -a / source / set +a）
load_env() {
  if [[ ! -f "${ENV_FILE}" ]]; then
    err "未找到环境变量文件：${ENV_FILE}"
    exit 1
  fi
  set -a
  # shellcheck disable=SC1090
  source "${ENV_FILE}"
  set +a
}

# ---------------- 阶段 1：构建并部署后端 JAR ----------------
build_backend() {
  log "构建后端 JAR 包"
  cd "${BACKEND_DIR}"
  mvn clean package -DskipTests

  log "部署 JAR 到 ${BIN_DIR}/"
  cd "${BACKEND_DIR}"
  rm -rf "${BIN_DIR}/${GATEWAY_JAR}"
  cp ./sca-skeleton-gateway/target/${GATEWAY_JAR} "${BIN_DIR}/"
  rm -rf "${BIN_DIR}/${AUTH_JAR}"
  cp ./sca-skeleton-auth/target/${AUTH_JAR} "${BIN_DIR}/"
  rm -rf "${BIN_DIR}/${SYSTEM_JAR}"
  cp ./sca-skeleton-system/sca-skeleton-system-biz/target/${SYSTEM_JAR} "${BIN_DIR}/"
  rm -rf "${BIN_DIR}/${DATASOURCE_JAR}"
  cp ./sca-skeleton-datasource/sca-skeleton-datasource-biz/target/${DATASOURCE_JAR} "${BIN_DIR}/"
}

# ---------------- 阶段 2：启动后端服务 ----------------
start_backend() {
  load_env
  cd "${PROJECT_ROOT}"

  log "启动 gateway 服务"
  pkill -9 -f "./bin/${GATEWAY_JAR}" 2>/dev/null || true
  sleep 1
  nohup java -jar "./bin/${GATEWAY_JAR}" > ./logs/gateway.log 2>&1 &

  log "启动 auth 服务"
  pkill -9 -f "./bin/${AUTH_JAR}" 2>/dev/null || true
  sleep 1
  nohup java -jar "./bin/${AUTH_JAR}" > ./logs/auth.log 2>&1 &

  log "启动 system 服务"
  pkill -9 -f "./bin/${SYSTEM_JAR}" 2>/dev/null || true
  sleep 1
  nohup java -jar "./bin/${SYSTEM_JAR}" > ./logs/system.log 2>&1 &

  log "启动 datasource 服务"
  pkill -9 -f "./bin/${DATASOURCE_JAR}" 2>/dev/null || true
  sleep 1
  nohup java -jar "./bin/${DATASOURCE_JAR}" > ./logs/datasource.log 2>&1 &

  log "后端服务已启动，日志：${LOG_DIR}/{gateway,auth,system,datasource}.log"
}

# ---------------- 阶段 3：构建前端 ----------------
build_frontend() {
  log "安装前端依赖"
  cd "${FRONTEND_DIR}"
  pnpm install --frozen-lockfile

  log "构建前端 admin / portal dist"
  pnpm build:admin
  pnpm build:portal
}

# ---------------- 阶段 4：部署前端到 nginx 并 reload ----------------
deploy_frontend() {
  log "部署 admin dist → ${NGINX_HTML}/admin/"
  rm -rf "${NGINX_HTML}/admin/"*
  mv "${FRONTEND_DIR}/apps/admin/dist/"* "${NGINX_HTML}/admin/"

  log "部署 portal dist → ${NGINX_HTML}/portal/"
  rm -rf "${NGINX_HTML}/portal/"*
  mv "${FRONTEND_DIR}/apps/portal/dist/"* "${NGINX_HTML}/portal/"

  log "检查 nginx 配置语法"
  docker exec nginx nginx -t

  log "reload nginx"
  docker exec nginx nginx -s reload
}

# ---------------- 主流程 ----------------
main() {
  local stage="${1:-all}"
  ensure_dirs

  case "${stage}" in
    build-backend)
      build_backend
      ;;
    start-backend)
      start_backend
      ;;
    build-frontend)
      build_frontend
      ;;
    deploy-frontend)
      deploy_frontend
      ;;
    all)
      build_backend
      start_backend
      build_frontend
      deploy_frontend
      ;;
    *)
      err "未知阶段：${stage}"
      echo "用法：$0 {build-backend|start-backend|build-frontend|deploy-frontend|all}"
      exit 1
      ;;
  esac

  log "阶段 [${stage}] 完成"
}

main "$@"
