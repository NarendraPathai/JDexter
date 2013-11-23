package org.jdexter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdexter.reader.DefaultReader;
import org.jdexter.reader.Reader;

/**
 * The annotation can be placed at two places:
 * <br/>
 * <i>Class Level:</i>
 * When provided at class level then it is used for extracting the meta data regarding how
 * the class should be read, written.
 * <br/><br/>
 * <i>Property level:</i>
 * When provided at property or field level then it suggests container relationship with the containing class.
 * A new freshly read configuration object will be injected, which is different than {@link Depends} 
 * @author Narendra
 *
 */

@Target(value = {ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {
	Class<? extends Reader> readWith() default DefaultReader.class;
}
