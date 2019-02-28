package org.aion.harness.unit;

import org.aion.harness.kernel.PrivateKey;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

public class PrivateKeyTest {
    private String testingPrivateKey = "32ee00c327f522f0c8d300921148a6c42f40a3ce45c1f56baa7bfa752200d9e5";

    @Test (expected = IllegalArgumentException.class)
    public void testInstantiatingAddressWithInvalidPrivateKey() throws InvalidKeySpecException {
        PrivateKey.fromBytes(new byte[0]);
    }

    @Test
    public void testPrivateKeyEquality() throws DecoderException, InvalidKeySpecException {
        PrivateKey privateKey = PrivateKey.fromBytes(Hex.decodeHex(testingPrivateKey));
        PrivateKey privateKey2 = PrivateKey.fromBytes(Hex.decodeHex(testingPrivateKey));

        Assert.assertTrue(privateKey.equals(privateKey2));
        Assert.assertEquals(privateKey.hashCode(), privateKey2.hashCode());
    }

    @Test
    public void testImmutabilityByModifyingOriginalPrivateKeyBytes() throws DecoderException, InvalidKeySpecException {
        byte[] randomPrivateKeyCopy = Hex.decodeHex(testingPrivateKey);
        PrivateKey privateKey = PrivateKey.fromBytes(randomPrivateKeyCopy);

        // modify PrivateKeyString
        randomPrivateKeyCopy[0] = (byte)((int) randomPrivateKeyCopy[0] + 1);

        // check that that PrivateKey bytes in the object has not changed
        Assert.assertFalse(Arrays.equals(randomPrivateKeyCopy, privateKey.getPrivateKeyBytes()));
        Assert.assertArrayEquals(Hex.decodeHex(testingPrivateKey), privateKey.getPrivateKeyBytes());
    }

    @Test
    public void testImmutabilityByModifyingReturnedPrivateKeyBytes() throws DecoderException, InvalidKeySpecException {
        byte[] randomPrivateKeyCopy = Hex.decodeHex(testingPrivateKey);
        PrivateKey privateKey = PrivateKey.fromBytes(randomPrivateKeyCopy);

        // retrieve the PrivateKey bytes from the object and modify
        byte[] retrievedPrivateKey = privateKey.getPrivateKeyBytes();
        retrievedPrivateKey[0] = (byte)((int) retrievedPrivateKey[0] + 1);

        // check that the original PrivateKey was not changed
        Assert.assertFalse(Arrays.equals(retrievedPrivateKey, randomPrivateKeyCopy));
        Assert.assertFalse(Arrays.equals(retrievedPrivateKey, privateKey.getPrivateKeyBytes()));
        Assert.assertArrayEquals(Hex.decodeHex(testingPrivateKey), privateKey.getPrivateKeyBytes());
    }
}
