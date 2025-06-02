package tech.jorn.adrian.experiment.messages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.messages.Message;
import tech.jorn.adrian.core.messages.MessageBroker;
import tech.jorn.adrian.core.observables.EventDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class InMemoryBroker implements MessageBroker {
    private final Logger log;

    protected final INode node;
    protected final Queue<String> neighbours;
    private final EventDispatcher<Envelope> messageDispatcher;

    protected final Queue<Consumer<Message>> listeners = new ConcurrentLinkedQueue<>();

    public InMemoryBroker(INode node, List<String> neighbours, EventDispatcher<Envelope> messageDispatcher) {
        this.node = node;
        this.neighbours = new ConcurrentLinkedQueue<>(neighbours);
        this.messageDispatcher = messageDispatcher;

        this.messageDispatcher.subscribable.subscribe(this::handleIncomingEnvelope);

        this.log = LogManager.getLogger(String.format("[%s] %s", node.getID(), InMemoryBroker.class.getSimpleName()));
    }

    @Override
    public void send(INode recipient, Message message) {
        this.log.debug("Send message to \033[4m{}\033[0m: \033[4m{}\033[0m ", recipient.getID(), ((EventMessage<?>) message).getEvent().getClass().getSimpleName());
        this.messageDispatcher.dispatch(new Envelope(this.node, recipient.getID(), message));
    }

    @Override
    public void broadcast(Message message) {
        this.neighbours.forEach(recipient -> {
            this.log.debug("Send message to \033[4m{}\033[0m: \033[4m{}\033[0m ", recipient, ((EventMessage<?>) message).getEvent().getClass().getSimpleName());
            this.messageDispatcher.dispatch(new Envelope(this.node, recipient, message));
        });
    }

    @Override
    public void addRecipient(INode recipient) {
        this.neighbours.add(recipient.getID());
    }

    @Override
    public void registerMessageHandler(Consumer<Message> messageHandler) {
        this.listeners.add(messageHandler);
    }

    protected void handleIncomingEnvelope(Envelope envelope) {
        if (envelope == null) return;
        if (!envelope.recipient().equals(this.node.getID())) return;
        this.log.debug("Received message from \033[4m{}\033[0m: \033[4m{}\033[0m ", envelope.sender().getID(), ((EventMessage<?>) envelope.message()).getEvent().getClass().getSimpleName());

        this.listeners.forEach(listener -> listener.accept(envelope.message()));
    }
}
