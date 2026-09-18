FROM docker.elastic.co/logstash/logstash:8.17.0

USER root
RUN rm -f /usr/share/logstash/pipeline/logstash.conf
COPY --chown=logstash:root pipeline/ /usr/share/logstash/pipeline/
COPY --chown=logstash:root config/logstash.yml /usr/share/logstash/config/logstash.yml
USER logstash
