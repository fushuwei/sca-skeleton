# Cloudflare Tunnel (cloudflared) 安装与配置指南

本文档提供了在 Ubuntu Server 上部署 Cloudflare Tunnel 的标准化流程，用于将本地服务（如 `192.168.1.105:8080`）安全地暴露至公网域名 `newease.cloud`。

---

## 1. 安装 cloudflared

下载并安装最新的 Debian 软件包：

```bash
# 下载最新版
wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64.deb

# 执行安装
sudo dpkg -i cloudflared-linux-amd64.deb

# 验证版本
cloudflared --version
```

## 2. 身份验证与隧道创建

### 2.1 登录 Cloudflare
执行以下命令并访问生成的 URL 进行授权：

```bash
cloudflared tunnel login
```
*授权完成后，系统会自动生成证书文件：`/root/.cloudflared/cert.pem`。*

### 2.2 创建隧道
创建一个名为 `sca-skeleton` 的隧道：

```bash
cloudflared tunnel create sca-skeleton
```
**注意**：记下返回的 **Tunnel ID (UUID)**，后续配置需使用。

---

## 3. 编写配置文件

创建或编辑 `/root/.cloudflared/config.yml`，内容如下：

```yaml
tunnel: <YOUR_TUNNEL_UUID>
credentials-file: /root/.cloudflared/<YOUR_TUNNEL_UUID>.json
loglevel: warn

ingress:
  # 1. 主域名映射至本地 Nginx 或服务端口
  - hostname: newease.cloud
    service: http://localhost:8080

  # 2. SSH 远程访问（可选）
  - hostname: ssh.newease.cloud
    service: ssh://localhost:22

  # 3. 兜底规则（必须保留）
  - service: http_status:404
```
> **替换说明**：将 `<YOUR_TUNNEL_UUID>` 替换为 2.2 步骤中获取的实际 UUID。

---

## 4. 配置 DNS 路由

将域名指向创建的隧道：

```bash
# 映射主域名
cloudflared tunnel route dns sca-skeleton newease.cloud

# 映射 SSH 域名（如配置了 SSH）
cloudflared tunnel route dns sca-skeleton ssh.newease.cloud
```

---

## 5. 部署为系统服务

将 `cloudflared` 配置为开机自启服务：

```bash
# 安装服务（会自动将配置拷贝至 /etc/cloudflared/）
sudo cloudflared service install

# 启动并启用服务
sudo systemctl enable --now cloudflared

# 检查运行状态
sudo systemctl status cloudflared
```

---

## 6. 常用维护命令

| 操作 | 命令 |
| :--- | :--- |
| **查看日志** | `sudo journalctl -u cloudflared -f` |
| **重启服务** | `sudo systemctl restart cloudflared` |
| **更新软件** | 重复步骤 1 重新安装即可 |
| **查看隧道列表** | `cloudflared tunnel list` |

---

## 7. 客户端 SSH 连接 (macOS/Linux)

若配置了 SSH 转发，在客户端编辑 `~/.ssh/config`：

```text
Host my-server
    HostName ssh.newease.cloud
    ProxyCommand /usr/local/bin/cloudflared access ssh --hostname %h
    User root
```
配置后即可直接通过 `ssh my-server` 连接。
