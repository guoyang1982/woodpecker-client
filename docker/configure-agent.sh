#!/bin/sh
set -e
set -x

sed -i "/application.name=/ s/=.*/=${APP_NAME}/" /opt/agent/wpclient-agent/woodpecker.properties
sed -i "/redis.cluster.host=/ s/=.*/=${REDIS_CLUSTER_HOST}/" /opt/agent/wpclient-agent/woodpecker.properties
sed -i "/redis.cluster.password=/ s/=.*/=${REDIS_CLUSTER_PASSWORD}/" /opt/agent/wpclient-agent/woodpecker.properties
sed -i "/log.netty.server=/ s/=.*/=${IS_NETTY_SERVER}/" /opt/agent/wpclient-agent/woodpecker.properties

sed -i "/profiler.collector.ip=/ s/=.*/=${COLLECTOR_IP}/" /opt/agent/pinpoint-agent/pinpoint.config
sed -i "/profiler.collector.tcp.port=/ s/=.*/=${COLLECTOR_TCP_PORT}/" /opt/agent/pinpoint-agent/pinpoint.config
sed -i "/profiler.collector.stat.port=/ s/=.*/=${COLLECTOR_STAT_PORT}/" /opt/agent/pinpoint-agent/pinpoint.config
sed -i "/profiler.collector.span.port=/ s/=.*/=${COLLECTOR_SPAN_PORT}/" /opt/agent/pinpoint-agent/pinpoint.config
sed -i "/profiler.sampling.rate=/ s/=.*/=${PROFILER_SAMPLING_RATE}/" /opt/agent/pinpoint-agent/pinpoint.config

sed -i "/level value=/ s/=.*/=\"${DEBUG_LEVEL}\"\/>/g" /opt/agent/pinpoint-agent/lib/log4j.xml

exec "$@"