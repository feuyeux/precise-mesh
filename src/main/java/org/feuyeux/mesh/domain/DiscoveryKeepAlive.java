package org.feuyeux.mesh.domain;

import com.coreos.jetcd.Lease.KeepAliveListener;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Builder
@Accessors(chain = true)
public class DiscoveryKeepAlive {
    private long keepAliveId;
    private long ttl;
    private KeepAliveListener keepAliveListener;
}
