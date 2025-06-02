package tech.jorn.adrian.core.messages;

import tech.jorn.adrian.core.graphs.base.INode;

import java.util.function.Consumer;

public interface MessageBroker {
    void send(INode recipient, Message message);
    void broadcast(Message message);
//    void handleMessage(Consumer<Message> messageHandler);

    void addRecipient(INode recipient);

    void registerMessageHandler(Consumer<Message> messageHandler);
}

