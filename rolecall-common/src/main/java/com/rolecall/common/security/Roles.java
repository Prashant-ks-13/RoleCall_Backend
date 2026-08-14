package com.rolecall.common.security;

/**
 * Role name constants matching the JWT {@code role} claim issued by auth-service.
 * Plain strings only — no Spring Security dependency here by design.
 */
public final class Roles {

    public static final String CANDIDATE = "CANDIDATE";
    public static final String EMPLOYER = "EMPLOYER";
    public static final String ADMIN = "ADMIN";

    private Roles() {
    }
}
