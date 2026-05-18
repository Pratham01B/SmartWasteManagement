package com.smartwaste.entity;

/**
 * Enum representing all user roles in the SmartWaste system.
 */
public enum Role {
    ADMIN,    // Full system access
    CITIZEN,  // Complaints + Rewards
    WORKER,   // Schedule + Routes
    RECYCLER  // Marketplace
}
