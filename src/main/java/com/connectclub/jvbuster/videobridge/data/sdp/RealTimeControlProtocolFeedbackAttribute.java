package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;
import net.logstash.logback.encoder.org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeControlProtocolFeedbackAttribute extends Attribute {

    private Integer format;
    private String type;
    private String subtype;

    @Override
    public String getField() {
        return "rtcp-fb";
    }

    @Override
    public String getValue() {
        return format + " " + type + (StringUtils.isNoneBlank(subtype) ? " " + subtype : "");
    }
}
