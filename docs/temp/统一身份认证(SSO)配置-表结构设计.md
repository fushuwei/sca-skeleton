# 统一身份认证（SSO）配置 —— 表结构设计

> 适用项目：sca-skeleton（高校教育行业数据中台）
> 目标：在"系统管理"中提供通用的 CAS / OAuth2 单点登录配置，避免每所学校改代码。

---

## 一、背景与目标

当前系统以**密码模式**为主（自建 Spring Authorization Server 授权服务器），前端登录页已预留"统一身份认证登录"按钮（占位）。

高校真实场景中，师生通过学校**统一身份认证中心**登录，主流有两种协议：

| 协议 | 典型场景 | 认证方式 |
|------|----------|----------|
| **CAS** | 国内高校最广泛，ticket 校验 | 重定向到学校 CAS 登录页，回传 ticket 后向 CAS Server 校验 |
| **OAuth2 / OIDC** | 较新高校统一认证 | 授权码流程，用 code 换 token，再从 userinfo/id_token 取用户信息 |

**诉求**：把两套协议的连接参数做成"配置数据"而非硬编码，每所学校（= 每租户）在后台自行配置即可启用，系统不改代码即可对接。

---

## 二、关键设计决策

1. **多租户隔离**：所有配置表带 `tenant_id`。每所学校是独立租户（`sys_tenant.domain_name` 绑定域名），各自维护自己的 CAS/OAuth2 配置，互不影响。
2. **主表 + 协议明细表**：CAS 与 OAuth2 的参数结构差异大，拆成"主表（公共配置 + 启用控制 + 用户映射策略）"和"两张协议明细表（1:1）"。字段归属清晰、无大量 NULL、可扩展（未来加 SAML/LDAP 只需新增明细表）。
3. **复用现有令牌体系**：SSO 认证成功后，**不新建 token 机制**，而是复用系统已有的 OAuth2 密码模式 Provider 完成本地 token 签发。前端拿到的始终是本系统统一 token，接入零改动。
4. **用户识别与自动开户**：SSO 回传唯一标识 → 先查 `sys_user_sso` 绑定表命中老用户；未命中则按"用户名映射属性"匹配 `sys_user.username`；仍无则按 `auto_create_user` 决定自动开户（并绑定默认角色/部门）或拒绝。
5. **敏感字段加密**：`client_secret` 采用与数据源一致的 **AES-GCM 加密存储 + cipher_version 密钥版本**，与 `ds_datasource` 完全对齐。
6. **属性映射可配置**：不同学校 SSO 返回的字段名不同（工号/学号/邮箱命名各异），通过"映射属性"配置项把外部属性对接到本地 `sys_user` 字段。

---

## 三、表关系（ER）

```
sys_sso_config  (主表: 通用配置 + 启用 + 映射策略)
   |  1:1
   +--< sys_sso_cas_config     (CAS 参数, protocol_type='cas')
   +--< sys_sso_oauth2_config   (OAuth2 参数, protocol_type='oauth2')

sys_user_sso  (本地用户 <-> 外部身份 绑定)
   user_id -------> sys_user.id
   sso_config_id -> sys_sso_config.id
```


---

## 四、表结构设计

### 4.1 `sys_sso_config` —— 统一身份认证配置主表

每租户每种协议一条记录。承载：通用信息、启用控制、用户映射与自动开户策略。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 主键（UUID） |
| tenant_id | VARCHAR(64) | 租户ID（多租户隔离） |
| name | VARCHAR(100) | 配置名称，如"XX大学统一身份认证" |
| protocol_type | VARCHAR(20) | 协议类型：`cas` / `oauth2` |
| status | VARCHAR(10) | 启用状态：`enabled` / `disabled` |
| is_default | TINYINT(1) | 是否默认登录方式（每租户至多一条为 1） |
| sort | INT | 排序 |
| auto_create_user | TINYINT(1) | 首次 SSO 登录是否自动创建本地用户 |
| default_role_ids | VARCHAR(500) | 自动开户默认角色（逗号分隔） |
| default_dept_id | VARCHAR(64) | 自动开户默认部门 |
| username_attribute | VARCHAR(100) | 用户名映射属性（外部哪个字段作为系统用户名） |
| nickname_attribute | VARCHAR(100) | 昵称映射属性 |
| real_name_attribute | VARCHAR(100) | 姓名映射属性 |
| email_attribute | VARCHAR(100) | 邮箱映射属性 |
| phone_attribute | VARCHAR(100) | 手机号映射属性 |
| avatar_attribute | VARCHAR(100) | 头像映射属性 |
| remark | TEXT | 备注 |
| version | INT | 乐观锁 |
| create_by / create_time / update_by / update_time / is_deleted | — | 公共审计字段 |

