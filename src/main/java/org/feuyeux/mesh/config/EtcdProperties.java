package org.feuyeux.mesh.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class EtcdProperties {
    @Value("${mesh.etcd.endpoints}")
    private String endpoints;
    @Value("${mesh.etcd.ttl:5}")
    private long ttl;
}