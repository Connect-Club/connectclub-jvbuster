package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormatAttribute extends Attribute {

    private Integer format;
    @Builder.Default
    private Map<String, String> parameters = new LinkedHashMap<>();

    @Override
    public String getField() {
        return "fmtp";
    }

    @Override
    public String getValue() {
        StringBuilder string = new StringBuilder().append(format);
        //For each parameter
        boolean first = true;
        for (Map.Entry<String, String> entry : parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList())
        ) {
            //Add separator
            if (first) {
                first = false;
                string.append(" ").append(entry.getKey());
            } else {
                string.append("; ").append(entry.getKey());
            }
            //If got value
            if (entry.getValue() != null)
                //Append it
                string.append("=").append(entry.getValue());
        }
        ;
        return string.toString();
    }

}
