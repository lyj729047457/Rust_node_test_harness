# Tests
This module is where we store all of the tests that _use_ the testing harness. This is not the place to test out the harness itself - that's what the `TestHarness` module is for!

The tests here all need some way of talking about some external artifact, the specific kernel they rely on. Include comments on your tests to point any such assumptions out so that others know how to properly run your tests. Ideally, strive to write tests that make as few assumptions as possible.
Place contracts and any common utility/helper functionality in the src directory, in an appropriate package.
