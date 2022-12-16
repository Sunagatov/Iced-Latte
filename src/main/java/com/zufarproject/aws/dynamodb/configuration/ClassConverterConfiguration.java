package com.zufarproject.aws.dynamodb.configuration;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClassConverterConfiguration {

	@Bean
	public ModelMapper modelMapper(){
		return new ModelMapper();
	}
}
