FROM docker-registry.intr/user/javabox
ADD ./"${jarBuildName}" app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT [ ${entryPoint} ]
