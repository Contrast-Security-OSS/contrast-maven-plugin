package com.contrastsecurity.maven.plugin.it.stub;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Provides a JUnit test with a {@link ContrastAPI} stub for testing. Starts the {@code ContrastAPI}
 * instance before starting the test, and handles gracefully terminating the Contrast API instance
 * at the conclusion of the test.
 *
 * <pre>
 *   &#64;ContrastAPIStub
 *   &#64;Test
 *   public void test(final ContrastAPI contrast) { ... }
 * </pre>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(ContrastAPIStubExtension.class)
public @interface ContrastAPIStub {}
