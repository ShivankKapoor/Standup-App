package com.shivank.Standup_App;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class StandupAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(StandupAppApplication.class, args);
	}

}
