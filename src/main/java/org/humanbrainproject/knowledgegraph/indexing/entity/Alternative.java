package org.humanbrainproject.knowledgegraph.indexing.entity;


import java.util.Set;

public class Alternative {

    private Object value;
    private Set<String> userIds;

    public Alternative(Object value,  Set<String> userIds) {
        this.value = value;
        this.userIds = userIds;
    }

    public Object getValue() {
        return value;
    }

    public Set<String> getUserIds() {
        return userIds;
    }
}
