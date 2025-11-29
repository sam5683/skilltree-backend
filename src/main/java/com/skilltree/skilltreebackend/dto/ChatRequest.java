package com.skilltree.skilltreebackend.dto;
public class ChatRequest {
    private String node;
    private String message;
    private String mode; // "fast" or "deep"

    public String getNode() {
        return node;
    }
    public void setNode(String node) {
        this.node = node;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public String getMode() {
        return mode;
    }
    public void setMode(String mode) {
        this.mode = mode;
    }
}


    

