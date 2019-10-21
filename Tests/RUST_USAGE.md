# Rust node test usage

## Requirements

  Download and setup these requirements:

* [aion_solo_pool](https://github.com/aionnetwork/aion_miner/tree/patch-jsonrpc)
```
git clone -b patch-jsonrpc git@github.com:aionnetwork/aion_miner.git
```
* [UnityBootstrap](https://github.com/jeff-aion/UnityBootstrap/tree/updated_scripts)
```
git clone -b updated_scripts git@github.com:jeff-aion/UnityBootstrap.git
```
* [external_staker](https://github.com/arajasek/external_staker)
```
git clone git@github.com:arajasek/external_staker.git
```

## Configs

### unityUpdate

For `BeaconHashTest` in concurrent test, it's `10`.

For `RustBeaconHashSidechainTest` in sequential test, it's `6`.

### rpc http port

For `BeaconHashTest` in concurrent test, it's `8545`.

For `RustBeaconHashSidechainTest` in sequential test, node1 is `8101`, node2 is `8102`.

External tools will connect kernal via rpc port. The port used here:

1. aion_solo_pool/pool_configs/aion.json `paymentProcessing` and `daemons`
```
    "paymentProcessing": {
        "enabled": false,
        "paymentInterval": 20,
        "minimumPayment": 70,
        "daemon": {
            "host": "127.0.0.1",
            "__port": 19332,
            "port": 8545,
            "user": "",
            "password": ""
        }
    },
```
```
    "daemons": [
        {
            "host": "127.0.0.1",
            "port": 8545,
            "user": "",
            "password": ""
        }
    ],
```
2. UnityBootstrap/bootstrap.sh `NODE_ADDRESS`
```
NODE_ADDRESS="127.0.0.1:8545"
```

Make sure they were fixed to the correct number before running the command

## Concurrent testing 
1. copy or extract the aionr package directory named `aionr` to node_test_harness/Tests if there is no `aionr` under `Tests`

2. fix rpc port to 8545

3. run tests under test harness directory
```
./gradlew :Tests:test -PtestNodes=rust
```
4. run bootstrap.sh under bootstrap directory
```
./bootstrap.sh
```
5. run run.sh under aion_solo_pool directory
```
./run.sh
```
6. waiting for the contract to complete deployment (bootstrap.sh finished running)

7. run external staker under external_staker directory
```
java -jar external_staker.jar 0xcc76648ce88bc18130bc9d637995e5c42a922ebeab78795fac58081b9cf9d4 a02df9004be3c4a20aeb50c459212412b1d0a58da3e1ac70ba74dde6b4accf4b "127.0.0.1" "8545"
```

After the tests finish running, stop external staker and aion solo pool, remove `data` under `node_test_harness\Tests\aionr` for next test.

## RustBeaconHashSidechainTest
1. copy or extract the aionr package directory named `aion` to node_test_harness/Tests if there is no `aionr` under `Tests`

2. copy or extract the aionr package directory named `aion2` to node_test_harness/Tests if there is no `aionr2` under `Tests`

3. fix rpc port to 8102

4. run tests under test harness directory
```
./gradlew :Tests:test -PtestNodes=rust -Psequential
```
5. run bootstrap.sh under bootstrap directory
```
./bootstrap.sh
```
6. run run.sh under aion_solo_pool directory
```
./run.sh
```
7. waiting for the contract to complete deployment (bootstrap.sh finished running)

8. run external staker under external_staker directory
```
java -jar external_staker.jar 0xcc76648ce88bc18130bc9d637995e5c42a922ebeab78795fac58081b9cf9d4 a02df9004be3c4a20aeb50c459212412b1d0a58da3e1ac70ba74dde6b4accf4b "127.0.0.1" "8102"
```
9. `tail -f` for the newest out log file in `node_test_harness\Tests\logs` and wait for the kernel shutdown

10. stop the external staker and aion solo pool

11. fix rpc port to 8101

12. repeat 5-7

13. run external staker under external_staker directory
```
java -jar external_staker.jar 0xcc76648ce88bc18130bc9d637995e5c42a922ebeab78795fac58081b9cf9d4 a02df9004be3c4a20aeb50c459212412b1d0a58da3e1ac70ba74dde6b4accf4b "127.0.0.1" "8101"
```
14. repeat 9-10

After the tests finish running, remove `data` under `node_test_harness\Tests\aionr` and `node_test_harness\Tests\aionr2` for next test.

## shutdown stuck
Due to rust shutdown bug , it maybe failed to shutdown the kernal in the tests, and the test will report timeout error.
It maybe happen before run bootstrap.sh or after the step 14 in `RustBeaconHashSidechainTest`.
So check the kernel status with `ps -ef | grep aion`, especially the start time, and kill it if stuck.


