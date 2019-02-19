package org.aion.harness.unit;

import org.aion.harness.kernel.Address;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class AddressTest {
    private String testingAddress = "a0ee00c327f522f0c8d342921148a6c42f40a3ce45c1f56baa7bfa752200d9e5";

    @Test
    public void testAddressEquality() throws DecoderException {
        Address address = Address.createAddress(Hex.decodeHex(testingAddress));
        Address address2 = Address.createAddress(Hex.decodeHex(testingAddress));

        Assert.assertTrue(address.equals(address2));
        Assert.assertEquals(address.hashCode(), address2.hashCode());
    }

    @Test
    public void testImmutabilityByModifyingOriginalAddressBytes() throws DecoderException {
        byte[] randomAddressCopy = Hex.decodeHex(testingAddress);
        Address address = Address.createAddress(randomAddressCopy);

        // modify addressString
        randomAddressCopy[0] = (byte)((int) randomAddressCopy[0] + 1);

        // check that that address bytes in the object has not changed
        Assert.assertFalse(Arrays.equals(randomAddressCopy, address.getAddressBytes()));
        Assert.assertArrayEquals(Hex.decodeHex(testingAddress), address.getAddressBytes());
    }

    @Test
    public void testImmutabilityByModifyingReturnedAddressBytes() throws DecoderException {
        byte[] randomAddressCopy = Hex.decodeHex(testingAddress);
        Address address = Address.createAddress(randomAddressCopy);

        // retrieve the address bytes from the object and modify
        byte[] retrievedAddress = address.getAddressBytes();
        retrievedAddress[0] = (byte)((int) retrievedAddress[0] + 1);

        // check that the original address was not changed
        Assert.assertFalse(Arrays.equals(retrievedAddress, randomAddressCopy));
        Assert.assertFalse(Arrays.equals(retrievedAddress, address.getAddressBytes()));
        Assert.assertArrayEquals(Hex.decodeHex(testingAddress), address.getAddressBytes());
    }
}
