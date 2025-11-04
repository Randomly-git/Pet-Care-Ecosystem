# 宠物养护生态系统 🐾

基于微服务架构的全栈宠物健康管理平台，提供宠物医疗、社交互动、电商服务等一站式解决方案。

## 🚀 功能特性

### 已实现功能
- 🏠 **宠物空间** - 个性化的宠物主页和动态分享
- 📝 **动态发布** - 发布宠物日常，支持图片上传
- 👥 **多宠物管理** - 支持切换不同宠物账号
- 💬 **社交互动** - 点赞、评论、关注功能

### 即将实现
- 🏥 宠物医疗服务预约
- 🛍️ 宠物电商平台  
- 📊 健康数据追踪
- 👥 宠物社群交流

## 🛠️ 技术栈

### 后端
- **框架**: Flask + SQLAlchemy
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **API风格**: RESTful

### 前端  
- **框架**: Vue 3 + Element Plus
- **构建工具**: Vite
- **路由**: Vue Router
- **状态管理**: Pinia

### 部署
- **容器化**: Docker + Docker Compose
- **云服务**: 阿里云/腾讯云
- **CDN**: 对象存储 + 内容分发

## 📦 快速开始

### 环境要求
- Python 3.8+
- Node.js 16+
- MySQL 8.0+
- Redis

### 后端启动
```bash
cd backend/pet-space-service

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件配置数据库连接

# 启动服务
python run.py

### 前端启动
cd frontend/pet-care-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev

### Docke 启动（推荐）
cd backend
docker-compose up -d

### 项目结构
pet-care-ecosystem/
├── backend/                 # 后端服务
│   ├── pet-space-service/  # 宠物空间微服务
│   └── docker-compose.yml  # 基础设施配置
├── frontend/               # 前端应用
│   └── pet-care-frontend/ # Vue3前端项目
├── docs/                   # 项目文档
└── README.md              # 项目说明

### 配置说明

# 数据库配置
在 backend/pet-space-service/.env 中配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=pet_space_db
DB_USER=root
DB_PASSWORD=your_password

### API接口
基础URL: http://localhost:5001/api/v1/pet-space
文档: 待补充 Swagger 文档