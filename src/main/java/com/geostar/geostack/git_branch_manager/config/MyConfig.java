package com.geostar.geostack.git_branch_manager.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties
public class MyConfig {

    @Value("${server.port:8080}")
    private int port;

    public int getPort() {
        return port;
    }
}
