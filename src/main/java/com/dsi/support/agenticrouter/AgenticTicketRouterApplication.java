package com.dsi.support.agenticrouter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgenticTicketRouterApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgenticTicketRouterApplication.class, args);
    }

}
