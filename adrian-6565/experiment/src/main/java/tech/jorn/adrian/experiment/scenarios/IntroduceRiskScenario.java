package tech.jorn.adrian.experiment.scenarios;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tech.jorn.adrian.core.graphs.infrastructure.Infrastructure;
import tech.jorn.adrian.core.observables.EventDispatcher;
import tech.jorn.adrian.experiment.instruments.ExperimentalAgent;
import tech.jorn.adrian.experiment.messages.Envelope;

import java.util.List;
import java.util.Queue;

public class IntroduceRiskScenario extends Scenario {
    Logger log = LogManager.getLogger(IntroduceRiskScenario.class);

    public IntroduceRiskScenario(Infrastructure infrastructure, EventDispatcher<Envelope> messageDispatcher) {
        super(infrastructure, messageDispatcher, 20 * 60 * 1000 /* Twenty Minutes */);
    }

    @Override
    public void onScheduleEvents(Queue<ExperimentalAgent> agents) {
        this.after(2 * 60 * 1000 - 2000, () -> {
//            var asset = infrastructure.listSoftwareAssets().get(1);
//            this.log.info("Introducing Risk to asset {}", asset.getID());
//            asset.setProperty("os-fake/os-version", "1.0.1");

            for (var i = 0; i < infrastructure.listNodes().size(); i++) {
                var node = infrastructure.listNodes().get(i);

                if (!(boolean) node.getProperty("isExposed").orElse(false)) continue;

                this.log.info("Introducing Risk to node {}", node.getID());
                node.setProperty("fake-sdk", "1.0.1");
            }

        });
        this.after(3 * 60 * 1000, () -> {
            this.log.debug("Waiting for silence");
            this.waitForSilence( 30 * 1000, this.finished::raise);
        });
    }
}
