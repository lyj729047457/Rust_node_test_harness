pragma solidity ^0.4.10;

contract FvmCrossCallDispatcher {

    function dispatch(address a) public {
        // We don't actually care about what the data we send in is, we should get rejected right away.
        require(a.call(bytes4(keccak256("sample(uint128)"))));
    }

}
