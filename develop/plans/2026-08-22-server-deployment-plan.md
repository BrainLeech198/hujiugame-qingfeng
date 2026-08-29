# 服务器部署计划

> **文档定位**：qingfeng 游戏服务器部署方案，包含技术选型、仓库结构、自动部署流程
> 
> **文档结构**：目标 → 技术栈 → 仓库结构 → 部署流程 → 后续步骤
> 
> **更新规范**：每次修改服务器架构或部署流程时更新

## 目标

搭建一个游戏创作社区服务器，支持：
1. 用户登入和注册
2. 存储用户数据
3. 游玩房间搜索和链接功能
4. 发送订阅的邮件功能

## 技术选型

### 服务器
- **云服务商**：阿里云/腾讯云（国内）或 DigitalOcean/Vultr（海外）
- **配置**：2核4G 内存（入门够用）
- **系统**：Ubuntu 22.04 或 CentOS 8
- **管理面板**：宝塔面板（免费，图形化管理）

### 技术栈
- **后端**：Node.js + Express 或 Python + Flask
- **数据库**：MySQL 或 PostgreSQL（宝塔一键安装）
- **用户认证**：JWT（JSON Web Token）
- **邮件服务**：SendGrid（免费 100封/天）或 阿里云邮件推送
- **房间管理**：Redis（存房间状态）+ 数据库（存房间信息）

### 简化方案（快速启动）
- **Vercel/Railway** — 免费额度，不用管服务器
- **Supabase** — 免费数据库 + 用户认证
- **Firebase** — Google 的，有免费额度

## 仓库结构

### 新建仓库
- 客户端：`qingfeng`（现有仓库，官网在 `docs/`）
- 服务器：`qingfeng-server`（新仓库）

### 服务器仓库目录结构
```
qingfeng-server/
├── src/                    # 代码（Git 管理）
│   ├── routes/             # API 接口
│   ├── models/             # 数据模型
│   ├── services/           # 业务逻辑
│   └── utils/              # 工具函数
├── config/                 # 配置文件（Git 管理）
│   ├── default.json        # 默认配置
│   └── production.json     # 生产配置（敏感信息不提交）
├── migrations/             # 数据库结构变更（Git 管理）
│   ├── 001_create_users.sql
│   └── 002_create_rooms.sql
├── scripts/                # 脚本（Git 管理）
│   ├── deploy.sh
│   └── backup.sh
├── .gitignore              # 排除敏感文件
└── README.md
```

### .gitignore 配置
```gitignore
# 敏感配置（不提交）
config/production.json
.env

# 依赖（不提交）
node_modules/

# 日志（不提交）
logs/

# 用户上传文件（不提交）
uploads/

# 数据库文件（不提交）
*.sqlite
```

## 数据管理策略

| 数据类型 | 管理方式 | 说明 |
|---------|---------|------|
| 代码 | Git | 业务逻辑、接口、模型 |
| 数据库结构 | Git（migrations） | 表结构变更用迁移文件 |
| 数据库数据 | 不用 Git | 用户数据、运行数据存在数据库 |
| 配置文件 | Git（敏感信息除外） | 默认配置提交，生产配置用环境变量 |
| 用户上传文件 | 不用 Git | 存服务器本地或对象存储（OSS） |
| 日志 | 不用 Git | 服务器本地存储 |

## 自动部署方案

### 方式一：GitHub Actions（推荐）

**原理**：本地 `git push` → GitHub 通知服务器 → 服务器自动拉取更新

**配置文件**：`.github/workflows/deploy.yml`
```yaml
name: Deploy
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Deploy to server
        uses: appleboy/ssh-action@master
        with:
          host: ${{ secrets.SERVER_IP }}
          username: root
          key: ${{ secrets.SSH_KEY }}
          script: |
            cd /var/www/qingfeng-server
            git pull
            npm install
            pm2 restart app
```

**优点**：
- 实时更新（push 后几秒就生效）
- 不浪费资源（只在有更新时执行）
- 可以加测试、编译等步骤

### 方式二：服务器定时拉取

**原理**：服务器定时检查 GitHub 仓库，有更新就拉取

**配置**：服务器 crontab
```bash
# 每5分钟检查一次
*/5 * * * * cd /var/www/qingfeng-server && git pull
```

**优点**：
- 配置简单
- 不需要 GitHub Actions

**缺点**：
- 有延迟（最多5分钟）
- 服务器要频繁检查

## 部署流程

### 初始部署
1. 租服务器，安装系统
2. 安装宝塔面板
3. 安装 Node.js/Python、MySQL/PostgreSQL
4. 克隆服务器仓库到 `/var/www/qingfeng-server`
5. 配置环境变量（数据库密码、JWT 密钥等）
6. 启动服务（PM2 或 systemd）

### 日常更新
1. 本地开发，测试通过
2. `git push` 到 GitHub
3. GitHub Actions 自动部署（或服务器定时拉取）
4. 服务器自动更新，重启服务

### 手动部署（备用）
```bash
# 登录服务器
ssh root@服务器IP

# 进入项目目录
cd /var/www/qingfeng-server

# 拉取更新
git pull

# 安装依赖
npm install

# 执行数据库迁移
npm run migrate

# 重启服务
pm2 restart app
```

## 后续步骤

1. **选择云服务商**，租服务器
2. **新建 GitHub 仓库** `qingfeng-server`
3. **初始化项目**，搭建基础框架
4. **实现用户系统**（注册、登录、JWT 认证）
5. **实现房间系统**（创建、搜索、加入）
6. **实现邮件订阅**（SendGrid 集成）
7. **配置自动部署**（GitHub Actions）
8. **测试和优化**

## 参考资源

- [宝塔面板官网](https://www.bt.cn/)
- [SendGrid 文档](https://docs.sendgrid.com/)
- [GitHub Actions 文档](https://docs.github.com/cn/actions)
- [PM2 文档](https://pm2.keymetrics.io/)
