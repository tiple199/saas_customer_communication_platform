package vn.lnt.saas_customer_communication_platform.feature.conversation.dto;

import vn.lnt.saas_customer_communication_platform.feature.conversation.entity.Visitor;

public class VisitorResolutionResult {

    private final Visitor visitor;
    private final boolean newVisitor;

    public VisitorResolutionResult(Visitor visitor, boolean newVisitor) {
        this.visitor = visitor;
        this.newVisitor = newVisitor;
    }

    public Visitor getVisitor() {
        return visitor;
    }

    public boolean isNewVisitor() {
        return newVisitor;
    }
}