索引：`uk_tenant_protocol(tenant_id, protocol_type)` 唯一，保证每租户每协议仅一条配置。

---

### 4.2 `sys_sso_cas_config` —— CAS 协议明细表（1:1）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 主键 |
| sso_config_id | VARCHAR(64) | 关联主表 ID（唯一，1:1） |
| tenant_id | VARCHAR(64) | 租户ID（冗余） |
| server_url_prefix | VARCHAR(255) | CAS 服务端地址前缀，如 `https://cas.xxx.edu.cn/cas` |
| server_login_url | VARCHAR(255) | CAS 登录地址，如 `.../cas/login` |
| server_logout_url | VARCHAR(255) | CAS 登出地址，如 `.../cas/logout` |
| service_url | VARCHAR(255) | 本系统服务（回调）地址，接收 ticket |
| cas_version | VARCHAR(10) | CAS 版本：`2.0` / `3.0` |
| charset | VARCHAR(20) | CAS 服务端字符编码 |

---

### 4.3 `sys_sso_oauth2_config` —— OAuth2/OIDC 协议明细表（1:1）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 主键 |
| sso_config_id | VARCHAR(64) | 关联主表 ID（唯一，1:1） |
| tenant_id | VARCHAR(64) | 租户ID（冗余） |
| client_id | VARCHAR(200) | OAuth2 客户端ID（向学校 IdP 申请） |
| client_secret | VARCHAR(512) | 客户端密钥（AES-GCM 加密存储） |
| cipher_version | VARCHAR(10) | 密钥版本号（轮换解密用） |
| authorization_uri | VARCHAR(255) | 授权端点 |
| token_uri | VARCHAR(255) | 令牌端点 |
| user_info_uri | VARCHAR(255) | 用户信息端点 |
| jwk_set_uri | VARCHAR(255) | JWK 公钥端点（校验 id_token） |
| issuer_uri | VARCHAR(255) | 签发者地址（OIDC Discovery） |
| redirect_uri | VARCHAR(255) | 授权回调地址（需在 IdP 注册） |
| scope | VARCHAR(255) | 授权范围，空格分隔（如 `openid profile email`） |
| user_name_attribute | VARCHAR(100) | 用户名属性（userinfo/id_token 中作为用户名的 claim） |
| client_authentication_method | VARCHAR(50) | 客户端认证方式（basic / post / none） |
| 公共字段 | — | create/update/is_deleted |

---

### 4.4 `sys_user_sso` —— 用户 SSO 绑定表

记录"本地用户 ↔ 外部身份"映射，SSO 回调时据此精确识别老用户。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | VARCHAR(64) | 主键 |
| tenant_id | VARCHAR(64) | 租户ID |
| user_id | VARCHAR(64) | 本地用户ID（sys_user.id） |
| sso_config_id | VARCHAR(64) | 关联 SSO 配置 ID |
| protocol_type | VARCHAR(20) | 协议类型（冗余，便于按协议检索） |
| external_user_id | VARCHAR(200) | 外部身份唯一标识（工号/学号/openid/sub） |
| external_username | VARCHAR(200) | 外部账号名 |
| last_login_time | DATETIME | 最近 SSO 登录时间 |
| 公共字段 | — | create/update/is_deleted |

索引：`uk_protocol_external(sso_config_id, external_user_id)` 唯一，防止同一外部身份重复绑定。


---

## 五、菜单与接口规划（system 服务）

菜单挂在"系统管理"下：**系统管理 → 统一身份认证配置**。

| 接口 | 方法 | 说明 |
|------|------|------|
| `/sso/config/page` | GET | 分页查询 SSO 配置列表 |
| `/sso/config/{id}` | GET | 查询单条（含对应协议明细） |
| `/sso/config` | POST | 新增（按 protocol_type 写入主表 + 明细表） |
| `/sso/config` | PUT | 更新（事务更新主表 + 明细表，处理 secret 加密） |
| `/sso/config/{id}` | DELETE | 删除 |
| `/sso/config/status` | PUT | 启用/禁用 |
| `/sso/config/available` | GET | **登录页用**：按当前域名解析租户，返回已启用的 SSO 列表（脱敏，不含 secret） |

> 登录页"统一身份认证登录"按钮点击后调用 `/sso/config/available`：若仅一条则直接跳转；多条则弹窗让用户选择学校/协议。

---

## 六、SSO 登录集成流程（auth 服务）

