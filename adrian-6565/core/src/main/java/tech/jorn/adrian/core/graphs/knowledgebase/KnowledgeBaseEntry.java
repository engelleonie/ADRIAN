package tech.jorn.adrian.core.graphs.knowledgebase;

import org.apache.logging.log4j.LogManager;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.properties.AbstractProperty;

import java.util.Date;

public abstract class KnowledgeBaseEntry<P extends AbstractProperty<?>> extends AbstractDetailedNode<P> {
    private KnowledgeOrigin knowledgeOrigin;
    private Date updatedAt;

    public KnowledgeBaseEntry(String id) {
        super(id);
        this.updatedAt = new Date();
    }

    public KnowledgeOrigin getKnowledgeOrigin() {
        return knowledgeOrigin;
    }

    public KnowledgeBaseEntry<P> setKnowledgeOrigin(KnowledgeOrigin knowledgeOrigin) {
        if (this.knowledgeOrigin == null || knowledgeOrigin.compareTo(this.knowledgeOrigin) < 0) {
            this.knowledgeOrigin = knowledgeOrigin;
        }
        return this;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public KnowledgeBaseEntry<P> setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void learnFrom(KnowledgeBaseEntry<?> node) {
        // TODO: if knowledge is old, maybe not learn it?
        // if (node.updatedAt.before(this.updatedAt)) return;
        // TODO: Unlearn other knowledge???
        node.getProperties().forEach(this::setFromProperty);
    }
}

