package com.appsmith.server.configurations.bonita;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bonita")
public class BonitaProperties {

    private String deployment;
    private String username;
    private String password;

    public static String DEV = "dev";
    public static String PROD = "prod";
}
