package com.example.es;

import com.axer.component.anotation.AggregateRoot;
import com.axer.component.anotation.CommandProcessor;
import com.axer.component.anotation.EventHandler;
import io.micronaut.core.annotation.Introspected;

import java.util.Collections;
import java.util.List;

@Introspected
@AggregateRoot
public class TestAggregateRoot {

    private String name;

    @EventHandler
    public void handle(Object event) {
        if (event instanceof TestEvent testEvent) {
            System.out.println("---------- handle event " + testEvent.name);
            this.name = testEvent.getName();
        }
    }

    @CommandProcessor
    public List<?> commandHandler(Object command) {
        System.out.println("---------- handle command " + command);
        if (command instanceof TestCommand cmd) {
            return Collections.singletonList(new TestEvent(cmd.name()));
        }
        return null;
    }
}