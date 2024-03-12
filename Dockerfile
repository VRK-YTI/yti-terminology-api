FROM yti-docker-java-base:corretto-17.0.10

ADD build/libs/yti-terminology-api.jar yti-terminology-api.jar

ENTRYPOINT ["/bootstrap.sh", "yti-terminology-api.jar", "-j", "-Djava.security.egd=file:/dev/./urandom"]
