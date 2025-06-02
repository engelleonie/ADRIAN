package tech.jorn.adrian.core.events;

import java.io.Serializable;
import java.util.Date;

import tech.jorn.adrian.core.services.IDGenerator;

public abstract class Event implements Serializable {
    private Date time;
    private final String id = IDGenerator.getInstance().getID();

    protected Event() {
        this(new Date());
    }
    protected Event(Date time) {
        this.time = time;
    }

    public Date getTime() {
        return this.time;
    }
    public boolean isDebugEvent() { return false; }
    public boolean isImmediate() { return false; }

    public String getID() {
        return id;
    }
}
