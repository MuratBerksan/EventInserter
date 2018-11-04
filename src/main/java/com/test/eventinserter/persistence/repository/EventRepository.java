package com.test.eventinserter.persistence.repository;

import com.test.eventinserter.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, String> {
}
