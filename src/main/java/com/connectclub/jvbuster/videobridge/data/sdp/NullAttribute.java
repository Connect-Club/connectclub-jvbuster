package com.connectclub.jvbuster.videobridge.data.sdp;

public class NullAttribute extends Attribute {

    public static final NullAttribute INSTANCE = new NullAttribute();

    private NullAttribute() {
    }

    @Override
    public String getField() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    public StringBuilder append(StringBuilder stringBuilder) {
        return stringBuilder;
    }
}
