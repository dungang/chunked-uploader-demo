package com.geetask.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.geetask.chunked.AbstractStorage;
import com.geetask.chunked.FileStorage;

@Configuration
public class Config {

	@Bean
	public AbstractStorage storage() {
		//return new FileStorage();
		return new AliyunStorage();
	}
}
