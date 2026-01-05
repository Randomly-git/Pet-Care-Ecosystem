FROM nginx:stable-alpine

# 1. 复制静态文件到 nginx 默认托管目录
COPY dist/ /usr/share/nginx/html/

# 2. 复制 nginx 配置（这个配置处理了 Vue 的 404 刷新问题和 API 转发）
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]