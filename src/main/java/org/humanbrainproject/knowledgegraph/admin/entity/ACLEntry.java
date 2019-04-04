package org.humanbrainproject.knowledgegraph.admin.entity;

import java.util.Arrays;
import java.util.List;

public class ACLEntry {

    public enum Type{
        GROUP, SUBJECT, TYPE;
    }

    private final Type type;
    private final String value;
    private final String realm;
    private final List<String> permissions;

    public ACLEntry(Type type, String value, String realm, String... permissions) {
        this.type = type;
        this.value = value;
        this.realm = realm;
        this.permissions = Arrays.asList(permissions);
    }

    public Type getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getRealm() {
        return realm;
    }

    public List<String> getPermissions() {
        return permissions;
    }
}
