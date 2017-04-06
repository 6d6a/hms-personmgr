FROM docker-registry.intr/user/javabox
ADD ./"${jarBuildName}" app.jar
RUN sh -c 'touch /app.jar'
RUN adduser -u 32156 -D personmgr
USER personmgr
HEALTHCHECK CMD /healthcheck.sh
ENTRYPOINT [ ${entryPoint} ]
