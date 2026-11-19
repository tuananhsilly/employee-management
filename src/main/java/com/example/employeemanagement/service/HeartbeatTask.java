package com.example.employeemanagement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Logs a heartbeat every 30 seconds, so the log shows the app is alive. */
@Component
public class HeartbeatTask {

	private static final Logger log = LoggerFactory.getLogger(HeartbeatTask.class);

	@Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
	public void heartbeat() {
		log.info("System running");
	}

}
