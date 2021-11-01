FROM yti-docker-java11-base:alpine

ADD build/libs/yti-terminology-api.jar yti-terminology-api.jar

ENTRYPOINT ["/bootstrap.sh", "yti-terminology-api.jar", "-j", "-Djava.security.egd=file:/dev/./urandom"]
