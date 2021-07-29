FROM apache/nifi:latest
USER root

COPY deployment/nifi/config/core-site.xml /opt/nifi/nifi-current/
COPY deployment/nifi/config/hdfs-site.xml /opt/nifi/nifi-current/

EXPOSE 8091