# Dockerfile for demo-helloworld
FROM 192.168.233.9:80/base/openjdk:8-jre-alpine

# 设置工作目录
# WORKDIR /app

# 复制 JAR 文件
COPY target/demo-helloworld.jar app.jar

# 暴露端口
EXPOSE 8080

# 设置启动命令
ENTRYPOINT ["java", "-jar", "app.jar"]