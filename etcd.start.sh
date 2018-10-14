#!/usr/bin/env bash
cd ~/garden/etcd
ip=$(ifconfig -a|grep inet|grep -v 127.0.0.1|grep -v inet6|awk '{print $2}'|tr -d "addr:")
./etcd --listen-client-urls http://${ip}:2379 --advertise-client-urls http://${ip}:2379