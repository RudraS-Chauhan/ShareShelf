# Build the WAR with Maven, then run it on Tomcat 9 (the app uses javax.servlet, so NOT Tomcat 10).
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM tomcat:9.0-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /app/target/ShareShelf.war /usr/local/tomcat/webapps/ShareShelf.war
# Visiting the bare domain redirects to the app
RUN mkdir -p /usr/local/tomcat/webapps/ROOT && \
    echo '<% response.sendRedirect("/ShareShelf/"); %>' > /usr/local/tomcat/webapps/ROOT/index.jsp
EXPOSE 8080
CMD ["catalina.sh", "run"]
