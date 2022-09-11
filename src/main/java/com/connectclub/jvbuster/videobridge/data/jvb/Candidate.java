package com.connectclub.jvbuster.videobridge.data.jvb;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Candidate {

    public enum Type {
        @SerializedName("prflx") PRFLX,
        @SerializedName("srflx") SRFLX,
        @SerializedName("relay") RELAY,
        @SerializedName("host") HOST,
        @SerializedName("local") LOCAL,
        @SerializedName("stun") STUN,
    }

    public enum Protocol {
        @SerializedName("tcp") TCP,
        @SerializedName("udp") UDP,
        @SerializedName("tls") TLS,
        @SerializedName("dtls") DTLS,
        @SerializedName("sctp") SCTP,
        @SerializedName("ssltcp") SSLTCP,
    }

    public enum TcpType {
        @SerializedName("passive") PASSIVE
    }

    @SerializedName("id")
    private String id;

    @SerializedName("type")
    private Type type;

    @SerializedName("generation")
    private String generation;

    @SerializedName("component")
    private Integer component;

    @SerializedName("protocol")
    private Protocol protocol;

    @SerializedName("port")
    private Integer port;

    @SerializedName("ip")
    private String ip;

    @SerializedName("tcptype")
    private TcpType tcpType;

    @SerializedName("foundation")
    private String foundation;

    @SerializedName("priority")
    private Integer priority;

    @SerializedName("network")
    private String network;

    @SerializedName("rel-addr")
    private String relAddr;

    @SerializedName("rel-port")
    private Integer relPort;

}
