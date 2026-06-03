FROM java:8

RUN mkdir app
WORKDIR app
ENV LANG: C.UTF-8
ENV LC_ALL: C.UTF-8
ENV JAVA_OPTS: "-Dfile.encoding=UTF-8"
COPY monitor-server-0.0.1-SNAPSHOT.jar app/
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app/monitor-server-0.0.1-SNAPSHOT.jar"]
EXPOSE 10020
