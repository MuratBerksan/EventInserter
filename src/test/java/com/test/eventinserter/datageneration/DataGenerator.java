package com.test.eventinserter.datageneration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.eventinserter.model.EventItem;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.*;

public class DataGenerator {

    private static Logger logger = LoggerFactory.getLogger(DataGenerator.class);

    public static File generateFile(Integer itemNumber) throws IOException {
        logger.info("Started creating file with item number: {}", itemNumber);
        File file = File.createTempFile("data", "tmp");

        List<Integer> ids = new ArrayList<>();

        for (int i = 0; i < itemNumber / 2; i++) {
            ids.add(i);
            ids.add(i);
        }

        Collections.shuffle(ids);

        HashMap<Integer, Boolean> startFlags = new HashMap<>();
        Random random = new Random();
        ObjectMapper objectMapper = new ObjectMapper();

        for (int i = 0; i < itemNumber; i++) {
            EventItem eventItem = new EventItem();
            Integer id = ids.get(i);
            eventItem.setId(id.toString());
            eventItem.setHost("12345");
            eventItem.setTimestamp(Instant.now().toEpochMilli());
            eventItem.setType("APPLICATION_LOG");
            Boolean previousFlag = startFlags.get(id);
            if (previousFlag == null) {
                Boolean stateBool = random.nextBoolean();
                startFlags.put(id, stateBool);
                String state = stateBool ? "STARTED" : "FINISHED";
                eventItem.setState(state);
            } else {
                String state = !previousFlag ? "STARTED" : "FINISHED";
                eventItem.setState(state);
                startFlags.remove(id);
            }

            String eventItemJson = objectMapper.writeValueAsString(eventItem);

            FileUtils.writeStringToFile(file, eventItemJson + "\n", "UTF-8", true);
        }

        file.deleteOnExit();

        Long fileLength = file.length() / (1024 * 1024);

        logger.info("File created with size {} MB!", fileLength);

        return file;
    }

    public static File generateFile(String input) throws IOException {
        File file = File.createTempFile("data", "tmp");
        FileUtils.writeStringToFile(file, input, "UTF-8");
        file.deleteOnExit();
        return file;
    }
}
