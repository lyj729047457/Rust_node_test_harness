package org.aion.harness.kernel;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.aion.harness.main.Network;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

/**
 * An instance of a kernel.
 *
 * This class is immutable.
 */
public final class Kernel {
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("\\p{XDigit}{64}");
    private final File kernelDirectory;
    private final String network;

    public Kernel(File kernelDirectory, Network network) {
        if (kernelDirectory == null) {
            throw new NullPointerException("Cannot create a kernel with a null directory!");
        }
        if (network == null) {
            throw new NullPointerException("Cannot create a kernel with a null network!");
        }
        this.kernelDirectory = kernelDirectory;
        this.network = network.string();
    }

    /**
     * Creates a new account in the kernel's keystore and returns the newly created address.
     *
     * @param password The password to unlock the keystore file.
     * @return the account address.
     */
    public Address createNewAccountInKeystore(String password) throws IOException, InterruptedException, DecoderException {
        Process process = new ProcessBuilder()
            .command("./aion.sh", "-n", this.network, "-a", "create")
            .directory(this.kernelDirectory)
            .start();

        // Write the password to the process so that it can create the account.
        OutputStream outputStream = process.getOutputStream();
        outputStream.write((password + "\n").getBytes("UTF-8"));
        outputStream.write(password.getBytes("UTF-8"));
        outputStream.close();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IllegalStateException("Invoking create in the kernel failed with exit code: " + exitCode);
        }

        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            while (line != null) {
                stringBuilder.append(line);
                line = reader.readLine();
            }
        }
        String output = stringBuilder.toString();
        process.destroy();

        Matcher matcher = ADDRESS_PATTERN.matcher(output);
        if (matcher.find()) {
            return new Address(Hex.decodeHex(matcher.group()));
        } else {
            throw new IOException("Failed to parse program output: " + output);
        }
    }

    /**
     * Deletes all accounts in this kernel's keystore.
     */
    public void clearKeystore() throws IOException {
        String keystorePath = this.kernelDirectory.getCanonicalPath() + File.separator + this.network + File.separator + "keystore";
        FileUtils.deleteDirectory(new File(keystorePath));
    }

    @Override
    public String toString() {
        return "Kernel { path = " + this.kernelDirectory.getPath() + ", network = " + this.network + " }";
    }
}
