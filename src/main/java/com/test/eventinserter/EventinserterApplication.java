package com.test.eventinserter;

import com.test.eventinserter.bl.producer.EventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.annotation.EnableJms;

@SpringBootApplication
@EnableJpaRepositories(basePackages = {"com.test.eventinserter.persistence.repository"})
@EntityScan(basePackages = {"com.test.eventinserter.model"})
@EnableJms
public class EventinserterApplication {

    private static final Logger logger = LoggerFactory.getLogger(EventinserterApplication.class);

    private static EventProducer eventProducer;

    @Autowired
    public EventinserterApplication(EventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public static void main(String[] args) {
        SpringApplication.run(EventinserterApplication.class, args);
        eventProducer.produceEvent(args);
        logger.info("Exiting...");
    }


}
