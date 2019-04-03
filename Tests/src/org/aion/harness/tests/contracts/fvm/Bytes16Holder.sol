pragma solidity ^0.4.10;

contract Bytes16Holder {
  bytes16 data;

  function Bytes16Holder() public {
    data = 0x0102;
  }
  function set(bytes16 newData) public {
    data = newData;
  }
  function get() public constant returns (bytes16) {
    return data;
  }

}