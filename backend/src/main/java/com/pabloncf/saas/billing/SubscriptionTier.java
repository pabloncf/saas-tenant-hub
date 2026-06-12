package com.pabloncf.saas.billing;

public enum SubscriptionTier {

    FREE(3, 5),
    PRO(50, 25),
    ENTERPRISE(Integer.MAX_VALUE, Integer.MAX_VALUE);

    private final int maxProjects;
    private final int maxMembers;

    SubscriptionTier(int maxProjects, int maxMembers) {
        this.maxProjects = maxProjects;
        this.maxMembers  = maxMembers;
    }

    public int getMaxProjects() { return maxProjects; }
    public int getMaxMembers()  { return maxMembers;  }

    public boolean allowsMoreProjects(long current) {
        return current < maxProjects;
    }
}
