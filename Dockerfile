FROM java:openjdk-8-jre-alpine
MAINTAINER Mitsuo Takaki <mitsuotakaki@gmail.com>

ENV CREDENTIAL_STORAGE_FOLDER=/opt/credential-storage
ENV CONFIG_FOLDER=$CREDENTIAL_STORAGE_FOLDER/config
ENV CONFIG_FILE=config.yml
ENV LOG_LEVEL=INFO
ENV TIME_ZONE=UTC

ADD target/bundled-credential-storage-*.tar.gz $CREDENTIAL_STORAGE_FOLDER
# Moving the config.yml to the config folder, so we can mount later.
RUN mkdir -p $CONFIG_FOLDER \
  && mv $CREDENTIAL_STORAGE_FOLDER/*.yml $CONFIG_FOLDER
COPY start.sh $CREDENTIAL_STORAGE_FOLDER

EXPOSE 8080
EXPOSE 8081

VOLUME $CREDENTIAL_STORAGE_FOLDER
WORKDIR /opt/credential-storage/

CMD ["/opt/credential-storage/start.sh"]
