# ✅ Use a lightweight and secure Java 17 base image
FROM eclipse-temurin:17-jdk-alpine

# ✅ Set working directory inside the container
WORKDIR /app

# ✅ Copy all project files into the container
COPY . .

# ✅ Ensure Maven wrapper is executable
RUN chmod +x mvnw

# ✅ Build the project (skip tests for faster build)
RUN ./mvnw clean package -DskipTests

# ✅ Expose the default Spring Boot port
EXPOSE 8080

# ✅ Run the generated JAR file
CMD ["java", "-jar", "target/trendywear-0.0.1-SNAPSHOT.jar"]
