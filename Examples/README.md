# Examples
The contents of this module do not belong to the project proper. These are "proof of concept" demos we have put together at various milestones. The demos may not be up to date and may not always be reproducible (the Example1 demo relies on the Avm Testnet, which gets reset from time to time). We will make a best effort to keep them working, but these examples are more of interest to us only for a short time. They are held here mostly as references and to give people an idea of how to use the tool.

## Reproducing the examples
The examples themselves will not work as they are (some context is required). Below is the context required to reproduce each example, if in fact it can be reproduced.

### Example1
- To build and run, this example will first of all require the dependent jar files located in `TestHarness/lib/`
- A 32-byte private key and its corresponding Aion address is required; this address needs some balance on the Avm Testnet.
- The recommended kernel build can be obtained from the 'aion' repository by running `./gradlew clean pack` on the branch `node_harness_example1` (note this project contains submodules, you will probably want to run `git submodule update --init --recursive` after checking out the branch). The build directory path (which should be the 3rd command-line argument) is the `pack/` directory inside this aion project.

_Some notes about this example:_
- The purpose of this proof of concept was to demonstrate the tool's ability to start up a local node, connect it to a network, wait for it to sync up with the network, send some transactions and wait for them to be processed, display some time measurements, and shut down the local node.
- The node_test_harness jar file that is used by this example is built from the first release on our repo, at commit `56876ed`.
- The avm jar file that is used by this example is built from the Feb 8, 2018 Avm Testnet release on the AVM repository (built at commit `03275e4`)

