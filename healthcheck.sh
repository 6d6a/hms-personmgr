#!/usr/bin/env bash

CURL_OPTS=' -s --connect-timeout 15 --max-time 45'

if [ "$1" == "" ]; then
    HEALTH_URL="http://localhost:18080/actuator/health"
else
    HEALTH_URL="$1"
fi

if [ "$2" == "" ]; then
    JQ_LINE='.details.discoveryComposite.details.eureka.status'
else
    JQ_LINE="$2"
fi

RESULT=`CURLOPT_FAILONERROR=false curl $CURL_OPTS $HEALTH_URL | jq -r $JQ_LINE`

if [ "$RESULT" == "UP" ]; then
    exit 0
fi

exit 1

