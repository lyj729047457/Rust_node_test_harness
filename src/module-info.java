module aion.node.harness {
    requires org.apache.commons.io;
    requires org.apache.commons.codec;
    requires offline.signer;

    exports org.aion.harness.result;
    exports org.aion.harness.kernel;
    exports org.aion.harness.main;
    exports org.aion.harness.main.impl;
}