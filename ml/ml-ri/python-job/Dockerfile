FROM --platform=linux/amd64 amd64/ubuntu:24.10

# Add and confirm Java installed
RUN apt upgrade
RUN apt update
RUN apt-get install -y wget
# Add Java
RUN apt update
RUN cd /tmp ; \
    wget -q https://download.java.net/java/GA/jdk21/fd2272bbf8e04c3dbaee13770090416c/35/GPL/openjdk-21_linux-x64_bin.tar.gz ; \
    tar xf openjdk-21_linux-x64_bin.tar.gz ;
RUN update-alternatives --install /usr/bin/java java /tmp/jdk-21/bin/java 1

# Maven compiled target
ARG JAR_FILE
COPY target/${JAR_FILE} application.jar

# Default true
ENV MY_KUBERNETES_ENABLED="true"

ENV JAVA_ARGS=""
ENV JAVA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec java $JAVA_ARGS $JAVA_OPTS -Dmy.kubernetes.enabled=$MY_KUBERNETES_ENABLED -jar application.jar $0
