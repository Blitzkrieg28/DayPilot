package com.daypilot.domain.enums;

/**
 * Lifecycle states of an {@link com.daypilot.domain.entity.Approval} request.
 *
 * <p>The approval gateway creates requests in {@code PENDING} state.
 * The user (or a timeout) transitions them to a terminal state.</p>
 */
public enum ApprovalStatus {

    /** Awaiting the user's decision. */
    PENDING,

    /** User approved the proposed side-effect. */
    APPROVED,

    /** User rejected the proposed side-effect. */
    REJECTED,

    /** The approval window timed out without a decision. */
    EXPIRED
}
