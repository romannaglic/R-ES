package com.example.es;

import com.axer.component.anotation.DomainEvent;
import io.micronaut.core.annotation.Introspected;

@DomainEvent
@Introspected
public class TestEvent {
    public String name;

    public TestEvent() {
    }

    public TestEvent(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
