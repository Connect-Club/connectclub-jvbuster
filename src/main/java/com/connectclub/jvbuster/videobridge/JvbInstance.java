package com.connectclub.jvbuster.videobridge;

import com.connectclub.jvbuster.exception.EndpointNotFound;
import com.connectclub.jvbuster.repository.data.JvbInstanceData;
import com.connectclub.jvbuster.videobridge.data.jvb.*;
import com.connectclub.jvbuster.videobridge.exception.JvbInstanceRestException;
import com.google.gson.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class JvbInstance {

    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Getter
    private final String id;

    @Getter
    private final String version;

    @Getter
    private final String scheme;

    @Getter
    private final String host;

    @Getter
    private final int port;

    @Getter
    private final Instant creationTimestamp;

    @Getter
    private final boolean nodeExporterAvailable;

    @Getter
    private final boolean forSpeakers;

    @Getter
    private final int octoBindPort;

    private final OkHttpClient okHttpClient;

    private final static Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(ChannelCommon.class, new JsonDeserializer<ChannelCommon>() {
                @Override
                public ChannelCommon deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    JsonElement typeElem = json.getAsJsonObject().get("type");
                    if(typeElem != null && "octo".equals(typeElem.getAsString())) {
                        return gson.fromJson(json, OctoChannel.class);
                    }
                    return gson.fromJson(json, Channel.class);
                }
            })
            .registerTypeAdapter(ChannelCommon.class, new JsonSerializer<ChannelCommon>() {
                @Override
                public JsonElement serialize(ChannelCommon src, Type typeOfSrc, JsonSerializationContext context) {
                    if(src instanceof OctoChannel) {
                        return gson.toJsonTree(src, OctoChannel.class);
                    }
                    return gson.toJsonTree(src, Channel.class);
                }
            })
            .create();

    public JvbInstance(JvbInstanceData jvbInstanceData, OkHttpClient okHttpClient) {
        id = jvbInstanceData.getId();
        version = jvbInstanceData.getVersion();
        scheme = jvbInstanceData.getScheme();
        host = jvbInstanceData.getHost();
        port = jvbInstanceData.getPort();
        nodeExporterAvailable = jvbInstanceData.getNodeTime() != null;
        creationTimestamp = null;
        forSpeakers = jvbInstanceData.isForSpeakers();
        octoBindPort = jvbInstanceData.getOctoBindPort();

        this.okHttpClient = okHttpClient;
    }

    public static JvbInstance from(JvbInstanceData jvbInstanceData, OkHttpClient okHttpClient) {
        return new JvbInstance(jvbInstanceData, okHttpClient);
    }

    private <T> T request(String path, String method, Object requestBody, Type responseTypeOfT) throws IOException, JvbInstanceRestException {
        String bodyJson = gson.toJson(requestBody);
        RequestBody body = requestBody != null ? RequestBody.create(bodyJson, JSON) : null;
        Request request = new Request.Builder()
                .url(scheme + "://" + host + ":" + port + path)
                .method(method, body)
                .build();
        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new JvbInstanceRestException(getId(), path, method, bodyJson, response.code(), response.message(), response.body().string());
            }
            T r = null;
            if (responseTypeOfT != null) {
                r = gson.fromJson(response.body().string(), responseTypeOfT);
            }
            return r;
        }
    }

    private <T> T getRequest(String path, Object requestBody, Type responseTypeOfT) throws IOException, JvbInstanceRestException {
        return request(path, "GET", requestBody, responseTypeOfT);
    }

    private <T> T postRequest(String path, Object requestBody, Type responseTypeOfT) throws IOException, JvbInstanceRestException {
        return request(path, "POST", requestBody, responseTypeOfT);
    }

    private <T> T patchRequest(String path, Object requestBody, Type responseTypeOfT) throws IOException, JvbInstanceRestException {
        return request(path, "PATCH", requestBody, responseTypeOfT);
    }

    private <T> T deleteRequest(String path, Object requestBody, Type responseTypeOfT) throws IOException, JvbInstanceRestException {
        return request(path, "DELETE", requestBody, responseTypeOfT);
    }


    private <T> T request(String path, String method, Object requestBody, Class<T> responseClassOfT) throws IOException, JvbInstanceRestException {
        return request(path, method, requestBody, (Type) responseClassOfT);
    }

    private <T> T getRequest(String path, Object requestBody, Class<T> responseClassOfT) throws IOException, JvbInstanceRestException {
        return getRequest(path, requestBody, (Type) responseClassOfT);
    }

    private <T> T postRequest(String path, Object requestBody, Class<T> responseClassOfT) throws IOException, JvbInstanceRestException {
        return postRequest(path, requestBody, (Type) responseClassOfT);
    }

    private <T> T patchRequest(String path, Object requestBody, Class<T> responseClassOfT) throws IOException, JvbInstanceRestException {
        return patchRequest(path, requestBody, (Type) responseClassOfT);
    }

    private <T> T deleteRequest(String path, Object requestBody, Class<T> responseClassOfT) throws IOException, JvbInstanceRestException {
        return deleteRequest(path, requestBody, (Type) responseClassOfT);
    }


    public Conference getConference(String conferenceId) throws IOException, JvbInstanceRestException {
        return getRequest("/colibri/conferences/" + conferenceId, null, Conference.class);
    }

    public Conference getConference(String conferenceId, String endpoint) throws IOException, JvbInstanceRestException {
        return getRequest("/colibri/conferences/" + conferenceId + "?endpoint=" + endpoint, null, Conference.class);
    }

    public List<Conference> getConferences() throws IOException, JvbInstanceRestException {
        return getRequest("/colibri/conferences", null, Conference.LIST_TYPE);
    }

    public Conference createConference(String gid) throws IOException, JvbInstanceRestException {
        return postRequest("/colibri/conferences", Conference.builder().gid(gid).build(), Conference.class);
    }

    public Conference patchConference(Conference conference) throws IOException, JvbInstanceRestException {
        try {
            return patchRequest("/colibri/conferences/" + conference.getId(), conference, Conference.class);
        } catch (JvbInstanceRestException e) {
            if(e.getResponseCode() == HttpStatus.BAD_REQUEST.value() && e.getResponseMessage().startsWith("Failed to create conference: No SCTP connection found for ID:")) {
                throw new EndpointNotFound(e);
            }
            throw e;
        }
    }

    public Conference patchConference(Conference conference, String endpoint) throws IOException, JvbInstanceRestException {
        try {
            return patchRequest("/colibri/conferences/" + conference.getId() + "?endpoint=" + endpoint, conference, Conference.class);
        } catch (JvbInstanceRestException e) {
            if(e.getResponseCode() == HttpStatus.BAD_REQUEST.value() && e.getResponseMessage().startsWith("Failed to create conference: No SCTP connection found for ID:")) {
                throw new EndpointNotFound(e);
            }
            throw e;
        }
    }

    public void broadcastMessage(String conferenceId, String colibriClass, Object message) throws IOException, JvbInstanceRestException {
        postRequest("/colibri/conferences/" + conferenceId + "/broadcast-message/" + colibriClass, message, null);
    }

    public void deleteEndpoint(String conferenceId, String endpointId) throws IOException, JvbInstanceRestException {
        deleteRequest("/colibri/conferences/" + conferenceId + "/endpoint/" + endpointId, null, null);
    }

    public Stats getStats() throws IOException, JvbInstanceRestException {
        return getRequest("/colibri/stats", null, Stats.class);
    }

    public void deleteConferences() throws IOException, JvbInstanceRestException {
        deleteRequest("/colibri/conferences", null, null);
    }

    public List<Conference> expireConferences() throws IOException, JvbInstanceRestException {
        return postRequest("/colibri/conferences/expire", Map.of(), Conference.LIST_TYPE);
    }

    public void shutdown(boolean graceful) throws IOException, JvbInstanceRestException {
        postRequest("/colibri/shutdown", Map.of("graceful-shutdown", graceful), null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JvbInstance that = (JvbInstance) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "JvbInstance{" +
                "id='" + id + '\'' +
                ", scheme='" + scheme + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