登录前用户未认证，需**通过访问域名解析租户**（`sys_tenant.domain_name`），再加载该租户已启用的 SSO 配置。

**CAS 流程：**
1. 前端跳转 → `server_login_url?service=<service_url>`
2. 学校 CAS 认证通过 → 回调 `service_url` 携带 `ticket`
3. 后端用 ticket 请求 CAS Server（`.../serviceValidate`）校验，拿到用户属性
4. 按"映射属性"提取 username 等 → 走用户识别/自动开户
5. 复用密码模式 Provider 签发本系统 token → 重定回前端

**OAuth2 流程：**
1. 前端跳转 → `authorization_uri?client_id&redirect_uri&scope&response_type=code&state`
2. 用户授权 → 回调 `redirect_uri` 携带 `code`
3. 后端用 code 请求 `token_uri` 换 access_token（携带 client_id/secret）
4. 用 token 请求 `user_info_uri`（或解密 id_token）取用户信息
5. 按"映射属性"提取 username 等 → 走用户识别/自动开户
6. 复用密码模式 Provider 签发本系统 token → 重定回前端

**用户识别逻辑（两协议通用）：**
```
externalId = 从 SSO 属性提取唯一标识
1) 查 sys_user_sso(sso_config_id, externalId) 命中 -> 取 user_id 登录
2) 未命中 -> 用 username_attribute 映射值匹配 sys_user.username
   命中 -> 建立 sys_user_sso 绑定 -> 登录
3) 仍无 -> auto_create_user=1 则创建用户(绑定默认角色/部门, source_type='sso') + 建绑定 -> 登录
          否则拒绝登录（提示无权限）
```


---

## 七、与现有代码的衔接点

1. **库归属**：`auth` 服务直连 `sca_platform` 库读写 `sys_user`，故 SSO 配置表放入 `sca_platform.sql`，auth 可直接读取，无需跨库。
2. **`sys_user.source_type`** 已预留 `sso` 值，自动开户时写入 `sso`，天然复用。
3. **token 签发**：新增一个内部 SSO 认证入口，最终调用与密码模式相同的 token 生成链路（`tokenGenerator` + `ScaOpaqueAccessTokenClaimsCustomizer`），保证前端 token 结构一致。
4. **登录日志**：`sys_login_log` 记录 SSO 登录，可扩展 `login_type` 字段区分 cas/oauth2/password。
5. **加密**：`client_secret` 复用数据源模块的 AES-GCM 加密组件。

---

## 八、依赖与安全注意事项

1. **协议依赖**：
   - CAS：引入 `org.apereo.cas:cas-client-support-*`（或自实现 serviceValidate 调用）。
   - OAuth2：引入 `spring-boot-starter-oauth2-client`（项目已有 authorization-server，补 client 依赖即可）。
2. **安全**：
   - `client_secret` 加密存储，接口返回脱敏。
   - OAuth2 必须校验 `state` 防 CSRF；校验 id_token 的 `iss`/`aud`/`exp`。
   - CAS 回调需校验 ticket 一次性（防重放）。
   - `/sso/config/available` 需限流（登录前公开接口）。
3. **回调地址**：`service_url` / `redirect_uri` 需按租户域名动态生成，配置时给出可复制的回调地址提示。
4. **回退策略**：SSO 全部禁用或异常时，登录页仍保留账号密码登录入口作为兜底。

---

## 九、落地产物与并发冲突说明

- 本设计文档：`docs/统一身份认证(SSO)配置-表结构设计.md`
- 规范化 4 表 DDL：`deploy/sql/upgrade/1.3.0/sso_config_normalized.sql`（可直接执行，括号已校验平衡）

> ⚠️ **并发冲突提示**：工作区中另有并行进程生成了**单表 + JSON** 方案
> （`deploy/sql/install/sca_platform_sso.sql`、`deploy/sql/upgrade/1.3.0/1.3.0_sso_config.sql`，
> 以及实体 `SysSsoConfig`、mapper、枚举等代码）。
> 该方案与本规范化方案对 `sys_sso_config` 的定位**互斥**，二者只能取其一上线：
> - 单表+JSON：贴合现有 `connectionParams/poolConfig` JSON 惯例、已有代码，但**缺少 `sys_user_sso` 用户绑定表**，SSO 登录识别链有缺口。
> - 规范化 4 表：参数可查询可索引、`client_secret` 独立加密列、含 `sys_user_sso`，登录闭环完整。
>
> 建议：若采用单表+JSON，至少**补齐 `sys_user_sso` 表**以支撑 SSO 登录时的用户识别与自动开户。

| 公共字段 | — | create/update/is_deleted |
