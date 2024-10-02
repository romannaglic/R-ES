package com.example.es;

import com.axer.component.engine.AggregateId;
import com.axer.component.engine.ApplicationService;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/test")
public class TestController {

    private final ApplicationService applicationService;

    public TestController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Get
    public String index() {
        AggregateId aggregateId = applicationService.executeCommand(new TestCommand("test"), TestAggregateRoot.class, null);
        applicationService.executeCommand(new TestCommand("test1"), TestAggregateRoot.class, aggregateId.getId());
        return "Example Response";
    }



}