#!/bin/sh
java -jar `ls *.jar | head -n 1` server $CONFIG_FOLDER/$CONFIG_FILE
