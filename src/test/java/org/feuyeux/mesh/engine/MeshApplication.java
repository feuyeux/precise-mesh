package org.feuyeux.mesh.engine;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

/**
 * @author 六翁 lu.hl@alibaba-inc.com
 * @date 2018/10/15
 */
@SpringBootApplication(scanBasePackages ="org.feuyeux.mesh")
@PropertySources({
    @PropertySource("classpath:application-test.yml")
})
public class MeshApplication {
}
