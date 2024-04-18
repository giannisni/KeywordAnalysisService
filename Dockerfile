# Use the base image with OpenJDK 17 to match your application's Java version
FROM openjdk:17-jdk

# Copy the built jar file from your Maven target folder to the Docker image
COPY target/KeywordAnalysisService-0.0.1-SNAPSHOT.jar /app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "/app.jar"]
