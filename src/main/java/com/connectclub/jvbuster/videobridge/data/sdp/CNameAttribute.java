package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CNameAttribute extends Attribute {

    private String cname;

    @Override
    public String getField() {
        return "cname";
    }

    @Override
    public String getValue() {
        return cname;
    }

}
