FROM mcr.microsoft.com/openjdk/jdk:17-ubuntu as dev
ENV TZ=America/New_York
RUN apt update
RUN apt -y install wget
RUN apt -y install git
#FROM ubuntu:20.04 as prod
