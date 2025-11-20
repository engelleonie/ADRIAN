package tech.jorn.adrian.core.events;

import java.util.*;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tech.jorn.adrian.core.EventNode;
import tech.jorn.adrian.core.GlobalQueue;
import tech.jorn.adrian.core.agents.AgentState;
import tech.jorn.adrian.core.events.queue.IEventQueue;
import tech.jorn.adrian.core.observables.SubscribableValueEvent;



public class EventManager {
    protected final Logger log = LogManager.getLogger(EventManager.class);

    private final IEventQueue queue;
    private final SubscribableValueEvent<AgentState> agentState;
    private final Map<Class<Event>, List<Consumer<Event>>> eventHandlers = new HashMap<>();
    //private final ExecutorService executorService = Executors.newSingleThreadExecutor();
     //private final Deque<Event> _queue = new ArrayDeque<>();
    // private Semaphore processing = new Semaphore(1);

    private long simulatedTime = 0;
    private final Set<Event> scheduledEvents = Collections.synchronizedSet(new HashSet<>());

    private final GlobalQueue globalQueue = GlobalQueue.getInstance();



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

        /*if (!scheduledEvents.add(event)) {
            if (!event.isDebugEvent()) {
                this.log.debug("Event {} wurde nicht erneut eingeplant, weil es bereits existiert", event.getClass().getSimpleName());
            }
            return;
        } */

        if (!event.isDebugEvent()) {
            this.log.debug("Added \033[4m{}\033[0m to queue with {} events before it",
                    event.getClass().getSimpleName(),
                    GlobalQueue.getInstance().getSize());

            /* Event last = this._queue.peekLast();
            if (last != null && last.getID().equals(event.getID())) {
                this.log.debug("Event \033[4m{}\033[0m is already scheduled", event.getClass().getSimpleName());
                return;
            } */

        }
        GlobalQueue.getInstance().offer(event, eventDuration(event));

        //adding event to globalqueue
        //long finishTime = simulatedTime + eventDuration(event);
        //EventNode node = new EventNode(event, finishTime);
        //event.setFinishTime(finishTime);

        //this.processEvent(event);
    }

    public int eventDuration(Event event) {

        return event.getDuration();
    }

    public boolean canHandle(Event event) {

        return !getEventHandlers(event.getClass()).isEmpty();
    }

    public  <E extends Event> void processEvent(E event) {

        //var maxtime = new Date(System.currentTimeMillis() - 10 * 1000);
        //if (event.getTime().before(maxtime)) {
          //  return;
        //}

        scheduledEvents.remove(event);
            if (!event.isDebugEvent()) {
                this.log.debug("Processing event \033[4m{}\033[0m {}", event.getClass().getSimpleName(), event.getID());
            }
            var handlers = this.getEventHandlers(event.getClass());
            if (handlers != null) {
                for (var handler : handlers) {
                    try {
                        System.out.println(55);
                        handler.accept(event);
                        System.out.println(66);
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

        System.exit(0);
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

