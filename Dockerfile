FROM adoptopenjdk/openjdk11:alpine-slim
RUN apk update && \
      apk add sqlite && \
      rm -rf /var/cache/apk/*
COPY target/diabetes-dashboard-server.jar /app/app.jar
RUN chown -R 1001:1001 /app
WORKDIR /app
USER 1001
EXPOSE 3000
CMD ["java", "-jar", "app.jar"]

