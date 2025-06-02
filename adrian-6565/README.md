# ADRIAN: Automated decentralized management of cybersecurity risks

This is the repository for the ADRIAN project, containing the source code and results of the research project.

## Authors

- XXXX-1 (1)
- Jorn J. Verhoeven (2)
- XXXX-3 (2)

(1) XXXX-4
\
(2) XXXX-5


## Risk Rules and Adaptation Patterns

The agents utilize predefined rules to generate an attack graph from the knowledge base, determining when to create risk edges between vertices and their associated probabilities. These rules consider various conditions, such as the attributes and existence of nodes, software components, and links within the knowledge base. Below is a detailed list of all risk rules used in the experiments.

For more information on how the risk rules are used, please refer [this short description](https://anonymous.4open.science/r/adrian-6565/risk-rules.pdf).
The adaptation patterns for mitigating risks can be found [here](https://anonymous.4open.science/r/adrian-6565/adaptation-patterns.pdf).

The table below provides an overview of the implemented risk rules, as well as their exploitation probability, the manner in which they can be mitigated through adaptations, and the resulting changed exploitation probability after executing the adaptation.

| Name | Rule Type | Exploitation Probability | Adaptation | Mitigation Probability
| ---- | --------- | ---- | ---- | --- |
| CVE-2020-3676 | Forward | 0.8 | Enable Property | 0.2
| CVE-2021-22547 | Forward | 0.18 | Version Change | 0.0
| CVE-2021-40830 | Inward | 0.18 | Version Change | 0.0
| CVE-2022-25666 | Forward | 0.28 | Version Change | 0.0
| CVE-2022-359274(1) | Backward | 0.39 | Version Change | 0.0
| CVE-2022-359274(2) | Inward | 0.39 | Version Change | 0.0
| Firmware Risk* | Forward | 0.1 | N.A. |
| OS Risk* | Forward | 0.4 | Version Change | 0.0
| SDK Risk* | Forward | 0.99 | Version Change | 0.0
| Uncertainty* | Inward | 0.08 | N.A. |
| Firewall* | Backward | 0.8 | Enable Property | 0.2
| Physically Secured* | Backward | 0.8 | Enable Property | 0.2
| Software Encrypted* | Backward | 0.8 | Enable Property | 0.2

* Risk rules marked with * were used for testing and are not based on real-world vulnerabilities (for example, using CVEs). 

## Metrics

The following metrics are collected during the experiments:

**Effectiveness metrics**
- Number of unique risks identified
- Number of remaining risks
- Total expected damage

**Efficiency metrics**
- Number of messages exchanged
- Number of adaptations
- Cumulative time spent auctioning
- Cumulative time spent adapting

## Results

### Scenario 1
In this scenario, no external changes are made to the infrastructure. The purpose of this scenario is to see how the system behaves when no changes are made.

![damage](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-1/damage.png)

_Fig. 1: Overall damage of the system in the scenario where no changes are made over time._

![messages](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-1/messages.png)

_Fig. 2: Total number of messages sent between agents in the scenario where no changes are made over time._

![adaptations](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-1/adaptations.png)

_Fig. 3: Total number of adaptations applied by agents in the scenario where no changes are made over time._

![unique-risks](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-1/unique-risks.png)

_Fig. 4: Number of unique risks detected by agents in the scenario where no changes are made over time._

![remaining-risks](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-1/remaining-risks.png)

_Fig. 5: Number of remaining risks in the system in the scenario where no changes are made over time._

![auctioning-time](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-1/auctioning-time.png)

_Fig. 6: Total time spent auctioning by agents in the scenario where no changes are made over time._

![adapting-time](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-1/adapting-time.png)

_Fig. 7: Total time spent adapting by agents in the scenario where no changes are made over time._

### Scenario 2
This scenario introduces a risk to the infrastructure after 120 seconds. The purpose of this scenario is to see how the system behaves when a new risk is introduced.

![damage](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-2/damage.png)

_Fig. 8: Overall damage of the system in the risk introduction scenario. The damage is shown for each of the three ADRIAN variants. The vertical line indicates the time at which a risk is introduced._

![messages](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-2/messages.png)

_Fig. 9: Total number of messages sent between agents in the risk introduction scenario._

![adaptations](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-2/adaptations.png)

_Fig. 10: Total number of adaptations applied by agents in the risk introduction scenario._

![unique-risks](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-2/unique-risks.png)

_Fig. 11: Number of unique risks detected by agents in the risk introduction scenario._

![remaining-risks](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-2/remaining-risks.png)

_Fig. 12: Number of remaining risks in the system in the risk introduction scenario._

![auctioning-time](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-2/auctioning-time.png)

_Fig. 13: Total time spent auctioning by agents in the risk introduction scenario._

![adapting-time](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-2/adapting-time.png)

_Fig. 14: Total time spent adapting by agents in the risk introduction scenario._

### Scenario 3
This scenario introduces a new infrastructure node every 30 seconds. The purpose of this scenario is to see how the system behaves when the infrastructure is growing.

![damage](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-3/damage.png)

_Fig. 15: Overall damage of the system in the infrastructure growth scenario. The damage is shown for each of the three variants. The vertical lines indicate the times at which a new node is introduced._

![messages](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-3/messages.png)

_Fig. 16: Total amount of messages sent between agents in the infrastructure growth scenario._

![adaptations](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-3/adaptations.png)

_Fig. 17: Total number of adaptations applied by agents in the infrastructure growth scenario._

![unique-risks](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-3/unique-risks.png)

_Fig. 18: Number of unique risks detected by agents in the infrastructure growth scenario._

![remaining-risks](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-3/remaining-risks.png)

_Fig. 19: Number of remaining risks in the system in the infrastructure growth scenario._

![auctioning-time](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-3/auctioning-time.png)

_Fig. 20: Total time spent auctioning by agents in the infrastructure growth scenario._

![adapting-time](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-3/adapting-time.png)

_Fig. 21: Total time spent adapting by agents in the infrastructure growth scenario._

### Scenario 4
This scenario removes an existing infrastructure node after 30 seconds, and adds the node back into the infrastructure after another 30 seconds. This is repeated twice. The purpose of this scenario is to see how the system behaves when the infrastructure (connection) is unstable.

![damage](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-4/damage.png)

_Fig. 22: Overall damage in the system in the unstable infrastructure scenario. The damage is shown for each of the three variants. The vertical lines indicate the times at which a node is removed or added back._

![messages](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-4/messages.png)

_Fig. 23: Total number of messages sent between agents in the unstable infrastructure scenario._

![adaptations](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-4/adaptations.png)

_Fig. 24: Total number of adaptations applied by agents in the unstable infrastructure scenario._

![unique-risks](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-4/unique-risks.png)

_Fig. 25: Number of unique risks detected by agents in the unstable infrastructure scenario._

![remaining-risks](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-4/remaining-risks.png)

_Fig. 26: Number of remaining risks in the system in the unstable infrastructure scenario._

![auctioning-time](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-4/auctioning-time.png)

_Fig. 27: Total time spent auctioning by agents in the unstable infrastructure scenario._

![adapting-time](https://anonymous.4open.science/r/adrian-6565/graphs/scenario-4/adapting-time.png)

_Fig. 28: Total time spent adapting by agents in the unstable infrastructure scenario._

### Miscellaneous
![small-infrastructures](https://anonymous.4open.science/r/adrian-6565/graphs/small-infra.png)

_Fig. 29: Overall damage on a small infrastructure containing 4 nodes. This shows the overhead that comes with the time spent on auctioning for smaller infrastructures._

![consistency](graphs/consistency.png)

_Fig. 30: Multiple runs using the same scenario. The purpose of this graph is to show the consistency of the results._
