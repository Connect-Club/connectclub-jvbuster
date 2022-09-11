package com.connectclub.jvbuster.videobridge.data.sdp;

public abstract class Attribute {
    public abstract String getField();

    public abstract String getValue();

    public StringBuilder append(StringBuilder stringBuilder) {
        stringBuilder.append("a=").append(getField());
        String value = getValue();
        if (value != null) {
            stringBuilder.append(":").append(value);
        }
        return stringBuilder.append("\r\n");
    }
}
