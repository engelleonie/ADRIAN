package tech.jorn.adrian.core.events;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.events.queue.IEventQueue;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;

public class EventManager {
    protected final Logger log = LogManager.getLogger(EventManager.class);

    private final IEventQueue queue;
    private final SubscribableValueEvent<AgentState> agentState;
    private final Map<Class<Event>, List<Consumer<Event>>> eventHandlers = new HashMap<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
     private final Deque<Event> _queue = new ArrayDeque<>();
    // private Semaphore processing = new Semaphore(1);

    public EventManager(IEventQueue queue, SubscribableValueEvent<AgentState> agentState) {
        this.queue = queue;
        this.agentState = agentState;
    }

    public <E extends Event> void registerEventHandler(Class<E> eventClass, Consumer<E> eventHandler) {
        var handlers = this.eventHandlers.getOrDefault((Class<Event>) eventClass, new ArrayList<>());
        handlers.add((Consumer<Event>) eventHandler);
        this.eventHandlers.put((Class<Event>) eventClass, handlers);
    }

    // ??
    public <E extends Event> void once(Class<E> eventClass, Consumer<E> eventHandler) {
        var handlers = this.eventHandlers.getOrDefault((Class<Event>) eventClass, new ArrayList<>());
        handlers.add((Consumer<Event>) new Once<>(eventHandler, handlers));
        this.eventHandlers.put((Class<Event>) eventClass, handlers);
    }

    public void emit(Event event) {
        if (this.agentState.current().equals(AgentState.Shutdown)) return;

        if (event.isImmediate()) {
            this.log.debug("Scheduled immediate \033[4m{}\033[0m", event.getClass().getSimpleName());
            Event first = this._queue.peekFirst();
            if (first != null && first.getID().equals(event.getID())) {
                this.log.debug("Event \033[4m{}\033[0m is already scheduled", event.getClass().getSimpleName());
                return;
            }
            this._queue.addFirst(event);
        } else {
            if (!event.isDebugEvent())
                this.log.debug("Added \033[4m{}\033[0m to queue with {} events before it",
                        event.getClass().getSimpleName(),
                        this._queue.size());

            Event last = this._queue.peekLast();
            if (last != null && last.getID().equals(event.getID())) {
                this.log.debug("Event \033[4m{}\033[0m is already scheduled", event.getClass().getSimpleName());
                return;
            }
            this._queue.addLast(event);
        }

        this.processEvent(event);
    }


    // muss von executorService entkoppelt werden
    private void scheduleProcessing() {
        if (this.executorService.isShutdown()) {
            this.log.warn("Attempted to schedule event processing after shutdown");
            return;
        }

        this.executorService.execute(() -> {
            if (this._queue.isEmpty())
                return;

            var event = this._queue.pollFirst();
            if (event == null)
                return;

            this.processEvent(event);
            if (!this._queue.isEmpty())
                this.scheduleProcessing();
        });
    }


    // Semaphore entfernt
    public  <E extends Event> void processEvent(E event) {
        var maxtime = new Date(System.currentTimeMillis() - 10 * 1000);
        if (event.getTime().before(maxtime)) {
            return;
        }

            if (!event.isDebugEvent()) {
                this.log.debug("Processing event \033[4m{}\033[0m {}", event.getClass().getSimpleName(), event.getID());
            }
            var handlers = this.getEventHandlers(event.getClass());
            if (handlers != null) {
                for (var handler : handlers) {
                    try {
                        handler.accept(event);
                    } catch (Exception e) {
                        this.log.error(e);
                    }
                }
            }


    }

    private <E extends Event> List<Consumer<Event>> getEventHandlers(Class<E> eventClass) {
        return this.eventHandlers.keySet().stream()
                .filter(c -> c.equals(eventClass) || c.isAssignableFrom(eventClass))
                .flatMap(c -> this.eventHandlers.get(c).stream())
                .toList();
    }

    public IEventQueue getQueue() {
        return queue;
    }

    public void terminate() {

        this._queue.clear();
        //this.executorService.shutdown();
        //this.executorService.awaitTermination(5000, TimeUnit.MILLISECONDS);
        //} catch (InterruptedException e) {
        //  this.log.error("Failed to terminate event manager");
        // }

    }
}

    class Once<E extends Event> implements Consumer<E> {
        private final Consumer<E> handler;
        private final List<Consumer<Event>> handlers;

        public Once(Consumer<E> handler, List<Consumer<Event>> handlers) {
            this.handler = handler;
            this.handlers = handlers;
        }

        @Override
        public void accept(E t) {
            this.handler.accept(t);
            this.handlers.remove(this);
        }
    }

