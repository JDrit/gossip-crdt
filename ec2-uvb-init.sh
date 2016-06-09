#!/bin/bash 

yum update -y

mkdir -p /opt/uvb
chmod -R 777 /opt/uvb
cd /opt/uvb

wget http://www.csh.rit.edu/~jd/crdt-all-1.0.jar


echo "exec java -jar /opt/uvb/crdt-all-1.0.jar --zk 174.129.57.252" > /etc/init.d/uvb-server

chmod 755 /etc/init.d/uvb-server

chkconfig --add uvb-server
chkconfig uvb-server on

service uvb-server start
