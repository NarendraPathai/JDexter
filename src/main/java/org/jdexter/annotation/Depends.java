package org.jdexter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation when specified for a configuration the framework will lookup if the configuration
 * is already read and cached, if the configuration is already read then the <b><i>same instance</i></b>
 * is returned, while if configuration is not read then it is freshly read and cached to be provided
 * for future dependencies. This behavior is different from {@link Configuration} in which every time
 * a new instance is created and returned.
 * 
 * @author Narendra
 *
 */

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Depends {

}
