package org.aion.harness.main.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.aion.harness.misc.Assumptions;

/**
 * A class responsible for calling an RPC endpoint using the provided payload.
 */
public final class RpcCaller {

    public InternalRpcResult call(RpcPayload payload, boolean verbose) throws InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder()
            .command("curl", "-X", "POST", "--data", payload.payload, Assumptions.IP + ":" + Assumptions.PORT);

        if (verbose) {
            processBuilder.inheritIO();
        }

        try {
            long timeOfCall = System.currentTimeMillis();
            Process rpcProcess = processBuilder.start();

            int status = rpcProcess.waitFor();
            StringBuilder stringBuilder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(rpcProcess.getInputStream()))) {
                String line = reader.readLine();

                while (line != null) {
                    stringBuilder.append(line);
                    line = reader.readLine();
                }
            }

            String output = stringBuilder.toString();

            if (output.isEmpty()) {
                return InternalRpcResult.unsuccessful("unknown error");
            }

            JsonStringParser outputParser = new JsonStringParser(output);

            // This is only successful if the RPC Process exited successfully, and the RPC output
            // contained no 'error' content and it does contain 'result' content.

            if ((status == 0) && (!outputParser.hasAttribute("error")) && (outputParser.attributeToString("result") != null)) {
                return InternalRpcResult.successful(output, timeOfCall);
            } else {
                String error = outputParser.attributeToString("error");

                // We expect the content of 'error' to itself be a Json String. If it has no content
                // then the error is unknown.
                if (error == null) {
                    return InternalRpcResult.unsuccessful("unknown error");
                } else {
                    JsonStringParser errorParser = new JsonStringParser(error);

                    // The 'data' attribute should capture the error.
                    error = errorParser.attributeToString("data");

                    // If there was no data value then try to grab the less informative 'message'.
                    error = (error == null) ? errorParser.attributeToString("message") : error;

                    return InternalRpcResult.unsuccessful(error);
                }
            }

        } catch (IOException e) {
            return InternalRpcResult.unsuccessful(e.toString());
        }
    }

}
