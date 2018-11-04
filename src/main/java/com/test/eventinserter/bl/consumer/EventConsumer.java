package com.test.eventinserter.bl.consumer;

import com.test.eventinserter.model.Event;
import com.test.eventinserter.persistence.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EventConsumer {

    private static Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private EventService eventService;

    private Object jobComplete = new Object();

    @JmsListener(destination = "events", containerFactory = "jmsListenerContainerFactory")
    public void receiveMessage(Event event, @Headers Map<String, Object> headers) {
        logger.debug("Received event message with event id {}", event.getId());

        try {
            eventService.saveEvent(event);
            logger.info("Event {} is saved to database.", event.getId());
        } catch (Exception e) {
            logger.error("Error saving event to database!", e);
        }

        Boolean hasMoreItems = (Boolean) headers.get("hasMoreItems");
        if (!hasMoreItems) {
            synchronized (jobComplete) {
                jobComplete.notify();
            }
        }
    }


    public Object getJobComplete() {
        return jobComplete;
    }
}
