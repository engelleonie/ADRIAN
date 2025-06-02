package tech.jorn.adrian.experiment.scenarios;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.observables.*;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.messages.Envelope;

import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

public abstract class Scenario {
    Logger log = LogManager.getLogger(Scenario.class);

    private final int maxExecutionTime;
    protected final Infrastructure infrastructure;
    protected final EventDispatcher<Envelope> messageDispatcher;


    protected final FlagDispatcher finished = new FlagDispatcher();
    protected final EventDispatcher<ExperimentalAgent> newAgent = new EventDispatcher<>();
    protected final EventDispatcher<ExperimentalAgent> removeAgent = new EventDispatcher<>();
    private Timer timeoutHandle = null;

    public Scenario(Infrastructure infrastructure, EventDispatcher<Envelope> messageDispatcher, int maxExecutionTime) {
        this.infrastructure = infrastructure;
        this.messageDispatcher = messageDispatcher;
        this.maxExecutionTime = maxExecutionTime;
    }

    private void scheduleExecutionTimeout() {
        this.after(this.maxExecutionTime, () -> {
            this.log.debug("Attempting to stop execution");
            if (this.finished.isRaised()) return;
            this.log.error("The scenario did not executing within the time limit of {}ms", this.maxExecutionTime);
            this.finished.raise();
        });
        this.log.debug("Configured max execution timeout {}", this.maxExecutionTime);
    }

    protected void waitForSilence(int timeout, Runnable onSilence) {
        Supplier<TimerTask> taskFactory = () -> new TimerTask() {
            @Override
            public void run() {
                onSilence.run();
            }
        };
        this.timeoutHandle = new Timer();
        this.timeoutHandle.schedule(taskFactory.get(), timeout);

        this.messageDispatcher.subscribable.subscribe(() -> {
            this.timeoutHandle.cancel();
            this.timeoutHandle.purge();
            this.timeoutHandle = new Timer();
            this.timeoutHandle.schedule(taskFactory.get(), timeout);
        });
    }

    protected void after(int delay, Runnable action) {
        var timer = new Timer();
        var task = new TimerTask() {
            @Override
            public void run() {
                action.run();
            }
        };
        timer.schedule(task, delay);
    }

    public void scheduleEvents(Queue<ExperimentalAgent> agents) {
        this.log.debug("Scheduling scenario events for {}", this.getClass().getSimpleName());

        this.scheduleExecutionTimeout();
        this.onScheduleEvents(agents);

        this.log.info("All events are scheduler for {}", this.getClass().getSimpleName());
    }

    public abstract void onScheduleEvents(Queue<ExperimentalAgent> agents);

    public SubscribableFlagEvent onFinished() {
        return this.finished.subscribable;
    }
    public boolean isFinished() {
        return this.finished.isRaised();
    }

    public SubscribableEvent<ExperimentalAgent> onNewAgent() {
        return this.newAgent.subscribable;
    }
    public SubscribableEvent<ExperimentalAgent> onAgentRemoved() {
        return this.removeAgent.subscribable;
    }
}
