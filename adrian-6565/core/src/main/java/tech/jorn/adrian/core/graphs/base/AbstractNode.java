package tech.jorn.adrian.core.graphs.base;

import java.util.Objects;

public abstract class AbstractNode implements INode {
    private final String id;

    public AbstractNode(String id) {
        this.id = id;
    }

    @Override
    public String getID() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractNode that = (AbstractNode) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
