package org.aion.harness.main.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.math.BigInteger;
import java.util.Optional;
import org.aion.harness.misc.Assumptions;
import org.aion.harness.result.RpcResult;
import org.apache.commons.codec.binary.Hex;

/**
 * A tool for parsing the output of a <b>successful</b> {@link RpcResult} object.
 *
 * This is the preferred way of extracting information from the returned output.
 *
 * An rpc output parser is immutable.
 */
public final class RpcOutputParser {
    private final JsonObject outputAsJson;

    public RpcOutputParser(String rpcOutput) {
        if (rpcOutput == null) {
            throw new NullPointerException("Cannot construct rpc output parser with null output.");
        }
        if (rpcOutput.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct rpc output parser with empty output.");
        }

        this.outputAsJson = (JsonObject) new JsonParser().parse(rpcOutput);
    }

    /**
     * Returns {@code true} if, and only if, the rpc output string has an 'error' attribute.
     *
     * @return if the rpc output has an error attribute.
     */
    public boolean hasErrorAttribute() {
        return this.outputAsJson.has("error");
    }

    /**
     * Returns the content of the 'result' attribute of the rpc output string as a String.
     *
     * If this method is unable to parse the 'result' attribute then an empty optional is returned.
     *
     * @return the content of the 'result' attribute.
     */
    public Optional<String> resultAsString() {
        return attributeAsString("result");
    }

    /**
     * Returns the content of the 'error' attribute of the rpc output string as a String.
     *
     * If this method is unable to parse the 'error' attribute then an empty optional is returned.
     *
     * @return the content of the 'error' attribute.
     */
    public Optional<String> errorAsString() {
        return attributeAsString("error");
    }

    /**
     * Returns the content of the 'message' attribute of the rpc output string as a String.
     *
     * If this method is unable to parse the 'message' attribute then an empty optional is returned.
     *
     * @return the content of the 'message' attribute.
     */
    public Optional<String> messageAsString() {
        return attributeAsString("message");
    }

    /**
     * Returns the content of the 'data' attribute of the rpc output string as a String.
     *
     * If this method is unable to parse the 'data' attribute then an empty optional is returned.
     *
     * @return the content of the 'data' attribute.
     */
    public Optional<String> dataAsString() {
        return attributeAsString("data");
    }

    /**
     * Returns the content of the 'result' attribute of the {@link RpcResult#output} string as a
     * {@link BigInteger}.
     *
     * If this method is unable to parse the 'result' attribute then an empty optional is returned.
     *
     * @return the content of the 'result' attribute.
     */
    public Optional<BigInteger> resultAsBigInteger() {
        Optional<String> resultAsHexString = resultAsString();

        try {
            if (resultAsHexString.isPresent()) {
                return Optional.of(new BigInteger(resultAsHexString.get(), 16));
            }
        } catch (Exception e) {
            System.out.println(Assumptions.LOGGER_BANNER + "Unable to parse result attribute for: " + this.outputAsJson);
            System.out.println(Assumptions.LOGGER_BANNER + "Parsing error: " + e.toString());
        }

        return Optional.empty();
    }

    /**
     * Returns the content of the 'result' attribute of the {@link RpcResult#output} string as a
     * byte array.
     *
     * If this method is unable to parse the 'result' attribute then an empty optional is returned.
     *
     * @return the content of the 'result' attribute.
     */
    public Optional<byte[]> resultAsByteArray() {
        Optional<String> resultAsHexString = resultAsString();

        try {
            if (resultAsHexString.isPresent()) {
                return Optional.of(Hex.decodeHex(resultAsHexString.get()));
            }
        } catch (Exception e) {
            System.out.println(Assumptions.LOGGER_BANNER + "Unable to parse result attribute for: " + this.outputAsJson);
            System.out.println(Assumptions.LOGGER_BANNER + "Parsing error: " + e.toString());
        }

        return Optional.empty();
    }

    private Optional<String> attributeAsString(String attribute) {
        String result = null;

        try {
            String resultString = this.outputAsJson.get(attribute).toString();

            // The string may come with a pair of leading and trailing quotes. If so, remove them.
            resultString = (resultString.startsWith("\"")) ? resultString.substring(1) : resultString;
            resultString = (resultString.endsWith("\"")) ? resultString.substring(0, resultString.length() - 1) : resultString;

            // The string may be a hex string, leading with a "0x" prefix. If so, remove it.
            result = (resultString.startsWith("0x")) ? resultString.substring(2) : resultString;

        } catch (Exception e) {
            System.out.println(Assumptions.LOGGER_BANNER + "Unable to parse " + attribute + " attribute for: " + this.outputAsJson);
            System.out.println(Assumptions.LOGGER_BANNER + "Parsing error: " + e.toString());
        }

        return (result == null) ? Optional.empty() : Optional.of(result);
    }

}
