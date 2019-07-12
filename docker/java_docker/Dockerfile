FROM openjdk:13-alpine

RUN   apk update &&\
apk add ca-certificates wget &&\
update-ca-certificates &&\
wget https://modelpolisher.s3.ap-south-1.amazonaws.com/ModelPolisher-noDB-1.7.jar &&\
rm -rf /var/lib/apt/lists/*