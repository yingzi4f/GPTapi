package com.airepair.entity;

public enum Role {
    ADMIN("管理员"),
    TEAM_LEADER("团队组长"),
    USER("普通用户");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
