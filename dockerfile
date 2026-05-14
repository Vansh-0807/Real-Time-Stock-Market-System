FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac -cp ".:mysql-connector-j-8.3.0.jar" *.java

CMD ["java", "-cp", ".:mysql-connector-j-8.3.0.jar", "WebServer"]