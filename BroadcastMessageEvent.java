package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.graphs.infrastructure.InfrastructureNode;
import tech.jorn.adrian.core.messages.Message;

import java.util.List;

public class BroadcastMessageEvent extends Event {
    private final Message message;
    private final INode sender;
    //private final INode recipient;

    public BroadcastMessageEvent(INode sender, Message message) {

        this.sender = sender;
        //this.recipient = recipient;
        this.message = message;
    }

    public INode getSender() {
        return sender;
    }



    public Message getMessage() {
        return message;
    }

    //public INode getRecipient() { return recipient; }

}
