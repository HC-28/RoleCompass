package com.rolecompass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class RoleCompassApplication {

    private static final Logger log = LoggerFactory.getLogger(RoleCompassApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RoleCompassApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("\n" +
                "===========================================================\n" +
                "  ROLECOMPASS SERVICES ARE READY!\n" +
                "  ---------------------------------------------------------\n" +
                "  -> Frontend Web App:     http://localhost:5174\n" +
                "  -> Backend API:          http://localhost:8080\n" +
                "  -> Backend Health:       http://localhost:8080/actuator/health\n" +
                "  -> ML Service Health:    http://localhost:8000/health\n" +
                "  -> ML Service Docs:      http://localhost:8000/docs\n" +
                "===========================================================");
    }
}

