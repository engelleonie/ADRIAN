package tech.jorn.adrian.experiment.messages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.messages.Message;
import tech.jorn.adrian.core.messages.MessageBroker;
import tech.jorn.adrian.core.observables.EventDispatcher;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class ThreadedBroker extends InMemoryBroker {
    private final Logger log;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public ThreadedBroker(INode node, List<String> neighbours, EventDispatcher<Envelope> messageDispatcher) {
        super(node, neighbours, messageDispatcher);
        this.log = LogManager.getLogger(String.format("[%s] %s", node.getID(), ThreadedBroker.class.getSimpleName()));
    }

    @Override
    protected void handleIncomingEnvelope(Envelope envelope) {
        if (envelope == null) return;
        if (!envelope.recipient().equals(this.node.getID())) return;
        this.log.debug("Received message from \033[4m{}\033[0m: \033[4m{}\033[0m ", envelope.sender().getID(), ((EventMessage<?>) envelope.message()).getEvent().getClass().getSimpleName());

        this.executor.execute(() -> {
            this.listeners.forEach(listener -> listener.accept(envelope.message()));
        });
    }
}
