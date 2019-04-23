FROM frolvlad/alpine-oraclejdk8:slim

COPY configure-agent.sh /usr/local/bin/
COPY wpclient-agent.zip /opt/agent/

RUN chmod a+x /usr/local/bin/configure-agent.sh \
    && chmod -R o+x /opt/agent \
    && cd /opt/agent \
    && unzip wpclient-agent.zip \
    && rm wpclient-agent.zip \
    && apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

ARG PINPOINT_VERSION=${PINPOINT_VERSION:-1.8.0}
ARG INSTALL_URL=https://github.com/naver/pinpoint/releases/download/${PINPOINT_VERSION}/pinpoint-agent-${PINPOINT_VERSION}.tar.gz
#COPY pinpoint-agent-${PINPOINT_VERSION}.tar.gz /opt/agent/pinpoint-agent.tar.gz
RUN mkdir -p /opt/agent/pinpoint-agent \
    #&& curl -SL ${INSTALL_URL} -o /opt/agent/pinpoint-agent.tar.gz \
    && wget -O /opt/agent/pinpoint-agent.tar.gz ${INSTALL_URL} \
    && gunzip /opt/agent/pinpoint-agent.tar.gz \
    && tar -xf /opt/agent/pinpoint-agent.tar -C /opt/agent/pinpoint-agent \
    && rm /opt/agent/pinpoint-agent.tar

ENTRYPOINT ["configure-agent.sh"]
#CMD ["tail", "-f", "/dev/null"]
EXPOSE 8889