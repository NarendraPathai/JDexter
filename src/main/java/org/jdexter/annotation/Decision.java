package org.jdexter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation should be provided on a callback method which can take decision on whether the configuration should be 
 * read or not. The method MUST be <code>public</code>, MUST return <code>boolean</code> and MUST NOT throw any Exception.
 * The method MUST ONLY have <code>Class<?></code> as input parameter.
 * <br/>
 * This method is only called when the configuration class contains some conditional configurations.
 * 
 * @see Conditional
 * @author Narendra
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Decision {

}
