package com.pabloncf.saas.auth.domain;

public enum Role {
    VIEWER, MEMBER, ADMIN, OWNER;

    /** Returns true if this role has at least the permissions of {@code required}. */
    public boolean includes(Role required) {
        return this.ordinal() >= required.ordinal();
    }
}
