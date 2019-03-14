# Test Harness
This module contains the source code for the testing harness. This is where all active development takes place. All tests in this module are tests on the behaviour of the harness itself. This is not to be confused with the `Tests` module, which contains all of the tests that _use_ the test harness.

## Build and run the tests
```shell
ant && ant test
```
The built jar will be in the newly created `dist` directory.

### Why don't the tests pass?
We strive to ensure that all tests pass at every commit. A common reason why the tests are failing is because of some of the assumptions these tests make about your file system and the kernel build you're using.

Here's how your system should be set up:

1. Let's assume the directory that this test harness is in on your system is named node_test_harness. From this root directory you should be able to get to the aion repository (whose root directory must be named aion on your system) like so: `cd ../aion`
2. In the aion directory run: `git checkout node_test_harness && git submodule update --init --recursive && ./gradlew clean pack`

The tests should now all pass.
