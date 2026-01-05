# 1. 使用 slim 版本镜像（体积比标准版小 200MB+，且去除了不必要的工具，更安全）
FROM openjdk:21-jdk-slim


# 2. 设置容器内的时间，防止日志时间对不上
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

WORKDIR /app

# 3. 复制 JAR 包
# 提示：确保执行 docker build 前，target 目录下只有一个 jar 包
COPY target/*.jar app.jar

# 4. 优化 Java 启动参数（防止内存溢出，并在容器环境下限制内存）
ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-XX:+UseG1GC", "-jar", "app.jar"]