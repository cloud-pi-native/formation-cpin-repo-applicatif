FROM gcr.io/distroless/java21:nonroot
WORKDIR /app
COPY target/*.jar /app/app.jar

CMD ["-jar", "/app/app.jar"]
EXPOSE 8080
