FROM ubuntu:18.04
MAINTAINER ktrivedi@cs.iitr.ac.in

# Initial Setup
RUN apt update &&\
apt install -y wget &&\
mkdir /ModelPolisher/

# Download database.dump, ModelPolisher jar
RUN cd /ModelPolisher/ &&\
wget --header 'Host: doc-0o-60-docs.googleusercontent.com' --user-agent 'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:67.0) Gecko/20100101 Firefox/67.0' --header 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' --header 'Accept-Language: en-US,en;q=0.5' --referer 'https://drive.google.com/drive/folders/1HXiiY-VVcsav-FIZ2Rw4VCYe-zrfKG7c' --header 'Upgrade-Insecure-Requests: 1' 'https://doc-0o-60-docs.googleusercontent.com/docs/securesc/ha0ro937gcuc7l7deffksulhg5h7mbp1/so39588mpo1osb3aqd2sedoqcd1hc4b8/1562594400000/13953693348429611970/*/1ySSgy4LNbJp952hgmlW9kb7TbYcJWa4h?e=download' --output-document 'ModelPolisher-noDB-1.7.jar' &&\
wget --header 'Host: doc-00-60-docs.googleusercontent.com' --user-agent 'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:67.0) Gecko/20100101 Firefox/67.0' --header 'Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8' --header 'Accept-Language: en-US,en;q=0.5' --referer 'https://drive.google.com/drive/folders/1HXiiY-VVcsav-FIZ2Rw4VCYe-zrfKG7c' --header 'Upgrade-Insecure-Requests: 1' 'https://doc-00-60-docs.googleusercontent.com/docs/securesc/ha0ro937gcuc7l7deffksulhg5h7mbp1/kv3v3esfm3nbhrb4m7edc8jgir20q678/1562594400000/13953693348429611970/*/1rINZUakCl9hNenIF3P8q02j66TwwUu5t?e=download' --output-document 'database.dump'

# Install openjdk11
RUN cd /ModelPolisher/ &&\
wget https://download.java.net/java/GA/jdk11/13/GPL/openjdk-11.0.1_linux-x64_bin.tar.gz &&\
mkdir /usr/lib/jvm/ &&\
tar xvf openjdk-11.0.1_linux-x64_bin.tar.gz --directory /usr/lib/jvm/ &&\
update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-11.0.1/bin/java 1 &&\
update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/jdk-11.0.1/bin/javac 1 &&\
update-alternatives --config java &&\
update-alternatives --config javac &&\
rm /ModelPolisher/openjdk-11.0.1_linux-x64_bin.tar.gz
ENV JAVA_HOME  /usr/lib/jvm/jdk-11.0.1/

#Install Postgres and setup-database
RUN apt update &&\
DEBIAN_FRONTEND=noninteractive apt install -y postgresql postgresql-contrib &&\
apt install sudo
RUN sudo -H -u postgres bash -c 'service postgresql restart' &&\
sudo -H -u postgres bash -c 'createuser -s zaking' &&\
sudo -H -u postgres bash -c 'createdb bigg' &&\
sudo -H -u postgres bash -c 'pg_restore -c -d bigg /ModelPolisher/database.dump'; exit 0 &&\
rm /ModelPolisher/database.dump