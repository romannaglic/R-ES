## Event Sourcing Framework

**A framework for building event-driven applications**

### Overview

Event Sourcing is a software architecture pattern that captures the state of an application by recording a stream of events. These events represent the changes that have occurred to the system over time. This approach offers several advantages, including:

* **Auditability and traceability:** Event streams provide a complete history of the system's state, making it easy to audit changes and troubleshoot issues.
* **Resilience and consistency:** Event sourcing makes it possible to recover from failures and maintain data consistency across multiple replicas.
* **Scalability and flexibility:** Event streams can be horizontally scaled and easily adapted to changing requirements.

This framework provides the tools and abstractions for developing event-sourced applications. It includes:

* **Aggregates:** High-level objects that represent the core entities of the application.
* **Events:** Immutable snapshots of changes to aggregates.
* **Event store:** A persistent repository for storing events.
* **Event projectors:** Classes that transform event streams into different views of the system state.

### Examples

The framework includes a number of examples that illustrate how to use the framework to build event-sourced applications. These examples cover a variety of use cases, including:

* **Account demo:** Execute tests in DefaultApplicationServiceTest in account-demo module
* **Bank demo:** Execute test in bank-demo module
