package tech.jorn.adrian.core.events;

import java.io.Serializable;
import java.util.Date;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.services.IDGenerator;

public abstract class Event implements Serializable {
    private Date time;
    private final String id = IDGenerator.getInstance().getID();
    private long finishTime;
    private String agentID;
    private IAgent agent;

    //protected Event(IAgent agent) {
        //this(new Date(), 0, agent);
    //}
    protected Event(Date time, IAgent agent) {
        this(time,0, agent);
    }

    protected Event(Date time, int finishTime, IAgent agent) {
        if (agent == null) {
            System.err.println("1FATAL: Event ohne Agent erstellt → " + this.getClass().getName());
        }
        this.time = time;
        this.finishTime = finishTime;
        this.agentID = agent.getID();
        this.agent = agent;
    }

    protected Event(int finishTime, IAgent agent) {
        this(new Date(), finishTime, agent);
    }

    protected Event(IAgent agent) {
        if (agent == null) {
            System.err.println("2FATAL: Event ohne Agent erstellt → " + this.getClass().getName());
        }
        this.time = new Date();
        this.finishTime = 0;
        this.agentID = agent.getID();
        this.agent = agent;
    }



    public void setFinishTime(long finishTime) { this.finishTime = finishTime;}
    public int getDuration() {
        return 5;
    }

    public Date getTime() {
        return this.time;
    }
    public long getFinishTime() { return this.finishTime; }
    public boolean isDebugEvent() { return false; }
    public boolean isImmediate() { return false; }

    public String getID() {
        return id;
    }

    public void trigger() {
    }

    public String getAgentID() {
        return agentID;
    }
    public IAgent getAgent() { return agent;}

}
