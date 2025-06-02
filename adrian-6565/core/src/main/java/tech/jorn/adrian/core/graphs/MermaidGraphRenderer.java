package tech.jorn.adrian.core.graphs;

import tech.jorn.adrian.core.graphs.base.AbstractGraph;
import tech.jorn.adrian.core.graphs.base.GraphLink;
import tech.jorn.adrian.core.graphs.base.WeightedLink;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphLink;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MermaidGraphRenderer<N extends AbstractDetailedNode<?>, L extends GraphLink<N>> {
    public String render(AbstractGraph<N, L> graph) {
        return this.render(graph, null);
    }

    public String render(AbstractGraph<N, L> graph, String title) {
        var output = new ArrayList<String>();
        if (title != null) output.add("%% " + title);
        output.add("flowchart LR");

        graph.getNodes().forEach(node -> {
            output.add(this.renderNodeInfo(node));
        });

        graph.getNodes().forEach(node -> {
            var links = graph.getLinks(node);
            if (links == null) return;
            links.forEach(link -> {
                output.add(this.renderGraphLink(node, link));
            });
        });

        return output.stream().collect(Collectors.joining("\n"));
    }

    private String renderNodeInfo(N node) {
        return String.format("\t%s[%s]", node.getID(), node.getID()); // TODO: Maybe use a pretty name
    }

    private String renderGraphLink(N from, L link) {
        if (link instanceof WeightedLink<?>) {
            var text = ((WeightedLink<?>) link).getWeight();
            return String.format("\t%s -->|%.2f| %s", from.getID(), text, link.getNode().getID());
        } else if (link instanceof AttackGraphLink<?>) {
            var risk = ((AttackGraphLink<?>) link).getRisk();
            var text = String.format("%s%s %.2f", risk.isMitigated() ? "" : "!", risk.type(), risk.factor());
            return String.format("\t%s -->|%s| %s", from.getID(), text, link.getNode().getID());
        }
        return String.format("\t%s --> %s", from.getID(), link.getNode().getID());
    }

    public void toFile(String filename, AbstractGraph<N, L> graph, String meta) {
        try {
            var writer = new FileWriter(filename);
            var mmdGraph = this.render(graph);
            writer.write("%% " + meta + "\n");
            writer.write(mmdGraph);
            writer.close();
        } catch (IOException e) {
            System.err.println("SOMETHING WENT WRONG OUTPUTTING GRAPH " + e.toString());
            throw new RuntimeException(e);
        }
    }

    public static MermaidGraphRenderer<AttackGraphEntry<?>, AttackGraphLink<AttackGraphEntry<?>>> forAttackGraph() {
        return new MermaidGraphRenderer<>();
    }

    public static MermaidGraphRenderer<KnowledgeBaseEntry<?>, GraphLink<KnowledgeBaseEntry<?>>> forKnowledgeBase() {
        return new MermaidGraphRenderer<>();
    }
}
