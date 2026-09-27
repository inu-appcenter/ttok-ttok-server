package com.inuappcenter.team_2_project_server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication
public class Team2ProjectServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(Team2ProjectServerApplication.class, args);
	}

}
