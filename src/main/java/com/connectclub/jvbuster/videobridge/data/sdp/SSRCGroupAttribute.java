package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SSRCGroupAttribute extends Attribute {

    private String semantics;
    private List<Long> SSRCIds = new ArrayList<>();

    @Override
    public String getField() {
        return "ssrc-group";
    }

    @Override
    public String getValue() {
        String value = semantics;
        for (Long id : SSRCIds)
            value += " " + id;
        return value;
    }

}
