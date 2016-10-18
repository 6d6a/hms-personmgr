FROM docker-registry.intr/user/javabox
ADD ./"${jarBuildName}" app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT [ "java","-Xms192M","-Xmx192M","-Xmn64M","-jar","/app.jar"]
