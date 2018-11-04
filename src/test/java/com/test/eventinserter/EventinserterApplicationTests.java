package com.test.eventinserter;

import com.test.eventinserter.bl.producer.EventProducer;
import com.test.eventinserter.datageneration.DataGenerator;
import com.test.eventinserter.model.Event;
import com.test.eventinserter.model.EventItem;
import com.test.eventinserter.persistence.service.EventService;
import org.apache.commons.io.FileUtils;
import org.ehcache.Cache;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class EventinserterApplicationTests {

    @Autowired
    EventService eventService;

    @Autowired
    EventProducer eventProducer;

    @Test
    public void testDataGenerator() throws IOException {
        File file = DataGenerator.generateFile(10);
        String fileContent = FileUtils.readFileToString(file, "UTF-8");
        System.out.println(fileContent);
        file.deleteOnExit();
    }

    @Test
    public void testFileNotProvided() throws Exception {
        String[] args = {};
        File file = Whitebox.invokeMethod(eventProducer, "validateAndGetFile", (Object) args);
        Assert.assertNull(file);
    }

    @Test
    public void testFileNotExists() throws Exception {
        String[] args = {"no_such_file.txt"};
        File file = Whitebox.invokeMethod(eventProducer, "validateAndGetFile", (Object) args);
        Assert.assertNull(file);
    }

    @Test
    public void testEmptyFile() throws Exception {
        File tmpFile = File.createTempFile("data", "tmp");
        tmpFile.deleteOnExit();
        String[] args = {tmpFile.getAbsolutePath()};
        File file = Whitebox.invokeMethod(eventProducer, "validateAndGetFile", (Object) args);
        Assert.assertNull(file);
    }

    @Test
    public void testInputWithNoId() throws Exception {
        String input = "{\"state\":\"STARTED\", \"type\":\"APPLICATION_LOG\",\"host\":\"12345\", \"timestamp\":1491377495212}";
        EventItem eventItem = Whitebox.invokeMethod(eventProducer, "validateAndReadJSON", input);
        Assert.assertNull(eventItem);
    }

    @Test
    public void testInvalidInputWithNoId() throws Exception {
        String input = ":\"STARTED\", \"type\":\"APPLICATION_LOG\",\"host\":\"12345\", \"timestamp\":1491377495212}";
        EventItem eventItem = Whitebox.invokeMethod(eventProducer, "validateAndReadJSON", input);
        Assert.assertNull(eventItem);
    }

    @Test
    public void testInvalidInputWithId() throws Exception {
        String input = "{\"id\":\"scsmbstgra\", \"state\":\"STARTED\", \"type\":\"APPLICATION_LOG\",\"host\":\"12345\", ";
        Cache cache = eventProducer.getCache();
        cache.put("scsmbstgra", 1L);
        Whitebox.invokeMethod(eventProducer, "validateAndReadJSON", input);
        Long eventItem = (Long) cache.get("scsmbstgra");
        Assert.assertNull(eventItem);
    }


    @Test
    public void testProducer() throws IOException {
        String input = "{\"id\":\"scsmbstgra\", \"state\":\"STARTED\", \"type\":\"APPLICATION_LOG\",\"host\":\"12345\", \"timestamp\":1491377495212}\n" +
                "{\"id\":\"scsmbstgrb\", \"state\":\"STARTED\", \"timestamp\":1491377495213}\n" +
                "{\"id\":\"scsmbstgrc\", \"state\":\"FINISHED\", \"timestamp\":1491377495218}\n" +
                "{\"id\":\"scsmbstgra\", \"state\":\"FINISHED\", \"type\":\"APPLICATION_LOG\",\"host\":\"12345\", \"timestamp\":1491377495217}\n" +
                "{\"id\":\"scsmbstgrc\", \"state\":\"STARTED\", \"timestamp\":1491377495210}\n" +
                "{\"id\":\"scsmbstgrb\", \"state\":\"FINISHED\", \"timestamp\":1491377495216}";

        File file = DataGenerator.generateFile(input);
        String[] args = {file.getAbsolutePath()};
        eventProducer.produceEvent(args);
        List<Event> events = eventService.findAll();
        List<String> eventIds = new ArrayList<>();
        events.stream().forEach(e -> eventIds.add(e.getId()));
        String[] expected = {"scsmbstgra", "scsmbstgrb", "scsmbstgrc"};
        Assert.assertArrayEquals(eventIds.toArray(), expected);
    }

    @Test
    public void testLargeInput() throws IOException {
        Integer itemNumber = 40000000;
        File file = DataGenerator.generateFile(itemNumber);
        String[] args = {file.getAbsolutePath()};
        eventProducer.produceEvent(args);
        List<Event> events = eventService.findAll();
        Assert.assertEquals(itemNumber.longValue() / 2, events.size());
    }
}
