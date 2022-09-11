import { JvbusterClient, Logger } from './JvbusterClient.js';

export class JvbusterClientBuilder {
    private address: string = '';
    private token?: string;
    private logger?: Logger;
    private onNewEndpointMessage?: (from: string, message: object) => void;
    private onNewMessageForDataChannel?: (msg: string) => void;
    private videoBandwidth?: number;
    private audioBandwidth?: number;
    private onEndpoints?: (endpoints: string[]) => void;

    public setAddress(address: string) : JvbusterClientBuilder {
        this.address = address;
        return this;
    }

    public setToken(token: string) : JvbusterClientBuilder {
        this.token = token;
        return this;
    }

    public setOnNewEndpointMessage(onNewEndpointMessage: (from: string, message: object) => void) : JvbusterClientBuilder {
        this.onNewEndpointMessage = onNewEndpointMessage;
        return this;
    }

    public setOnNewMessageForDataChannel(onNewMessageForDataChannel: (msg: string) => void) : JvbusterClientBuilder {
        this.onNewMessageForDataChannel = onNewMessageForDataChannel;
        return this;
    }

    public setLogger(logger: Logger) : JvbusterClientBuilder {
        this.logger = logger;
        return this;
    }

    public setVideoBandwidth(videoBandwidth: number) : JvbusterClientBuilder {
        this.videoBandwidth = videoBandwidth;
        return this;
    }

    public setAudioBandwidth(audioBandwidth: number) : JvbusterClientBuilder {
        this.audioBandwidth = audioBandwidth;
        return this;
    }

    public setOnEndpoints(onEndpoints: (endpoints: string[]) => void) : JvbusterClientBuilder {
        this.onEndpoints = onEndpoints;
        return this;
    }

    public build() : JvbusterClient {
        if(this.token == null) {
            throw 'token has no value';
        }
        if(this.logger == null) {
            throw 'logger has no value';
        }
        return new JvbusterClient(
            this.address,
            this.token,
            this.logger,
            this.onNewEndpointMessage,
            this.onNewMessageForDataChannel,
            this.videoBandwidth,
            this.audioBandwidth,
            this.onEndpoints
        );
    }
}