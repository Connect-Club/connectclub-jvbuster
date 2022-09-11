package com.connectclub.jvbuster.videobridge.data.sdp;

import lombok.*;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MsidSemanticAttribute extends Attribute {

    public static final String FIELD = "msid-semantic";

    private String semanticToken;

    @Singular
    private Set<String> streams;

    @Override
    public String getField() {
        return FIELD;
    }

    @Override
    public String getValue() {
        return Stream.concat(
                Stream.of(semanticToken),
                streams.stream()
        ).collect(Collectors.joining(" "));
    }
}
