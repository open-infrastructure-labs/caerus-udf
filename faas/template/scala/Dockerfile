# Pull base image from https://github.com/hseeberger/scala-sbt
# FROM hseeberger/scala-sbt
FROM hseeberger/scala-sbt:8u222_1.3.5_2.13.1

RUN echo "Pulling watchdog binary from Github." \
    && curl -sSL https://github.com/openfaas/faas/releases/download/0.8.9/fwatchdog > /usr/bin/fwatchdog \
    && chmod +x /usr/bin/fwatchdog \
    && cp /usr/bin/fwatchdog /home/app

# Function
COPY function function/
WORKDIR /root/function/

RUN echo "Copied Sources" && ls

RUN echo "Building with SBT"
RUN sbt clean assembly

ENV JAR_FILE=target/scala-2.12/handler.jar
RUN mv ${JAR_FILE} .

# Populate example here - i.e. "cat", "sha512sum" or "node index.js"
ENV fprocess="java -jar handler.jar"

# Set to true to see request in function logs
ENV write_debug="true"

HEALTHCHECK --interval=5s CMD [ -e /tmp/.lock ] || exit 1
CMD [ "fwatchdog" ]
