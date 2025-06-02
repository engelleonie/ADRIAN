package tech.jorn.adrian.core.graphs.base;

import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseEntry;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeOrigin;
import tech.jorn.adrian.core.graphs.risks.AttackGraphEntry;
import tech.jorn.adrian.core.graphs.risks.AttackGraphNode;

public class VoidNode extends AbstractNode {
    public static final String ID = "VOID";

    public VoidNode() {
        super(VoidNode.ID);
    }

    public static AttackGraphEntry<?> getIncoming() {
        return new AttackGraphNode(VoidNode.ID);
    }
    public static KnowledgeBaseEntry<?> forKnowledge() {
        return new KnowledgeBaseNode(VoidNode.ID)
                .setKnowledgeOrigin(KnowledgeOrigin.DIRECT);
    }
}
