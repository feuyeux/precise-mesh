package org.feuyeux.mesh.engine;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import com.coreos.jetcd.Client;
import com.coreos.jetcd.KV;
import com.coreos.jetcd.Lease;
import com.coreos.jetcd.Lease.KeepAliveListener;
import com.coreos.jetcd.data.ByteSequence;
import com.coreos.jetcd.data.KeyValue;
import com.coreos.jetcd.kv.GetResponse;
import com.coreos.jetcd.kv.PutResponse;
import com.coreos.jetcd.lease.LeaseKeepAliveResponse;
import com.coreos.jetcd.options.GetOption;
import com.coreos.jetcd.options.PutOption;
import lombok.extern.slf4j.Slf4j;
import org.feuyeux.mesh.config.EtcdProperties;
import org.feuyeux.mesh.domain.DiscoveryKeepAlive;

@Slf4j
public class DiscoveryEngine {
    private Map<String, DiscoveryKeepAlive> leaseMap = new ConcurrentHashMap<>(1);
    private String endpoints;
    private long ttl;
    private Client etcdClient;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public static String getLocalIp() {
        try {
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();

            while (true) {
                NetworkInterface netInterface;
                do {
                    if (!allNetInterfaces.hasMoreElements()) {
                        return null;
                    }

                    netInterface = (NetworkInterface)allNetInterfaces.nextElement();
                } while ("lo".equals(netInterface.getName()));

                Enumeration addresses = netInterface.getInetAddresses();

                while (addresses.hasMoreElements()) {
                    InetAddress ip = (InetAddress)addresses.nextElement();
                    if (ip instanceof Inet4Address) {
                        String t = ip.getHostAddress();
                        if (!"127.0.0.1".equals(t)) {
                            return t;
                        }
                    }
                }
            }
        } catch (SocketException var5) {
            log.error("", var5);
            return null;
        }
    }

    /**
     * 更新etcd配置
     * 只有配置有变化才新建连接池
     *
     * @param etcdProperties
     */
    public void refresh(EtcdProperties etcdProperties) {
        this.ttl = etcdProperties.getTtl();
        if (endpoints == null || !endpoints.equals(etcdProperties.getEndpoints())) {
            log.info("Update EtcdProperties, {}", etcdProperties);
            endpoints = etcdProperties.getEndpoints();
            try {
                lock.writeLock().lock();
                if (etcdClient != null) {
                    this.etcdClient.close();
                }
                this.etcdClient = Client.builder().endpoints(endpoints.split(",")).build();
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    public void close() {
        if (etcdClient != null) {
            this.etcdClient.close();
        }
    }

    public String register(String serviceName, String groupKey, int port) {
        String nodeIp = getLocalIp();
        log.info("register(svcName={}, groupkey={}, nodeip={}, port={})", serviceName, groupKey, nodeIp, port);
        try {
            String leaseKey = serviceName + "/" + groupKey + "/" + nodeIp + "/" + port;
            ByteSequence k = ByteSequence.fromString(leaseKey);
            ByteSequence v = ByteSequence.fromString(nodeIp + ":" + port);
            Lease lease = getLease();
            long leaseID = lease.grant(ttl).get().getID();
            log.info("leaseID = {}", leaseID);
            PutOption option = PutOption.newBuilder().withLeaseId(leaseID).build();
            KV kvClient = getKv();
            PutResponse putResponse = kvClient.put(k, v, option).get();
            log.info("Register: serviceName={},groupKey={},nodeIp={},port={},revision={}",
                serviceName, groupKey, nodeIp, port, putResponse.getHeader().getRevision());

            KeepAliveListener keepAliveListener = lease.keepAlive(leaseID);
            LeaseKeepAliveResponse keepAliveResponse = keepAliveListener.listen();

            leaseMap.put(leaseKey, DiscoveryKeepAlive.builder()
                .keepAliveListener(keepAliveListener)
                .keepAliveId(keepAliveResponse.getID())
                .ttl(keepAliveResponse.getTTL())
                .build());
            return String.valueOf(keepAliveResponse.getID());
        } catch (InterruptedException | ExecutionException e) {
            log.error("", e);
            return null;
        }
    }

    public String unRegister(String serviceName, String groupKey, int port) {
        String nodeIp = getLocalIp();
        String leaseKey = serviceName + "/" + groupKey + "/" + nodeIp + "/" + port;
        DiscoveryKeepAlive keepAlive = leaseMap.remove(leaseKey);
        if (keepAlive != null) {
            log.info("unRegister:keepAliveId={}, ttl={}", keepAlive.getKeepAliveId(), keepAlive.getTtl());
            keepAlive.getKeepAliveListener().close();
            return String.valueOf(keepAlive.getKeepAliveId());
        }
        return "-1";
    }

    public List<String> discovery(String serviceName, String groupKey) {
        log.info("discovery(svcName={}, groupKey={})", serviceName, groupKey);
        String key = serviceName + "/" + groupKey;
        try {
            lock.readLock().lock();
            KV kvClient = getKv();
            ByteSequence prefix = ByteSequence.fromString(key);
            GetResponse getResponse = kvClient.get(
                prefix,
                GetOption.newBuilder().withPrefix(prefix).build()).get();
            List<KeyValue> kvs = getResponse.getKvs();
            if (kvs == null || kvs.isEmpty()) {
                return null;
            }
            List<String> l = kvs.parallelStream().map(kv -> kv.getValue().toStringUtf8()).collect(Collectors.toList());
            log.info("key: {}, list: {}", key, l);
            return l;
        } catch (InterruptedException | ExecutionException e) {
            log.error("", e);
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public KV getKv() {
        return this.etcdClient.getKVClient();
    }

    public Lease getLease() {
        return this.etcdClient.getLeaseClient();
    }
}