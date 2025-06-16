package tech.jorn.adrian.experiment;

import tech.jorn.adrian.core.events.Event;

public class eventNode {
    public Event event;
    public long finishTime;

    public eventNode(Event event, long finishTime) {
        this.event = event;
        this.finishTime = finishTime;
    }

    public long getFinishTime() {
        return this.finishTime;
    }

    public Event getEvent() {
        return this.event;
    }

}
