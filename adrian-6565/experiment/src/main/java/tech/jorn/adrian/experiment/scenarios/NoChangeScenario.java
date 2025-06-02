package tech.jorn.adrian.experiment.scenarios;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.messages.Envelope;

import java.util.List;
import java.util.Queue;
import tech.jorn.adrian.experiment.MetricCollector;

public class NoChangeScenario extends Scenario {
    Logger log = LogManager.getLogger(NoChangeScenario.class);



    public NoChangeScenario(Infrastructure infrastructure, EventDispatcher<Envelope> messageDispatcher) {
        super(infrastructure, messageDispatcher, 10 * 60 * 1000 /* Twenty Minutes */);
    }

    @Override
    public void onScheduleEvents(Queue<ExperimentalAgent> agents) {
        this.after(5 * 60 * 1000, () -> {
            this.log.debug("Waiting for silence");
            this.waitForSilence(30 * 1000, this.finished::raise);
        });
    }
}
