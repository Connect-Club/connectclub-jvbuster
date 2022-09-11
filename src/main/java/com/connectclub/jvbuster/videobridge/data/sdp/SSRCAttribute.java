package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SSRCAttribute extends Attribute {

    private Long SSRC;
    private String attrField;
    private String attrValue;

    public static final String FIELD = "ssrc";

    @Override
    public String getField() {
        return FIELD;
    }

    @Override
    public String getValue() {
        return SSRC + " " + attrField + (attrValue != null ? ":" + attrValue : "");
    }

}
