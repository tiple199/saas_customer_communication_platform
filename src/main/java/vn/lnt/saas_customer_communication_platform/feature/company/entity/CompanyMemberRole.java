package vn.lnt.saas_customer_communication_platform.feature.company.entity;

public enum CompanyMemberRole {
    OWNER(3),
    ADMIN(2),
    MEMBER(1);

    private final int priority;

    CompanyMemberRole(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
