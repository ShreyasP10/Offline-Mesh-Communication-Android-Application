package com.example.omc.protocol;

public class OMCMessage {

    private OMCHeader header;

    private String payload;

    private String signature;

    public OMCMessage() {
        // Required by Gson
    }

    public OMCMessage(
            OMCHeader header,
            String payload
    ) {
        this.header = header;
        this.payload = payload;
    }

    public OMCMessage(
            OMCHeader header,
            String payload,
            String signature
    ) {
        this.header = header;
        this.payload = payload;
        this.signature = signature;
    }

    public OMCHeader getHeader() {
        return header;
    }

    public void setHeader(OMCHeader header) {
        this.header = header;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}