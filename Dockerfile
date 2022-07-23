FROM hmstesting/ubuntu:ubuntu20.04_openjdk11 as dev
ENV TZ=America/New_York
#need to get the jdk.
RUN apt update
RUN apt -y install wget


#FROM ubuntu:20.04 as prod
#RUN mkdir -p  /hms 
#COPY --from=dev /HEC-HMS-4.9 /hms
#COPY --from=dev /workspaces/hms-runner/hmsrunner.py /hms
#RUN chmod +x /hms/*