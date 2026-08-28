#!/usr/bin/env bash
# ============================================================
# 医院个人科研成果诚信综合评价系统 · 一键部署脚本
# 适用：Linux（Ubuntu / CentOS）· 低并发单机部署
#
# 用法：
#   bash scripts/deploy.sh                              # 全量部署（交互式输入 MySQL 密码）
#   DB_PASSWORD=密码 bash scripts/deploy.sh             # 全量部署（root 直接运行）
#   sudo DB_PASSWORD=密码 bash scripts/deploy.sh        # 全量部署（sudo 时变量须放在 sudo 之后）
#   bash scripts/deploy.sh --db-only                    # 仅初始化数据库
#   bash scripts/deploy.sh --backend-only               # 仅构建并启动后端
#   bash scripts/deploy.sh --frontend-only              # 仅构建前端并配置 Nginx
#   bash scripts/deploy.sh --skip-db                    # 跳过数据库步骤
#
# 前置：JDK 17、Maven 3.6+、MySQL 8.0、Node 18+、Nginx、Git
# 配置可用环境变量覆盖（见下方 CONFIG 段），如：
#   DB_PASSWORD=xxx JWT_SECRET=xxx bash scripts/deploy.sh
# ============================================================
set -euo pipefail

# ---------------- CONFIG（环境变量可覆盖） ----------------
APP_DIR="${APP_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-integrity_db}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
JWT_SECRET="${JWT_SECRET:-$(openssl rand -hex 24 2>/dev/null || echo change-me-to-a-long-random-secret)}"
FILE_PATH="${FILE_PATH:-/data/integrity/files}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
WEB_PORT="${WEB_PORT:-80}"
NGINX_CONF="${NGINX_CONF:-/etc/nginx/conf.d/integrity.conf}"
NPM_REGISTRY="${NPM_REGISTRY:-https://registry.npmmirror.com}"
RUN_USER="${RUN_USER:-$(id -un)}"

# 未通过环境变量提供数据库密码时，交互式输入（注意：sudo 会剥离环境变量，
# 请使用 "DB_PASSWORD=xxx bash deploy.sh"（root 直接跑）或 "sudo DB_PASSWORD=xxx bash deploy.sh"）
if [ -z "$DB_PASSWORD" ]; then
  read -r -s -p "请输入 MySQL 密码（${DB_USER}@${DB_HOST}:${DB_PORT}）: " DB_PASSWORD
  echo ""
fi

# ---------------- 参数解析 ----------------
DB_ONLY=0; BACKEND_ONLY=0; FRONTEND_ONLY=0; SKIP_DB=0
for arg in "$@"; do
  case "$arg" in
    --db-only) DB_ONLY=1 ;;
    --backend-only) BACKEND_ONLY=1 ;;
    --frontend-only) FRONTEND_ONLY=1 ;;
    --skip-db) SKIP_DB=1 ;;
    *) echo "未知参数: $arg"; exit 1 ;;
  esac
done

# 执行段开关：默认全量；指定单一模式则只执行对应段；--skip-db 可叠加
DO_DB=0; DO_BACKEND=0; DO_FRONTEND=0
if [ "$DB_ONLY" = 1 ]; then
  DO_DB=1
elif [ "$BACKEND_ONLY" = 1 ]; then
  DO_BACKEND=1
elif [ "$FRONTEND_ONLY" = 1 ]; then
  DO_FRONTEND=1
else
  DO_DB=1; DO_BACKEND=1; DO_FRONTEND=1
fi
[ "$SKIP_DB" = 1 ] && DO_DB=0

log()  { echo -e "\033[1;34m[$1]\033[0m $2"; }
ok()   { echo -e "\033[1;32m[OK]\033[0m $1"; }
warn() { echo -e "\033[1;33m[WARN]\033[0m $1"; }
die()  { echo -e "\033[1;31m[ERROR]\033[0m $1" >&2; exit 1; }

require() { command -v "$1" >/dev/null 2>&1 || die "缺少依赖: $1，请先安装（见 docs/部署运维手册.md 第 1 节）"; }

cd "$APP_DIR" || die "无法进入项目目录: $APP_DIR"
log "项目目录" "$APP_DIR"

# ---------------- 1. 前置检查 ----------------
log "前置检查" "JDK/Maven/MySQL/Node/Nginx"
require java; require mvn; require mysql; require node; require npm
command -v nginx >/dev/null 2>&1 || warn "未检测到 nginx（仅前端步骤需要，可稍后安装）"

JAVA_MAJOR=$(java -version 2>&1 | sed -n '1p' | sed -E 's/.*version "([0-9]+).*/\1/')
[ "${JAVA_MAJOR:-0}" -ge 17 ] || warn "JDK 版本为 $JAVA_MAJOR（需要 17+）；若为 JDK 8 请改用 Spring Boot 2.7 分支"
NODE_MAJOR=$(node -v 2>/dev/null | sed -E 's/v([0-9]+).*/\1/')
[ "${NODE_MAJOR:-0}" -ge 18 ] || warn "Node 版本 $NODE_MAJOR（建议 18+）"

