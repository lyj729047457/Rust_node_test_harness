These are temporary instructions for setting up Rust kernel so that the `Rust_*` test classes under `org.aion.harness.tests.integ` package can run against it.  Will improve them before this stuff is merged to master.

#### Setup rust kernel

1. If not already done, set up the project in your IDE.  Easiest way if you're using IntelliJ is to just open the file `node_test_harness/build.gradle`.
1. Extract rust kernel into node_test_harness/Tests/aionr

#### Execute tests

There are two ways.  You can do it through gradle:

`./gradlew rustTest`

Or you can do it through IDE:  

1. First, you must copy `node_test_harness/Tests/test-resources/rust-custom/*` into `node_test_harness/aionr/custom` (overwriting anything that is already there)
1. Then you can use IDE to run the tests under `org.aion.harness.tests.integ.rust`.
