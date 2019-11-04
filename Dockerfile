FROM navikt/java:11-appdynamics
ENV APPD_ENABLED=true
COPY build/libs/syfooversikthendelsetilfelle-*-all.jar app.jar
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml"
ENV APPLICATION_PROFILE="remote"
