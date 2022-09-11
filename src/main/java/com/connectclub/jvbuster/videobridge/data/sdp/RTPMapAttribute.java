package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RTPMapAttribute extends Attribute {

    private Integer format;
    private String name;
    private Integer rate;
    private String parameters;
    
    @Override
    public String getField() {
        return "rtpmap";
    }

    @Override
    public String getValue() {
        return format.toString() + " " + name + "/" + rate + (parameters!=null ? "/" + parameters : "" );
    }

}
