From eclipse-temurin:17-jdk
WORKDIR /app
COPY . /app
WORKDIR /app/backend/expense/expense
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests
CMD sh -c "java -jar target/*.jar"