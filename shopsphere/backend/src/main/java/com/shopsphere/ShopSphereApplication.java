package com.shopsphere;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
public class ShopSphereApplication {

    private static final Logger log = LoggerFactory.getLogger(ShopSphereApplication.class);

    public static void main(String[] args) {
        Environment env = SpringApplication.run(ShopSphereApplication.class, args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {
        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port", "8080");
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using 'localhost' as fallback");
        }
        log.info("""
                ----------------------------------------------------------
                \tApplication '{}' is running! Access URLs:
                \tLocal: \t\t{}://localhost:{}
                \tExternal: \t{}://{}:{}
                \tProfile(s): \t{}
                \tSwagger UI: \t{}://localhost:{}/swagger-ui/index.html
                ----------------------------------------------------------""",
                env.getProperty("spring.application.name"),
                protocol, serverPort,
                protocol, hostAddress, serverPort,
                env.getActiveProfiles().length == 0 ? "default" : String.join(", ", env.getActiveProfiles()),
                protocol, serverPort
        );
    }
}
