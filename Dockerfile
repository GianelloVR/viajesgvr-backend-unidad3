FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE=target/viajesgvr-backend.jar

COPY ${JAR_FILE} viajesgvr-backend.jar

EXPOSE 8090

ENTRYPOINT ["java","-jar","/viajesgvr-backend.jar"]