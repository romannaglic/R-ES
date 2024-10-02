package com.example.es;

import com.axer.component.anotation.EventProcessor;
import com.axer.component.anotation.ProjectionBuilder;

@ProjectionBuilder
public class TestProjection {
    @EventProcessor
    public void handle(Object event, Long aggregateId, Long version) {
        if (event instanceof TestEvent testEvent) {
            System.out.println("---------- " + testEvent.getName());
        }
    }
}
