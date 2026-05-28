package com.Morph.logisticspipes.pipes.basic;

import net.minecraft.util.StringRepresentable;

public enum PipeType implements StringRepresentable {
    BASIC("basic"),
    PROVIDER("provider"),
    REQUEST("request"),
    SUPPLIER("supplier"),
    CHASSIS("chassis");

    private final String name;

    PipeType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
