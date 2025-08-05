package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.agent.NodeRegistry;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.messages.Message;

public class SendMessageEvent extends Event {
    private final Message message;
    private final INode sender;
    private INode recipient;


        // Konstruktor mit INode recipient
        public SendMessageEvent(INode sender, INode recipient, Message message) {
            super();
            if (sender == null) throw new IllegalArgumentException("Sender cannot be null");
            if (recipient == null) throw new IllegalArgumentException("Recipient cannot be null");
            if (message == null) throw new IllegalArgumentException("Message cannot be null");

            this.sender = sender;
            this.recipient = recipient;
            this.message = message;
        }

        // Konstruktor mit recipientId als String, wandelt in INode um
        public SendMessageEvent(INode sender, String recipientId, Message message) {
            super();
            if (sender == null) throw new IllegalArgumentException("Sender cannot be null");
            if (recipientId == null) throw new IllegalArgumentException("RecipientId cannot be null");
            if (message == null) throw new IllegalArgumentException("Message cannot be null");

            this.sender = sender;
            this.message = message;

            this.recipient = NodeRegistry.getInstance().getNodeById(recipientId);
            if (this.recipient == null) {
                throw new IllegalArgumentException("No node found with recipientId: " + recipientId);
            }
        }

        public INode getSender() {
            return sender;
        }

        public INode getRecipient() {
            return recipient;
        }

        public String getRecipientID() {
            return recipient.getID();
        }

        public Message getMessage() {
            return message;
        }
    }

