package org.humanbrainproject.knowledgegraph.commons.authorization.entity;

public class AccessRight {

    public enum Permission{
        READ, WRITE;
    }

    private final String path;

    private final Permission permission;

    public AccessRight(String path, Permission permission) {
        this.path = path;
        this.permission = permission;
    }

    public String getPath() {
        return path;
    }

    public boolean canWrite(){
        return permission == Permission.WRITE;
    }

    public boolean isReadOnly(){
        return permission == Permission.READ;
    }

}
