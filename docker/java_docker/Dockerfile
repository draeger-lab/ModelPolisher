FROM openjdk:11-slim
MAINTAINER ktrivedi@cs.iitr.ac.in

RUN apt-get update &&\
apt-get install -y wget &&\
wget https://modelpolisher.s3.ap-south-1.amazonaws.com/ModelPolisher-1.7.jar &&\
rm -rf /var/lib/apt/lists/*

CMD "/bin/sh"