package tech.jorn.adrian.agent.events;

import tech.jorn.adrian.core.agents.IAgent;
import tech.jorn.adrian.core.events.Event;
import tech.jorn.adrian.core.graphs.AbstractDetailedNode;
import tech.jorn.adrian.core.graphs.knowledgebase.KnowledgeBase;
import tech.jorn.adrian.core.properties.NodeProperty;

import java.util.Objects;

public class ShareKnowledgeEvent extends Event  {

    private final AbstractDetailedNode<NodeProperty<?>>  origin;
    private final KnowledgeBase knowledgeBase;
    private final int distance;
    private static int distanceShare = 0;

    public ShareKnowledgeEvent(AbstractDetailedNode<NodeProperty<?>> origin, KnowledgeBase knowledgeBase, int distance, IAgent agent) {
        super(agent);
        this.origin = origin;
        this.knowledgeBase = knowledgeBase;
        this.distance = distance;
    }
    @Override
    public int getDuration() {
        return 10;
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
        distanceShare++;
        System.out.println("distanceShare: " + distanceShare);
        return new ShareKnowledgeEvent(event.getOrigin(), event.getKnowledgeBase(), event.getDistance() - 1, event.getAgent());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShareKnowledgeEvent)) return false;
        ShareKnowledgeEvent that = (ShareKnowledgeEvent) o;
        return distance == that.distance &&
                origin.equals(that.origin) &&
                knowledgeBase.equals(that.knowledgeBase);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, knowledgeBase, distance);
    }



}


