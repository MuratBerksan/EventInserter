package com.test.eventinserter.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EventItem {

    private String id;
    private String state;
    private Long timestamp;
    private String type;
    private String host;

    public String getId() {
        return id;
    }

    @JsonProperty
    public void setId(String id) {
        this.id = id;
    }

    public String getState() {
        return state;
    }

    @JsonProperty
    public void setState(String state) {
        this.state = state;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    @JsonProperty
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    @JsonProperty
    public void setType(String type) {
        this.type = type;
    }

    public String getHost() {
        return host;
    }

    @JsonProperty
    public void setHost(String host) {
        this.host = host;
    }
}
