FROM openjdk:8-jre-slim

ENV XMS 512M
ENV XMX 512M
ENV XMN 192M
ENV TZ Europe/Moscow

ADD http://archive.intr/Majordomo_LLC_Root_CA.crt /tmp/root.crt
RUN keytool -trustcacerts -keystore /usr/local/openjdk-8/lib/security/cacerts -storepass changeit -alias Root -import -file /tmp/root.crt -noprompt
RUN echo 'monitorRole password' >> /usr/local/openjdk-8/lib/management/jmxremote.password && echo 'controlRole password' >> /usr/local/openjdk-8/lib/management/jmxremote.password && \
chmod 600 /usr/local/openjdk-8/lib/management/jmxremote.password 

COPY ./build/libs /
COPY healthcheck.sh /healthcheck.sh

ONBUILD HEALTHCHECK --interval=10s --timeout=10s --retries=3 CMD /healthcheck.sh

ENTRYPOINT [ "/bin/bash", "-c" ,"exec java -Xms${XMS} -Xmx${XMX} -Xmn${XMN} ${DEBUG} -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -jar /*.jar" ]
