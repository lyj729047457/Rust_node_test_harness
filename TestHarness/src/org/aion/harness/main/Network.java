package org.aion.harness.main;

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

    /**
     * The name of the network.
     *
     * @return the network name.
     */
    public String string() {
        return this.network;
    }

    @Override
    public String toString() {
        return "Network { " + this.network + " }";
    }
}
