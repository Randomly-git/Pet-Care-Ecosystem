// entity/RelatedType.java
package com.petcare.media.entity;

public enum   RelatedType {
    ACTIVITY("活动记录"),
    STATUS("状态记录"),
    MOMENT("动态"),
    PET_AVATAR("宠物头像");

    private final String description;

    RelatedType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}