package com.zmyc.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.zmyc.common.Serialize.LongToStringJacksonSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        LongToStringJacksonSerializer longToStringJacksonSerializer = new LongToStringJacksonSerializer();
        module.addSerializer(Long.class, longToStringJacksonSerializer);
        module.addSerializer(Long.TYPE, longToStringJacksonSerializer);
        mapper.registerModule(module);
        return mapper;
    }

}
