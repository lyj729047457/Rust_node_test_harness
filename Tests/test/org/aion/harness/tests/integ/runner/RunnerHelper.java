package org.aion.harness.tests.integ.runner;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aion.harness.main.NodeFactory.NodeType;
import org.aion.harness.tests.integ.runner.internal.ThreadSpecificStderr;
import org.aion.harness.tests.integ.runner.internal.ThreadSpecificStdout;

class RunnerHelper {

    // -- Methods for determining which kernel node types need to be run --------------------------

    private final static Map<String, NodeType> NODE_STRING_TO_ENUM = Map.ofEntries(
        Map.entry("java", NodeType.JAVA_NODE),
        Map.entry("rust", NodeType.RUST_NODE)
    );

    List<NodeType> determineNodeTypes(List<NodeType> supportedNodes) {
        String propString = System.getProperty("testNodes");
        if(null == propString || propString.isEmpty()) {
            return supportedNodes;
        }

        List<NodeType> ret = new LinkedList<>();
        for(String nodeString : propString.split(",")) {
            NodeType maybeNodeType = NODE_STRING_TO_ENUM.get(nodeString.toLowerCase().trim());
            if(maybeNodeType == null) {
                throw new IllegalArgumentException("Unrecognized node type: " + nodeString);
            }
            ret.add(maybeNodeType);
        }

        return ret;
    }

    // -- Methods for processing annotations ------------------------------------------------------

    Set<NodeType> determineExcludedNodeTypes(Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType.equals(ExcludeNodeType.class)) {
                return Set.of(((ExcludeNodeType) annotation).value());
            }
        }
        return Collections.emptySet();
    }

    // -- Methods for managing test case stdout/stdin ---------------------------------------------

    /**
     * Replaces System.out with a custom print stream that allows each thread to print to a thread
     * local stdout stream.
     *
     * This method returns the original System.out print stream.
     */
    PrintStream replaceStdoutWithThreadSpecificOutputStream() {
        PrintStream originalOut = System.out;
        ThreadSpecificStdout threadSpecificStdout = new ThreadSpecificStdout();
        System.setOut(threadSpecificStdout);
        threadSpecificStdout.setStdout(originalOut);
        return originalOut;
    }

    /**
     * Replaces System.err with a custom print stream that allows each thread to print to a thread
     * local stderr stream.
     *
     * This method returns the original System.err print stream.
     */
    PrintStream replaceStderrWithThreadSpecificErrorStream() {
        PrintStream originalErr = System.err;
        ThreadSpecificStderr threadSpecificStderr = new ThreadSpecificStderr();
        System.setErr(threadSpecificStderr);
        threadSpecificStderr.setStderr(originalErr);
        return originalErr;
    }
}
