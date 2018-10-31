#!/bin/sh

if [ "$1" == "" ]; then
    HEALTH_URL="http://localhost:18080/health"
else
    HEALTH_URL="$1"
fi

if [ "$2" == "" ]; then
    JQ_LINE='.discoveryComposite.eureka.status'
else
    JQ_LINE="$2"
fi

RESULT=`CURLOPT_FAILONERROR=false curl $HEALTH_URL | jq -r $JQ_LINE`

if [ "$RESULT" == "UP" ]; then
    exit 0
fi

exit 1

