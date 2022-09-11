package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaseAttribute extends Attribute {
    private String field;
    private String value;

    public BaseAttribute(String field) {
        this.field = field;
    }

}
