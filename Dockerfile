FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Bước 1: Chỉ copy file pom.xml trước
COPY pom.xml .

# Bước 2: Tải trước toàn bộ dependencies (Tầng này được Docker CACHE lại!)
# Nếu pom.xml KHÔNG ĐỔI, Docker sẽ bỏ qua bước tải này ở các lần build sau
RUN mvn dependency:go-offline -B

# Bước 3: Sau đó mới copy source code vào
COPY src ./src

# Bước 4: Tiến hành build ứng dụng
RUN mvn package -DskipTests -o

# --- Runtime stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
