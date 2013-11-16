package org.jdexter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdexter.reader.DefaultReader;
import org.jdexter.reader.Reader;

@Target(value = {ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {
	Class<? extends Reader> readWith() default DefaultReader.class;
}
