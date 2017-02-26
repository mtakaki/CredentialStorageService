#!/bin/sh
java -Xmx$MAX_MEMORY -Xms$STARTING_MEMORY -XX:MetaspaceSize=$METASPACE_SIZE -jar `ls *.jar | head -n 1` server $CONFIG_FOLDER/$CONFIG_FILE