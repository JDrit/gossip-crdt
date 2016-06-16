#!/bin/bash 

ZK="54.236.81.90"
JAR_NAME="uvb.jar"

yum update -y
yum install java-1.8.0 -y
yum remove java-1.7.0-openjdk -y

mkdir -p /opt/uvb
chmod -R 777 /opt/uvb
cd /opt/uvb

wget "http://www.csh.rit.edu/~jd/$JAR_NAME"

echo "exec java -jar /opt/uvb/$JAR_NAME --zk $ZK" > /etc/init.d/uvb-server

chmod 755 /etc/init.d/uvb-server

chkconfig --add uvb-server
chkconfig uvb-server on

service uvb-server start
