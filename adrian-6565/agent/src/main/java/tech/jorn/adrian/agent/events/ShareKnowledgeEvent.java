package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.infrastructure.SoftwareAsset;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBaseSoftwareAsset;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeOrigin;
import tech.jorn.adrian.core.properties.NodeProperty;

import java.util.List;

public class ShareKnowledgeEvent extends Event  {

    private final AbstractDetailedNode<NodeProperty<?>>  origin;
    private final KnowledgeBase knowledgeBase;
    private final int distance;

    public ShareKnowledgeEvent(AbstractDetailedNode<NodeProperty<?>> origin, KnowledgeBase knowledgeBase, int distance) {
        this.origin = origin;
        this.knowledgeBase = knowledgeBase;
        this.distance = distance;
    }

    public AbstractDetailedNode<NodeProperty<?>>  getOrigin() {
        return origin;
    }

    public KnowledgeBase getKnowledgeBase() {
        return knowledgeBase;
    }

    public int getDistance() {
        return distance;
    }

    public static ShareKnowledgeEvent reducedDistance(ShareKnowledgeEvent event) {
        return new ShareKnowledgeEvent(event.getOrigin(), event.getKnowledgeBase(), event.getDistance() - 1);
    }


}


