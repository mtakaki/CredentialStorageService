web: java $JAVA_OPTS -Ddw.server.applicationConnectors[0].port=$PORT -jar `find target -type f -name 'credential-storage*SNAPSHOT.jar' -print -quit` server src/main/resources/config_heroku.yml
