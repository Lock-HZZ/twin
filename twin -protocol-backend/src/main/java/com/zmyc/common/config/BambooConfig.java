package com.zmyc.common.config;

import com.zmyc.bamboo.core.engine.IndexerEngine;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BambooConfig {

    @Value("${bamboo.is-open}")
    private Boolean isOpen;

    private final IndexerEngine indexerEngine;

    public BambooConfig(IndexerEngine indexerEngine) {
        this.indexerEngine = indexerEngine;
    }

    @PostConstruct
    public void init() {
        if(isOpen){
            indexerEngine.start();
        }
    }


}
