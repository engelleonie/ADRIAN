package tech.jorn.adrian.experiment.scenarios;
import tech.jorn.adrian.experiment.ExperimentRunner;

public class Runner {
    public static void main(String[] args) throws InterruptedException {
        String[] files = {"testgraph50.yml", "testgraph100.yml", "testgraph250.yml", "testgraph500.yml", "testgraph1000.yml", "testgraph5000.yml" };
        String[] features = {"knowledge-sharing", "full", "auctioning", "local"};
        String[] scenarios = {"large", "risk-introduction", "growing", "unstable", "mixed", "no-change"};

        for(int i = 0; i<=files.length; i++) {
            for(int j = 0; j<=features.length; j++) {
                for(int k = 0; k<= scenarios.length; k++) {
                    String[] file = {files[i], features[j], scenarios[k]};

                }
            }
        }
    }
}
