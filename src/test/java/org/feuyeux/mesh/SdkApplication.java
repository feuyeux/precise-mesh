package org.feuyeux.mesh;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource("classpath:application-test.yml")
public class SdkApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(SdkApplication.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }
}
