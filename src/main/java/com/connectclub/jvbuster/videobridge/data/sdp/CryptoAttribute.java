package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CryptoAttribute extends Attribute {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static public class KeyParam {
        private String method;
        private String info;
    }

    private Integer tag;
    private String suite;
    private ArrayList<KeyParam> keyParams;
    private String sessionParams;


    @Override
    public String getField() {
        return "crypto";
    }

    @Override
    public String getValue() {
        StringBuilder stringBuilder = new StringBuilder();

        boolean first = true;
        stringBuilder.append(tag).append(" ").append(suite).append(" ");
        for (KeyParam param : keyParams) {
            if (first)
                first = false;
            else
                stringBuilder.append(";");
            stringBuilder.append(param.method).append(":").append(param.info);

        }
        if (sessionParams != null)
            stringBuilder.append(" ").append(sessionParams);
        return stringBuilder.toString();
    }

}
