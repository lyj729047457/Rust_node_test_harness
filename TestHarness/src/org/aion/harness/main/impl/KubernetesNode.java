package org.aion.harness.main.impl;

import org.aion.harness.main.RemoteNode;
import org.aion.harness.main.global.SingletonFactory;
import org.aion.harness.result.Result;
import org.aion.harness.util.LogReader;
import org.aion.harness.util.NodeFileManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

public final class KubernetesNode implements RemoteNode {
    private final KubernetesNodeType nodeType;
    private final LogReader logReader;
    private final int ID;

    private boolean isConnected;
    private Process nodeProcess;

    private static final String IMPORT_LOG_COMMAND_1 = "kubectl";
    private static final String IMPORT_LOG_COMMAND_2 = "logs";
    private static final String IMPORT_LOG_COMMAND_3 = "-f";

    private static final String GET_PODS_COMMAND_1 = "kubectl";
    private static final String GET_PODS_COMMAND_2 = "get";
    private static final String GET_PODS_COMMAND_3 = "pods";

    public KubernetesNode(KubernetesNodeType kubernetesNodeType) {
        this.nodeType = kubernetesNodeType;
        logReader = new LogReader();
        this.ID = SingletonFactory.singleton().nodeWatcher().addReader(this.logReader);
        this.isConnected = false;
    }

    @Override
    public int getID() {
        return this.ID;
    }

    @Override
    public Result connect(File logFile) throws IOException {
        if (this.isConnected) {
            throw new IllegalStateException("the remote node is already connected");
        }

        if (logFile == null) {
            throw new NullPointerException("log file cannot be null");
        }

        if (!logFile.isFile()) {
            throw new IllegalArgumentException(logFile + " is not a file");
        }

        String nodeName = getKubernetesNodeName();

        if (nodeName == null) {
            return Result.unsuccessfulDueTo("failed to get kubernetes node name");
        }

        this.nodeProcess = new ProcessBuilder(IMPORT_LOG_COMMAND_1, IMPORT_LOG_COMMAND_2, IMPORT_LOG_COMMAND_3, nodeName)
                .directory(NodeFileManager.getLogsDirectory())
                .redirectOutput(logFile)
                .start();

        Result result = this.logReader.startReading(logFile);

        if (result.isSuccess()) {
            this.isConnected = true;
        }

        return result;
    }

    @Override
    public Result disconnect() throws InterruptedException {
        if (this.isConnected) {
            this.logReader.stopReading();
            this.isConnected = false;
        }

        this.nodeProcess.destroy();
        boolean shutdown = this.nodeProcess.waitFor(1, TimeUnit.MINUTES);

        return  (shutdown) ? Result.successful() : Result.unsuccessfulDueTo("Timed out waiting to disconnect from kubernetes node!");
    }

    /**
     * Grabs the full name of the kubernetes node
     */
    private String getKubernetesNodeName() throws IOException {
        String logOutput = getPods();
        String lines[] = logOutput.split("\n");

        for (String line: lines) {
            if (line.contains(this.nodeType.getName())) {
                return line.substring(0, line.indexOf(" "));
            }
        }
        return null;
    }

    /**
     * Get node information
     */
    private String getPods() throws IOException {
        Process process = new ProcessBuilder(GET_PODS_COMMAND_1, GET_PODS_COMMAND_2, GET_PODS_COMMAND_3).start();

        StringBuilder stringBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String log = reader.readLine();

            while (log != null) {
                stringBuilder.append(log).append("\n");
                log = reader.readLine() ;
            }
        }

        process.destroy();

        return stringBuilder.toString();
    }
}
