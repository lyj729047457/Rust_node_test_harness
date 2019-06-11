package org.aion.harness.tests.integ.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.aion.harness.main.NodeFactory.NodeType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ExcludeNodeType {
    NodeType[] value() default {};
}
