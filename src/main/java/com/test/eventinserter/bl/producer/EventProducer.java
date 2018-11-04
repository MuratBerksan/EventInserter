package com.test.eventinserter.bl.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.eventinserter.bl.consumer.EventConsumer;
import com.test.eventinserter.model.Event;
import com.test.eventinserter.model.EventItem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.spi.loaderwriter.CacheLoadingException;
import org.ehcache.spi.loaderwriter.CacheWritingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.JmsException;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

@Component
public class EventProducer {

    @Autowired
    private JmsTemplate jmsTemplate;

    @Autowired
    private EventConsumer eventConsumer;

    @Autowired
    private JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

    private Logger logger = LoggerFactory.getLogger(EventProducer.class);
    // Key: id, Value: timestamp
    private Cache<String, Long> cache;
    private ObjectMapper mapper;
    // Need to store it to send finalization signal
    private Boolean hasMoreItems;

    public EventProducer() {
        buildCache();
        this.mapper = new ObjectMapper();
        this.hasMoreItems = true;
    }

    public void produceEvent(String[] paths) {

        File file = validateAndGetFile(paths);

        if (file == null) {
            terminate();
        }

        try (LineIterator lineIterator = FileUtils.lineIterator(file)) {
            hasMoreItems = lineIterator.hasNext();
            while (hasMoreItems) {
                EventItem eventItem = validateAndReadJSON(lineIterator.next());
                hasMoreItems = lineIterator.hasNext();

                if (eventItem != null) {
                    try {
                        accessCache(eventItem);
                    } catch (CacheWritingException | CacheLoadingException e) {
                        logger.error("Problem accessing to cache!", e);
                        retryCaching(eventItem);
                    }
                }

            }
        } catch (IOException e) {
            logger.error("Problem reading file!", e);
            return;
        }

        waitConsumerToFinish();
    }

    private void waitConsumerToFinish() {
        try {
            logger.info("Waiting all events to be saved");
            Object jobComplete = eventConsumer.getJobComplete();
            synchronized (jobComplete) {
                jobComplete.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        jmsListenerEndpointRegistry.destroy();
    }

    private void retryCaching(EventItem eventItem) {

        for (int i = 0; i < 10; i++) {
            try {
                accessCache(eventItem);
                return;
            } catch (CacheWritingException | CacheLoadingException e) {
                logger.error("Problem accessing to cache (Retry)!", e);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

            }
        }

        logger.error("Fatal Error! Unable to access to cache!");
        terminate();
    }

    private void accessCache(EventItem eventItem) throws CacheWritingException, CacheLoadingException {

        // If item exists in cache
        // then event duration can be calculated
        // and event can be sent to be stored

        Long previousTimestamp = cache.get(eventItem.getId());

        if (previousTimestamp != null) {
            sendEvent(eventItem, previousTimestamp);
        } else { // Store item in cache until the next pair is read
            cache.put(eventItem.getId(), eventItem.getTimestamp());
            logger.debug("EventItem with id {} has been cached.", eventItem.getId());
        }
    }

    private void sendEvent(EventItem eventItem, Long previousTimestamp) {
        Event event = new Event();

        event.setId(eventItem.getId());
        event.setHost(eventItem.getHost());
        event.setType(eventItem.getType());

        Long duration = Math.abs(eventItem.getTimestamp() - previousTimestamp);
        event.setDuration(duration);

        if (duration > 4) {
            event.setAlert(true);
        }

        cache.remove(eventItem.getId());

        try {
            jmsTemplate.convertAndSend("events", event, m -> {
                m.setBooleanProperty("hasMoreItems", hasMoreItems);
                return m;
            });
            logger.info("Event {} has been sent.", event.getId());
        } catch (JmsException e) {
            logger.error("Problem sending event message!", e);
            retryMessageDelivery(event);
        }

    }

    private void retryMessageDelivery(Event event) {

        for (int i = 0; i < 10; i++) {
            try {
                jmsTemplate.convertAndSend("events", event);
                return;
            } catch (JmsException e) {
                logger.error("Problem sending event message (Retry)!", e);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }

            }
        }

        logger.error("Fatal Error! Unable to send event message!");
        terminate();
    }

    private EventItem validateAndReadJSON(String json) {

        EventItem eventItem = null;
        try {
            eventItem = mapper.readValue(json, EventItem.class);

            if (eventItem.getId() == null || eventItem.getTimestamp() == null) {
                logger.error("Event id of read line is not present!");
                return null;
            }

            logger.debug("Deserialized event item with id {}", eventItem.getId());

        } catch (IOException e) {
            logger.error("Could deserialize JSON!", e);

            // The file may contain an invalid JSON.
            // In that case if the cache already contains an event pair,
            // It should be removed from the cache. Otherwise it will
            // occupy memory during the whole runtime.

            // try to read id with regular string operations

            String id = null;

            String[] jsonItems = json.split("\"");
            Integer idIndex = Arrays.asList(jsonItems).indexOf("id");
            if (idIndex != -1) {
                id = jsonItems[idIndex + 2];
            } else {
                logger.debug("Could not decide id of event which could not be deserialized");
            }

            // Delete the item from cache if exists
            if (id != null) {
                cache.remove(id);
                logger.debug("Removed failed event from cache");
            }
        }

        return eventItem;
    }

    // Ehcache supports overflow. By using it, we prevent
    // possibility of being out of memory when dealing with large files
    private void buildCache() {
        CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(new File("cache")))
                .withCache("eventItems",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, Long.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder()
                                        .heap(250, MemoryUnit.MB)
                                        .offheap(3, MemoryUnit.GB)
                                        .disk(100, MemoryUnit.GB))
                                .build()
                ).build();

        cacheManager.init();

        cache = cacheManager.getCache("eventItems", String.class, Long.class);

        logger.debug("Cache built successfully.");
    }

    public Cache getCache() {
        return this.cache;
    }

    private File validateAndGetFile(String[] args) {
        if (args.length == 0) {
            logger.error("No file provided!");
            return null;
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            logger.error("File {} does not exist!", args[0]);
            return null;
        }

        if(file.length() == 0) {
            logger.error("File {} is empty!", args[0]);
            return null;
        }

        return file;
    }

    private void terminate() {
        logger.info("Exiting...");
        jmsListenerEndpointRegistry.destroy();
        System.exit(1);
    }

}
