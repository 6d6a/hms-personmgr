FROM docker-registry.intr/user/javabox
ADD ./"${jarBuildName}" app.jar
RUN sh -c 'touch /app.jar'
RUN adduser -D personmgr
USER personmgr
HEALTHCHECK CMD /healthcheck.sh http://localhost:18070/health
ENTRYPOINT [ ${entryPoint} ]
