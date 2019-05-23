# Test Harness
This module contains the source code for the testing harness. This is where all active development takes place. All tests in this module are tests on the behaviour of the harness itself. This is not to be confused with the `Tests` module, which contains all of the tests that _use_ the test harness.

## Build and run the tests
```shell
ant && ant test
```
The built jar will be in the newly created `dist` directory.
