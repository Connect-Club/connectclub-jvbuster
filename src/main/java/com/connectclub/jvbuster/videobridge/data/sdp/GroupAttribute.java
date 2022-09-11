package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupAttribute extends Attribute {

    private String semantics;

    @Singular
    private List<String> tags;

    public static final String FIELD = "group";

    @Override
    public String getField() {
        return FIELD;
    }

    @Override
    public String getValue() {
        StringBuilder value = new StringBuilder(semantics);
        for (String tag : tags)
            value.append(" ").append(tag);
        return value.toString();
    }

}