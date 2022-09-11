package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MsidAttribute extends Attribute {

    public static final String FIELD = "msid";

    private String streamId;

    private String trackId;

    @Override
    public String getField() {
        return FIELD;
    }

    @Override
    public String getValue() {
        return streamId + " " + trackId;
    }

    public MsidAttribute(MsidSemanticAttribute.MsidSemanticAttributeBuilder msidSemanticAttributeBuilder, String streamId, String trackId) {
        this(streamId, trackId);
        msidSemanticAttributeBuilder.stream(streamId);
    }
}
