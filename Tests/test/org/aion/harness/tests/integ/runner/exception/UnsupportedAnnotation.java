package org.aion.harness.tests.integ.runner.exception;

/**
 * Thrown when we encounter an annotation that our runners do not handle.
 *
 * Since we have custom runners now there may be quite a few instances of things we do not handle,
 * and rather than fail arbitrarily, we filter out and allow only those features we explicitly do
 * handle.
 */
public final class UnsupportedAnnotation extends RuntimeException {

    public UnsupportedAnnotation(String message) {
        super(message);
    }
}
