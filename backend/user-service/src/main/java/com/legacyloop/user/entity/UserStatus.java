package com.legacyloop.user.entity;

/** The account lifecycle. Only ACTIVE accounts may sign in. */
public enum UserStatus {

    PENDING_VERIFICATION("Verify your email address to continue"),
    PENDING_APPROVAL("An administrator has to approve this account before you can sign in"),
    ACTIVE(null),
    SUSPENDED("This account has been suspended");

    private final String rejectionMessage;

    UserStatus(String rejectionMessage) {
        this.rejectionMessage = rejectionMessage;
    }

    public boolean canSignIn() {
        return this == ACTIVE;
    }

    public String rejectionMessage() {
        return rejectionMessage;
    }

    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase().replace('_', ' ');
    }
}
