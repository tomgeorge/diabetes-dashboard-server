FROM adoptopenjdk/openjdk11:alpine-slim
RUN apk update && \
      apk add sqlite && \
      rm -rf /var/cache/apk/*
COPY target/diabetes-dashboard-server-0.1.0-SNAPSHOT-standalone.jar /app/app.jar
WORKDIR /app
USER 1001
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]

