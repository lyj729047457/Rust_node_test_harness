module TestingHarness {
    requires org.apache.commons.io;
    requires org.apache.commons.codec;
    requires offline.signer;

    exports org.aion.harness;
    exports org.aion.harness.result;
}