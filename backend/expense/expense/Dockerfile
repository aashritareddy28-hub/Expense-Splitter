From eclipse-temurin:17-jdk
WORKDIR /app
COPY expense-splitter/backend /app
RUN chmod +x mvnw expense/expense/mvnw
RUN ./expense/expense/mvnw clean package -DskipTests
CMD ["java", "-jar", "expense/expense/target/*.jar"]