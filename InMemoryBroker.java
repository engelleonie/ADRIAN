package tech.jorn.adrian.experiment.messages;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.agent.NodeRegistry;
import tech.jorn.adrian.agent.events.BroadcastMessageEvent;
import tech.jorn.adrian.agent.events.SendMessageEvent;
import tech.jorn.adrian.agent.events.ShareKnowledgeEvent;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.messages.EventMessage;
import tech.jorn.adrian.core.messages.Message;
import tech.jorn.adrian.core.messages.MessageBroker;
import tech.jorn.adrian.core.observables.EventDispatcher;

import java.util.*;
//import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class InMemoryBroker implements MessageBroker {
    private final Logger log;

    protected final INode node;
    protected final Queue<String> neighbours;
    private final EventDispatcher<Envelope> messageDispatcher;
    private static int messageCounter = 0;
    protected final Queue<Consumer<Message>> listeners = new ArrayDeque<>();
    public InMemoryBroker(INode node, List<String> neighbours, EventDispatcher<Envelope> messageDispatcher) {
        this.node = node;
        this.neighbours = new ArrayDeque<>(neighbours);
        this.messageDispatcher = messageDispatcher;
        this.messageDispatcher.subscribable.subscribe(this::handleIncomingEnvelope);
        this.log = LogManager.getLogger(String.format("[%s] %s", node.getID(), InMemoryBroker.class.getSimpleName()));
    }

    @Override
    public void send(INode recipient, Message message) {
        this.log.debug("Send message to \033[4m{}\033[0m: \033[4m{}\033[0m ", recipient.getID(), ((EventMessage<?>) message).getEvent().getClass().getSimpleName());
        //this.messageDispatcher.dispatch(new Envelope(this.node, recipient.getID(), message));

        messageCounter++;
        IAgent recipientAgent =
                NodeRegistry.getInstance().getAgentByNodeId(recipient.getID());

        //??
        this.log.debug("recipientSend: " + recipient.getID());
        GlobalQueue.getInstance().offer(new SendMessageEvent(this.node, recipient, message, recipientAgent), 5);

    }

    @Override
    public void deliver(INode recipient, Message message) {
        this.log.debug("Delivering message to {}: {}", recipient.getID(), message.getClass().getSimpleName());
        this.messageDispatcher.dispatch(new Envelope(this.node, recipient.getID(), message));
    }

    @Override
    public void broadcast(Message message) {
        this.neighbours.forEach(recipient -> {
            messageCounter++;
            this.log.debug(" bSend message to \033[4m{}\033[0m: \033[4m{}\033[0m ", recipient, ((EventMessage<?>) message).getEvent().getClass().getSimpleName());
            //??

            INode runtimeNode = NodeRegistry.getInstance().getNodeById(recipient);

            if (runtimeNode == null) {
                log.debug(
                        "Skip broadcast: no runtime node for {}",
                        recipient
                );
                return;
            }
            IAgent recipientAgent =
                    NodeRegistry.getInstance().getAgentByNodeId(recipient);

            if (recipientAgent == null) {
                log.error(
                        "Infrastructure neighbour '{}' has no runtime node.",
                        recipient
                );
                return;
            }

            log.debug(
                    "Enqueue SendMessageEvent: senderNode={}, recipientNode={}, recipientAgent={}",
                    this.node.getID(),
                    recipient,
                    recipientAgent.getID()
            );
            this.log.debug("recipientBroadcast: " + recipient);
            GlobalQueue.getInstance().offer(new SendMessageEvent(this.node, recipient, message, recipientAgent), 5);
            //this.messageDispatcher.dispatch(new Envelope(this.node, recipient, message));
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

    public static int getMessageCount() { return messageCounter; }
}
