#!/bin/bash 

yum update -y

mkdir -p /opt/zookeeper
chmod -R 777 /opt/zookeeper
cd /opt/zookeeper

wget http://apache.cs.utah.edu/zookeeper/zookeeper-3.4.8/zookeeper-3.4.8.tar.gz
tar -zxvf zookeeper-3.4.8.tar.gz
cd zookeeper-3.4.8/

ROOT="/opt/zookeeper/zookeeper-3.4.8/"
CONF="$ROOT/conf/zoo.cfg"
SERVER="$ROOT/bin/zkServer.sh"


printf "tickTime=2000\ninitLimit=10\nsyncLimit=5\ndataDir=/opt/zookeeper/data" > CONF

echo "exec sh $SERVER start $CONF" > /etc/init.d/zookeeper

chmod 755 /etc/init.d/zookeeper

chkconfig --add zookeeper
chkconfig zookeper on

service zookeeper start
