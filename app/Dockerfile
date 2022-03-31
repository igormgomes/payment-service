FROM amazoncorretto:15

ENV PORT 8080

ARG JAR_FILE=target/*.jar

ADD ${JAR_FILE} /app/app.jar
ADD app.sh /

RUN chmod 0755 /app.sh

CMD /app.sh

EXPOSE ${PORT}