FROM picoded/ubuntu-openjdk-8-jdk as build

# get git repo
RUN apt-get update
RUN apt-get install -y git

RUN mkdir /home/jars
WORKDIR /home/jars
COPY  eventListenerService/target/*-0.0.1-SNAPSHOT.jar /home/jars/
COPY  udfService/target/*-0.0.1-SNAPSHOT.jar /home/jars/
COPY  ndpService/target/*-0.0.1-SNAPSHOT.jar /home/jars/
COPY  registry/target/*-0.0.1-SNAPSHOT.jar /home/jars/


# Installing supervisor service and creating directories for copy supervisor configurations
RUN apt-get -y install supervisor && mkdir -p /var/log/supervisor && mkdir -p /etc/supervisor/conf.d
ADD deployment/supervisor.conf /etc/supervisor.conf

# Ports
# 8000 - Ndp Service
# 8002 - UDF Service
# 8003 - Event Listener Service
# 8004 - Registry Service

EXPOSE 8000
EXPOSE 8001
EXPOSE 8002
EXPOSE 8003
EXPOSE 8004
# Get ready to run command
#ENTRYPOINT ["./bin/onos-service"]
CMD ["supervisord", "-c", "/etc/supervisor.conf"]