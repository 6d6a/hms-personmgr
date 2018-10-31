FROM docker-registry.intr/base/javabox:master

ENV XMS 512M
ENV XMX 512M
ENV XMN 192M
ENV DEBUG ""
#ENV DEBUG "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=6006"

COPY ./build/libs /
COPY healthcheck.sh /healthcheck.sh

ONBUILD HEALTHCHECK --interval=10s --timeout=10s --retries=3 CMD /healthcheck.sh

ENTRYPOINT [ "/entrypoint.sh" ]
