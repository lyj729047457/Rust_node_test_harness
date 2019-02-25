package org.aion.harness.main.tools;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.aion.harness.result.RpcResult;

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
     * Returns {@code true} only if the output string being parsed contains the specified attribute.
     *
     * @param attribute The attribute whose existence is to be determined.
     * @return whether or not the attribute exists.
     */
    public boolean hasAttribute(String attribute) {
        return this.outputAsJson.has(attribute);
    }

    /**
     * Returns the content corresponding to the specified attribute as a string.
     *
     * If the specified attribute does not exist, or if its content is the string 'null' or the
     * empty string, then this method returns null.
     *
     * Otherwise, this method returns the corresponding content.
     *
     * Note, the returned string will be stripped of any leading or trailing quotation marks and
     * if the '0x' hexadecimal identifier was present it will also be removed.
     *
     * @param attribute The attribute whose content is to be fetched.
     * @return the corresponding content as a string.
     */
    public String attributeToString(String attribute) {
        if (!hasAttribute(attribute)) {
            return null;
        }

        String element = cleanElement(this.outputAsJson.get(attribute).toString());

        if ((element.isEmpty()) || (element.equals("null"))) {
            return null;
        }

        return element;
    }

    /**
     * Returns rawElement with no leading or trailing quotation marks and with no '0x' hex
     * identifier.
     */
    private String cleanElement(String rawElement) {
        rawElement = (rawElement.startsWith("\"")) ? rawElement.substring(1) : rawElement;
        rawElement = (rawElement.endsWith("\"")) ? rawElement.substring(0, rawElement.length() - 1) : rawElement;
        return (rawElement.startsWith("0x")) ? rawElement.substring(2) : rawElement;
    }

}
