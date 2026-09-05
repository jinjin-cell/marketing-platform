# 基础镜像
FROM openjdk:8-jre-slim

# 作者
MAINTAINER xiaofuge

# 配置
ENV PARAMS=""

# 时区
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 添加应用
ADD target/marketing-platform-app.jar /marketing-platform-app.jar
EXPOSE 8091 20880

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /marketing-platform-app.jar $PARAMS"]
