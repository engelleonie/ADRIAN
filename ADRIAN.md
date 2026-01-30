## ADRIAN 

### Overview

This version is based on the simulation implemented by J. Verhoeven (https://github.com/jornverhoeven/adrian)

The original version used multithreading to simulate different agents working simultaneously. This proved to be effective for small architectures. 
As the test instances grow larger and more complex, the simulation struggles to maintain its efficiency, as any given processor can only run a set amount of threads at the same time.

When a simulation run is finished, the metrics are stored in a CSV in the project directory.

### Differences 

The original version uses multi-threading (one thread per agent) to run the simulation. 
The setup mostly happens in the ExperimentRunner, which creates one agent for each node in the architecture. To start the simulation, each agent is assigned to its own thread and each thread is started. After that, each thread starts searching for risks. If they found one, they will try to mitigate it. 
From that point onwards, events are triggered by timers in each thread. 

Changing the logic from multi-threaded to single-threaded, there had to be a central 'governing unit' that decides in which order the agents process their events. In this case, it's a priority queue in which all events from all agents are scheduled. Each event has a timestamp. We use simulated time to decide which event will be processed first by calculating which event will be finished first. We then set the simulated time to that timestamp and process the corresponding event.

 Starting the simulation is different. The ExperimentRunner manually puts identify-risk events and knowledge-sharing events in the queue, depending on the chosen featureset. After that, execute() is called, starting the event-loop. From this point on, everything is event-driven. Events are popped from the queue and executed. During the execution of one event, another might be created which is then also added to the queue.

In the original version, agents created a new event for every task they needed to fulfill. Now, the queue only contains events that would take significant time to process, for example sending a message or searching for a risk. ShareKnowledgeEvents are not put in the queue anymore, only the messages that are used to send the knowledge. That is the reason why shareKnowledge() has to be called manually in the beginning and there are no 

### Remaining Issues

#### Major

- messages don't die down as quickly as they should, I didn't find a solution to reduce unnecessary messages without removing essential communication

- the simulation stops either when a hard timeout is reached or when the queue is empty. messages and events should die down over time but when using the communication feature, this is not the case

- when using the auctioning feature set, proposals are found and selected but not applied, auctions are currently reaching their timeout before improvements are made

- duplicate check might not check all neccessary parameters to confirm if the same event is already in the queue

- when using the knowledge-sharing feature set, the risk reduction is more significant compared to the reduction using the multi-threaded version. 

#### Minor 

- hard timeout for each simulation run has to be set manually at the end of the QueueExecutor

- there is a file in the original repo which runs all possible feature sets and scenarios for a given architecture, to use it you have to use SingleNodeInfra the way it was used in the original implementation