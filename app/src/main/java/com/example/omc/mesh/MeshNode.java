package com.example.omc.mesh;

import java.util.UUID;

public class MeshNode {

    private final String nodeId;
    private String nodeName;

    public MeshNode(String nodeName) {
        this.nodeId = UUID.randomUUID().toString();
        this.nodeName = nodeName;
    }

    public MeshNode(String nodeId, String nodeName, boolean existingNode) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    @Override
    public String toString() {
        return "MeshNode{" +
                "nodeId='" + nodeId + '\'' +
                ", nodeName='" + nodeName + '\'' +
                '}';
    }
}