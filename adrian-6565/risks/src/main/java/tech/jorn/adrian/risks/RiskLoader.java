package tech.jorn.adrian.risks;

import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.risks.AttackGraphSoftwareAsset;
import tech.jorn.adrian.core.mutations.AttributeChange;
import tech.jorn.adrian.core.mutations.Mutation;
import tech.jorn.adrian.core.properties.NodeProperty;
import tech.jorn.adrian.core.risks.RiskRule;
import tech.jorn.adrian.risks.rules.BackwardRisk;
import tech.jorn.adrian.risks.rules.ForwardRisk;
import tech.jorn.adrian.risks.rules.InwardRisk;
import tech.jorn.adrian.core.risks.validators.BeforeVersion;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

public class RiskLoader {
    public static List<RiskRule> listRisks() {
        return List.of(
//                new SdkVulnerabilityCve("CVE-2021-22547", "google/cloud_iot_device_sdk_for_embedded_c", new BeforeVersion("1.0.3"), 1.8f, "1.0.3"), // https://nvd.nist.gov/vuln/detail/CVE-2021-22547
//                new FirmwareVulnerabilityCve("CVE-2022-25666", "qualcomm/apq8096", new BeforeVersion("1.0.9"), 0.8f, "2.1"), // https://nvd.nist.gov/vuln/detail/CVE-2022-25666
//                new FirmwareVulnerabilityCve("CVE-2020-3676", "qualcomm/apq8096", new BeforeVersion("1.0.9"), 1.8f, "2.1")  // https://nvd.nist.gov/vuln/detail/CVE-2020-3676
//                        .fromSoftwareToNode(),
//                new OsVulnerabilityCve("CVE-2022-35927", "contiki-ng/contiki-ng", new BeforeVersion("4.7"), 3.9f, "4.7"), // https://nvd.nist.gov/vuln/detail/CVE-2022-35927
//
//                new EntryCve("CVE-2022-35927", "os-contiki-ng/contiki-ng-version", new BeforeVersion("4.7"), 3.9f, 500, 10000, "4.7")
//                        .includeNodes(),
//                new EntryCve("CVE-2021-40830", "sdk-amazon/amazon_web_services_iot_device_sdk_v2-version", new BeforeVersion("1.5"), 2.8f, 500, 5000, "1.5")
//                        .includeSoftware(),
//
//                new OsVulnerabilityCve("CVE-fake", "fake/os", new BeforeVersion("4.9"), 8.0f, "5.1"),
//                new EntryCve("CVE-fake", "os-fake/os-version", new BeforeVersion("4.9"), 8.0f, 500, 10000, "5.1")
//                        .includeNodes()
//                new RuleFakeVulnerability()

                // Always add a little uncertainty into the graph
                new InwardRisk("uncertainty-entry", null, 0.08f),

                new BackwardRisk("firewall-risk", "hasFirewall", 0.8f)
                        .mitigatedRisk(0.2f)
                        .target(true, false)
                        .withAdaptation(RiskLoader.enableProperty("hasFirewall", 2000)),
                new BackwardRisk("physically-secured-risk", "isPhysicallySecured", 0.8f)
                        .mitigatedRisk(0.2f)
                        .target(true, false)
                        .withAdaptation(RiskLoader.enableProperty("isPhysicallySecured", 2000)),
                new BackwardRisk("software-encrypted-risk", "softwareComponentIsEncrypted", 0.8f)
                        .mitigatedRisk(0.2f)
                        .target(true, false)
                        .withAdaptation(RiskLoader.enableProperty("softwareComponentIsEncrypted", 2000)),

                new ForwardRisk("firmware-risk", "fake-firmware", 0.1f)
                        .mitigationTest(new BeforeVersion("2.1"))
                        .target(false, true),
                new ForwardRisk("os-risk", "fake-os", 0.4f)
                        .mitigationTest(new BeforeVersion("2.1"))
                        .target(true, true)
                        .withAdaptation(RiskLoader.upgradeVersion("fake-os", "3.1", 10000)),
                new ForwardRisk("sdk-risk", "fake-sdk", 0.99f)
                        .mitigationTest(new BeforeVersion("2.1"))
                        .excludeNodes()
                        .includeSoftware()
                        .target(false, true)
                        .withAdaptation(RiskLoader.upgradeVersion("fake-sdk", "2.2", 5000)),

//                new ForwardRisk("fake-vulnerability", "fake-vulnerability", 0.99f)
//                        .target(true, true)
//                        .withAdaptation(RiskLoader.enableProperty("fake-vulnerability", 5000)),

                // new SdkVulnerabilityCve("CVE-2021-22547", "google/cloud_iot_device_sdk_for_embedded_c", new BeforeVersion("1.0.3"), 1.8f, "1.0.3"), // https://nvd.nist.gov/vuln/detail/CVE-2021-22547
                new ForwardRisk("CVE-2021-22547", "google/cloud_iot_device_sdk_for_embedded_c", 0.18f)
                        .mitigationTest(new BeforeVersion("1.0.3"))
                        .target(false, true)
                        .withAdaptation(RiskLoader.upgradeVersion("google/cloud_iot_device_sdk_for_embedded_c", "1.0.3", 5000)),

                // new FirmwareVulnerabilityCve("CVE-2022-25666", "qualcomm/apq8096", new BeforeVersion("1.0.9"), 0.8f, "2.1"), // https://nvd.nist.gov/vuln/detail/CVE-2022-25666
                new ForwardRisk("CVE-2022-25666", "qualcomm/apq8096", 0.08f)
                        .mitigationTest(new BeforeVersion("1.0.9"))
                        .target(false, true)
                        .withAdaptation(RiskLoader.upgradeVersion("qualcom/apq8096", "2.1", 10000)),
                // new FirmwareVulnerabilityCve("CVE-2020-3676", "qualcomm/apq8096", new BeforeVersion("1.0.9"), 1.8f, "2.1")  // https://nvd.nist.gov/vuln/detail/CVE-2020-3676
                new ForwardRisk("CVE-2020-3676", "qualcom/apq8096", 0.18f)
                        .mitigationTest(new BeforeVersion("1.0.9"))
                        .includeSoftware()
                        .excludeNodes()
                        .target(true, false)
                        .withAdaptation(RiskLoader.upgradeVersion("qualcom/apq8096", "2.1", 10000)),

                // new OsVulnerabilityCve("CVE-2022-35927", "contiki-ng/contiki-ng", new BeforeVersion("4.7"), 3.9f, "4.7"), // https://nvd.nist.gov/vuln/detail/CVE-2022-35927
                new BackwardRisk("CVE-2022-35927", "contiki-ng/contiki-ng", 0.39f)
                        .mitigationTest(new BeforeVersion("4.7"))
                        .target(true, true)
                        .withAdaptation(RiskLoader.upgradeVersion("contiki-ng/contiki-ng", "4.7", 10000)),
                // new EntryCve("CVE-2022-35927", "os-contiki-ng/contiki-ng-version", new BeforeVersion("4.7"), 3.9f, 500, 10000, "4.7")
                new InwardRisk("CVE-2022-35927", "contiki-ng/contiki-ng", 0.39f)
                        .mitigationTest(new BeforeVersion("4.7"))
                        .withAdaptation(RiskLoader.upgradeVersion("contiki-ng/contiki-ng", "4.7", 10000)),
                // new EntryCve("CVE-2021-40830", "sdk-amazon/amazon_web_services_iot_device_sdk_v2-version", new BeforeVersion("1.5"), 2.8f, 500, 5000, "1.5")
                new InwardRisk("CVE-2021-40830", "amazon/amazon_webservices_iot_device_sdk_v2", 0.28f)
                        .mitigationTest(new BeforeVersion("1.5"))
                        .withAdaptation(RiskLoader.upgradeVersion("amazon/amazon_webservices_iot_device_sdk_v2", "1.5", 10000))
        );
    }

    private static BiFunction<AbstractDetailedNode, RiskRule, Optional<? extends Mutation>> enableProperty(String property, float time) {
        return (AbstractDetailedNode node, RiskRule risk) -> {
            if (node instanceof AttackGraphSoftwareAsset) return Optional.empty();

            var mutation = new AttributeChange<>(node, new NodeProperty<>(property, true), 100, time, risk);
            return Optional.of(mutation);
        };
    }

    private static BiFunction<AbstractDetailedNode, RiskRule, Optional<? extends Mutation>> upgradeVersion(String property, String newVersion, float time) {
        return (AbstractDetailedNode node, RiskRule risk) -> {
            if (node instanceof AttackGraphSoftwareAsset) return Optional.empty();

            var mutation = new AttributeChange<>(node, new NodeProperty<>(property, newVersion), 100, time, risk);
            return Optional.of(mutation);

//                var mutation = new SoftwareAttributeChange<>(node, new SoftwareProperty<>(property, newVersion), 100, time, risk);
//                return Optional.of(mutation);
        };
    }
}
