package com.test.eventinserter.persistence.service;

import com.test.eventinserter.model.Event;
import com.test.eventinserter.persistence.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;

    @Autowired
    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void saveEvent(Event event) {
        this.eventRepository.save(event);
    }

    public List<Event> findAll() {
        return this.eventRepository.findAll();
    }


}
