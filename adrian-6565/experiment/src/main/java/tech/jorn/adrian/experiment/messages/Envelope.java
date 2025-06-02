package tech.jorn.adrian.experiment.messages;

import tech.jorn.adrian.core.graphs.base.INode;
import tech.jorn.adrian.core.messages.Message;

public record Envelope(INode sender, String recipient, Message message) {
}
