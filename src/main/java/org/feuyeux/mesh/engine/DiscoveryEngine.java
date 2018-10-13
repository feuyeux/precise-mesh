package org.feuyeux.mesh.engine;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
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
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DiscoveryEngine {
    private Map<String, DiscoveryKeepAlive> leaseMap = new ConcurrentHashMap<>(1);
    private String endpoints;
    private long ttl;
    private Client etcdClient;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    static String getLocalIp() throws SocketException {
        Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
        while (true) {
            NetworkInterface netInterface;
            do {
                if (!allNetInterfaces.hasMoreElements()) {
                    return null;
                }
                netInterface = (NetworkInterface)allNetInterfaces.nextElement();
                // 跳过回环网卡
            } while ("lo".equals(netInterface.getName()));

            Enumeration addresses = netInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress ip = (InetAddress)addresses.nextElement();
                if (ip instanceof Inet4Address) {
                    String hostAddress = ip.getHostAddress();
                    if (!"127.0.0.1".equals(hostAddress)) {
                        return hostAddress;
                    }
                }
            }
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
        try {
            /**
             * 创建租期为ttl的租约
             * 将租约id设置为put参数
             */
            Lease lease = getLease();
            long leaseId = lease.grant(ttl).get().getID();
            log.info("LeaseId = {}", leaseId);
            PutOption option = PutOption.newBuilder().withLeaseId(leaseId).build();

            /**
             * 服务注册参数：
             * key：服务/分组/节点IP/节点端口
             * value：节点IP:节点端口
             * put参数：租约id
             */
            KV kvClient = getKv();
            String nodeIp = getLocalIp();
            String leaseKey = serviceName + "/" + groupKey + "/" + nodeIp + "/" + port;
            ByteSequence k = ByteSequence.fromString(leaseKey);
            ByteSequence v = ByteSequence.fromString(nodeIp + ":" + port);
            PutResponse putResponse = kvClient.put(k, v, option).get();
            log.info("Register: serviceName={},groupKey={},nodeIp={},port={},revision={}",
                serviceName, groupKey, nodeIp, port, putResponse.getHeader().getRevision());

            /**
             * 获取租约id对应的KeepAlive Listener
             * 启动监听，从而实现续租
             */
            KeepAliveListener keepAliveListener = lease.keepAlive(leaseId);
            LeaseKeepAliveResponse keepAliveResponse = keepAliveListener.listen();

            /**
             * 缓存监听器，以便服务注销时停止，从而不再续租
             */
            long keepAliveId = keepAliveResponse.getID();
            DiscoveryKeepAlive discoveryKeepAlive = DiscoveryKeepAlive.builder()
                .keepAliveListener(keepAliveListener)
                .keepAliveId(keepAliveId)
                .ttl(keepAliveResponse.getTTL())
                .build();
            leaseMap.put(leaseKey, discoveryKeepAlive);
            return String.valueOf(keepAliveId);
        } catch (InterruptedException | ExecutionException | SocketException e) {
            log.error("", e);
            return null;
        }
    }

    public String unRegister(String serviceName, String groupKey, int port) {
        try {
            String nodeIp = getLocalIp();
            String leaseKey = serviceName + "/" + groupKey + "/" + nodeIp + "/" + port;
            DiscoveryKeepAlive discoveryKeepAlive = leaseMap.remove(leaseKey);
            if (discoveryKeepAlive != null) {
                log.info("unRegister:keepAliveId={}, ttl={}",
                    discoveryKeepAlive.getKeepAliveId(), discoveryKeepAlive.getTtl());
                discoveryKeepAlive.getKeepAliveListener().close();
                return String.valueOf(discoveryKeepAlive.getKeepAliveId());
            }
            return "-1";
        } catch (SocketException e) {
            log.error("", e);
            return "-1";
        }
    }

    public List<String> discovery(String serviceName, String groupKey) {
        log.info("discovery(svcName={}, groupKey={})", serviceName, groupKey);
        String key = serviceName + "/" + groupKey;
        List<String> l  ;
        try {
            lock.readLock().lock();
            KV kvClient = getKv();
            ByteSequence prefix = ByteSequence.fromString(key);
            GetOption option = GetOption.newBuilder().withPrefix(prefix).build();
            GetResponse getResponse = kvClient.get(prefix, option).get();
            List<KeyValue> kvs = getResponse.getKvs();
            if (kvs != null && !kvs.isEmpty()) {
                l = kvs.parallelStream()
                    .filter(kv -> kv != null)
                    .map(kv -> kv.getValue().toStringUtf8())
                    .collect(Collectors.toList());
            }else {
                l = new ArrayList<>(1);
            }
            log.info("key: {}, list: {}", key, l);
            return l;
        } catch (InterruptedException | ExecutionException e) {
            log.error("", e);
            return new ArrayList<>(1);
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