# ---------------- 2. 数据库初始化 ----------------
if [ "$DO_DB" = 1 ]; then
  log "数据库初始化" "${DB_HOST}:${DB_PORT}/${DB_NAME}"
  MYSQL_ARGS=(-h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER")
  [ -n "$DB_PASSWORD" ] && MYSQL_ARGS+=(-p"$DB_PASSWORD")
  mysql "${MYSQL_ARGS[@]}" -e "SELECT 1" >/dev/null 2>&1 || die "无法连接 MySQL（${DB_HOST}:${DB_PORT} 用户 ${DB_USER}），请检查账号密码"

  mysql "${MYSQL_ARGS[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
  mysql "${MYSQL_ARGS[@]}" "$DB_NAME" < sql/schema.sql
  TABLE_CNT=$(mysql "${MYSQL_ARGS[@]}" -N -s -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB_NAME';")
  DICT_CNT=$(mysql "${MYSQL_ARGS[@]}" -N -s -e "SELECT COUNT(*) FROM $DB_NAME.sys_dict_type;" 2>/dev/null || echo 0)
  if [ "${DICT_CNT:-0}" -eq 0 ]; then
    mysql "${MYSQL_ARGS[@]}" "$DB_NAME" < sql/init_dict.sql
    mysql "${MYSQL_ARGS[@]}" "$DB_NAME" < sql/init_rules.sql
    ok "字典与评分规则已初始化"
  else
    warn "字典数据已存在（${DICT_CNT} 条），跳过 init_dict/init_rules（避免重复插入）"
  fi
  ok "数据库就绪（${TABLE_CNT} 张表）；admin/角色/菜单由后端首次启动自动创建"
fi

# ---------------- 3. 后端构建与启动 ----------------
if [ "$DO_BACKEND" = 1 ]; then
  log "后端构建" "mvn clean package -DskipTests（首次需下载依赖，请耐心等待）"
  (cd backend && mvn clean package -DskipTests) || die "后端构建失败，请查看 Maven 输出"
  [ -f backend/target/integrity-system.jar ] || die "未找到构建产物 backend/target/integrity-system.jar"
  ok "后端构建完成: backend/target/integrity-system.jar"

  mkdir -p "$FILE_PATH"
  if [ "$(id -u)" = 0 ] && command -v systemctl >/dev/null 2>&1; then
    log "后端启动" "注册 systemd 服务 integrity.service"
    cat > /etc/systemd/system/integrity.service <<EOF
[Unit]
Description=Integrity Evaluation System
After=network.target mysql.service

[Service]
Type=simple
User=$RUN_USER
WorkingDirectory=$APP_DIR/backend
ExecStart=/usr/bin/java -jar $APP_DIR/backend/target/integrity-system.jar
Environment=DB_PASSWORD=$DB_PASSWORD
Environment=JWT_SECRET=$JWT_SECRET
Environment=FILE_PATH=$FILE_PATH
Environment=SERVER_PORT=$BACKEND_PORT
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
    systemctl daemon-reload
    systemctl enable integrity >/dev/null 2>&1 || true
    systemctl restart integrity
    sleep 8
    if systemctl is-active integrity >/dev/null 2>&1; then
      ok "后端已启动（systemd: integrity，端口 ${BACKEND_PORT}）"
    else
      warn "后端未正常运行，请查看: journalctl -u integrity -n 50"
    fi
  else
    log "后端启动" "非 root / 无 systemd，使用 nohup 方式"
    pkill -f integrity-system.jar 2>/dev/null || true
    env DB_PASSWORD="$DB_PASSWORD" JWT_SECRET="$JWT_SECRET" FILE_PATH="$FILE_PATH" \
      SERVER_PORT="$BACKEND_PORT" \
      nohup java -jar "$APP_DIR/backend/target/integrity-system.jar" \
      > "$APP_DIR/backend/run.log" 2>&1 &
    echo $! > "$APP_DIR/backend/run.pid"
    ok "后端已后台启动（PID $(cat "$APP_DIR/backend/run.pid")，日志 backend/run.log）"
    warn "生产环境建议以 root 运行本脚本以注册 systemd 服务"
  fi
fi

# ---------------- 4. 前端构建与 Nginx ----------------
if [ "$DO_FRONTEND" = 1 ]; then
  log "前端构建" "npm install && npm run build"
  (cd frontend && npm install --registry="$NPM_REGISTRY" --no-audit --no-fund && npm run build) \
    || die "前端构建失败，请查看 npm 输出"
  ok "前端构建完成: frontend/dist"

  if [ "$(id -u)" = 0 ]; then
    log "Nginx 配置" "$NGINX_CONF"
    cat > "$NGINX_CONF" <<EOF
server {
    listen ${WEB_PORT};
    server_name _;

    root $APP_DIR/frontend/dist;
    index index.html;

    location /api/ {
        proxy_pass http://127.0.0.1:$BACKEND_PORT;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        client_max_body_size 60m;
    }

    location /api/file/download {
        proxy_pass http://127.0.0.1:$BACKEND_PORT;
        proxy_buffering off;
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    # 入口页禁止缓存，避免发布后浏览器仍使用旧包
    location = /index.html {
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    location /assets/ {
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
EOF
    if nginx -t && systemctl reload nginx; then
      ok "Nginx 已配置并重载"
    else
      warn "nginx -t 失败，请检查 $NGINX_CONF"
    fi
  else
    warn "非 root，跳过 Nginx 配置；请参照 docs/部署运维手册.md 第 5 节手工配置"
  fi
fi

# ---------------- 5. 汇总 ----------------
echo ""
echo "══════════════════════════════════════════════════════════"
echo "  部署完成"
echo "  访问地址 : http://<服务器IP>:${WEB_PORT}/   （WEB_PORT 默认 80，可自定义避免端口冲突）"
echo "  管理员   : admin / admin123（首次登录后请修改密码）"
echo "  附件目录 : $FILE_PATH"
echo "  JWT密钥  : $JWT_SECRET  （请妥善保存；如需更换，修改环境变量后重启）"
echo "  参考文档 : docs/部署运维手册.md"
echo "══════════════════════════════════════════════════════════"
