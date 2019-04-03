package org.aion.harness.kernel;

import org.aion.harness.main.types.TransactionReceipt;
import org.apache.commons.codec.binary.Hex;

/**
 * An Aion transaction.
 *
 * @implNote Currently only supports a subset of the fields that an Aion
 * transaction can have.  As we add more test cases, we can add in the fields
 * as they become needed.
 */
public class Transaction {
    private final Address to;
    private final byte[] data;

    /** Constructor */
    public Transaction(Address to, byte[] data) {
        this.to = to;
        this.data = data;
    }

    /** @return <code>to</code> field of transaction */
    public Address getTo() {
        return to;
    }

    /** @return <code>data</code> field of transaction */
    public byte[] getData() {
        return data;
    }

    /**
     * serialize to JSON
     *
     * @return JSON representation
     * @implNote If we end up have more classes like this (data holder objects that
     * represent structures defined in JSON), should add Jackson mapper library and use
     * that instead.
     */
    public String jsonString() {
        return String.format("{"
                + "\"to\" : \"%s\","
                + "\"data\" : \"%s\""
                + "}",
            Hex.encodeHexString(getTo().getAddressBytes()),
            getData() != null? Hex.encodeHexString(getData()) : "null"
        );
    }
}
