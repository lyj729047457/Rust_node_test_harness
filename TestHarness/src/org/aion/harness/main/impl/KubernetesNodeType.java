package org.aion.harness.main.impl;

public enum KubernetesNodeType {

    AION_1("aion-1"),

    AION_2("aion-2"),

    AION_3("aion-3"),

    AION_SEED("aion-seed");

    private String  node;

    private KubernetesNodeType (String node) {
        this.node = node;
    }

    public String getName() {
        return this.node;
    }

    @Override
    public String toString() {
        return "KubernetesNodeType { " + this.node + " }";
    }
}
