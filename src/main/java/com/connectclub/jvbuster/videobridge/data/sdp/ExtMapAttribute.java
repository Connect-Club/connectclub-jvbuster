package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExtMapAttribute extends Attribute {
    Integer id;
    String direction;
    String name;
    String attributes;


    @Override
    public String getField() {
        return "extmap";
    }

    @Override
    public String getValue() {
        return id + (direction != null ? "/" + direction : "") + " " + name + (attributes != null ? " " + attributes : "");
    }

}
