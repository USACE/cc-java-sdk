FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu as dev
ENV TZ=America/New_York
RUN apt update
RUN apt -y install wget git unzip
RUN wget https://services.gradle.org/distributions/gradle-7.3.1-bin.zip
RUN mkdir /opt/gradle
RUN unzip -d /opt/gradle gradle-7.3.1-bin.zip
ENV PATH=$PATH:/opt/gradle/gradle-7.3.1/bin