FROM yti-docker-java-base:corretto-11.0.22

ADD build/libs/yti-terminology-api.jar yti-terminology-api.jar

ENTRYPOINT ["/bootstrap.sh", "yti-terminology-api.jar", "-j", "-Djava.security.egd=file:/dev/./urandom"]
