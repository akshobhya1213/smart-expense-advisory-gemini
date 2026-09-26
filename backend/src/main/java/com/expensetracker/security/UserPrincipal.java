package com.expensetracker.security;

/**
 * Lightweight principal placed into the SecurityContext after JWT validation.
 * Every service call for user-owned data (expenses, budgets) must filter by this userId —
 * that's the core of our per-user data isolation guarantee.
 */
public record UserPrincipal(Long userId, String email) {}
