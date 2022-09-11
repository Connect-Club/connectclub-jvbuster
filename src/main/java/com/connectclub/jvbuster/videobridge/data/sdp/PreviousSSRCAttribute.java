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
public class PreviousSSRCAttribute extends Attribute {

    private List<String> SSRCs = new ArrayList<>();

    @Override
    public String getField() {
        return "previous-ssrc";
    }

    @Override
    public String getValue() {
        String value = null;
        for (String SSRC : SSRCs)
            if (value==null)
                value = SSRC;
            else
                value += " " + SSRC;
        return value;
    }

}