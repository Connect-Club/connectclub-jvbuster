package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MidAttribute extends Attribute {

    private String identificationTag;

    public static final String FIELD = "mid";

    @Override
    public String getField() {
        return FIELD;
    }

    @Override
    public String getValue() {
        return identificationTag;
    }

    public MidAttribute(GroupAttribute.GroupAttributeBuilder groupAttributeBuilder, String identificationTag) {
        this(identificationTag);
        groupAttributeBuilder.tag(identificationTag);
    }

}
