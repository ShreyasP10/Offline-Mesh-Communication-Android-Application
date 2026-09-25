package com.example.omc.protocol;

public class OMCMessage {

    private OMCHeader header;

    private String payload;

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
}