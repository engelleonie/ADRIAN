package tech.jorn.adrian.experiment.scenarios;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.messages.Envelope;

import java.util.List;
import java.util.Queue;

public class MixedScenario extends Scenario {

    Logger log = LogManager.getLogger(MixedScenario.class);
    public MixedScenario(Infrastructure infrastructure, EventDispatcher<Envelope> messageDispatcher) {
        super(infrastructure, messageDispatcher, 10 * 60 * 1000);
    }

    @Override
    public void onScheduleEvents(Queue<ExperimentalAgent> agents) {
        this.after(3 * 60 * 1000, () -> {
            this.log.debug("Waiting for silence");
            this.waitForSilence(1000, this.finished::raise);
        });

    }
}
