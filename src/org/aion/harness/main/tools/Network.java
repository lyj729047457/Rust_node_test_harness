package org.aion.harness.main.tools;

/**
 * An enum that serves as a mapping between an enum type and a String. Namely, the String that
 * represents the network to use.
 */
public enum Network {

    AVMTESTNET("avmtestnet"),

    MASTERY("mastery"),

    MAINNET("mainnet"),

    CUSTOM("custom"),

    CONQUEST("conquest");

    private String network;

    private Network(String network) {
        this.network = network;
    }

    public String getNetwork() {
        return this.network;
    }

    @Override
    public String toString() {
        return "PublicNetwork { " + this.network + " }";
    }
}
