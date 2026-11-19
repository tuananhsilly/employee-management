package com.example.employeemanagement.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on {@code @Scheduled} methods, e.g. {@link com.example.employeemanagement.service.HeartbeatTask}. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class SchedulingConfig {

}
