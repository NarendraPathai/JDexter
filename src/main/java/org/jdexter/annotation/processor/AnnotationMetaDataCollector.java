package org.jdexter.annotation.processor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import org.jdexter.annotation.Conditional;
import org.jdexter.annotation.Configuration;
import org.jdexter.annotation.Decision;
import org.jdexter.annotation.Depends;
import org.jdexter.annotation.Optional;
import org.jdexter.reader.Reader;
import org.jdexter.util.ReflectionUtil;
import org.reflections.ReflectionUtils;

public class AnnotationMetaDataCollector implements MetaDataCollector {
	private Class<?> clazz;
	private Class<? extends Reader> reader;
	private Set<Field> dependencies;
	private Set<Field> optionalDependencies;
	private Method decisionMethod;
	private Set<Field> innerConfigurations;
	private Set<Field> conditionalCongurations;
	
	public AnnotationMetaDataCollector(Class<?> clazz) {
		this.clazz = clazz;
	}
	
	public void collect(){
		if(clazz == null)
			throw new IllegalArgumentException("Invalid parameter null, cannot collect metadata");
		
		Configuration configurationProperties = ReflectionUtil.getAnnotation(clazz, Configuration.class);
		if(configurationProperties == null)
			throw new IllegalArgumentException("Class: " + clazz.getName() + " does not contain annotation: " + Configuration.class.getName());
		
		extractReader(configurationProperties);
		
		extractDependencies();
		
		extractInnerConfigurations();
		
		extractDecisionMethod();
		
		validate();
	}

	@SuppressWarnings("unchecked")
	private void extractDecisionMethod() {
		Set<Method> decisionMethods = ReflectionUtils.getAllMethods(clazz, ReflectionUtils.withAnnotation(Decision.class), 
				ReflectionUtils.withReturnType(boolean.class), 
				ReflectionUtils.withParameters(Class.class));
		
		if(!decisionMethods.isEmpty())
			decisionMethod = (Method) decisionMethods.toArray()[0];
	}

	@SuppressWarnings("unchecked")
	private void extractInnerConfigurations() {
		innerConfigurations = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(Configuration.class));
		conditionalCongurations = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(Configuration.class), ReflectionUtils.withAnnotation(Conditional.class));
		innerConfigurations.removeAll(conditionalCongurations);
	}

	private void validate() {
		if(conditionalCongurations.size() > 0 
				&& decisionMethod == null)
			throw new IllegalArgumentException("No boolean returning method accepting Class<?> as parameter annotated with @Decision");
	}

	@SuppressWarnings("unchecked")
	public void extractDependencies() {
		dependencies = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(Depends.class));
		optionalDependencies = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(Depends.class), ReflectionUtils.withAnnotation(Optional.class));
		dependencies.removeAll(optionalDependencies);
	}

	public void extractReader(Configuration configurationProperties) {
		reader = configurationProperties.readWith();
	}
	
	public static AnnotationMetaDataCollector of(Class<?> clazz){
		AnnotationMetaDataCollector self = new AnnotationMetaDataCollector(clazz);
		self.collect();
		return self;
	}
	
	/* (non-Javadoc)
	 * @see org.jdexter.annotation.processor.MetaDataCollector#getReader()
	 */
	public Class<? extends Reader> getReader() {
		return reader;
	}
	
	/* (non-Javadoc)
	 * @see org.jdexter.annotation.processor.MetaDataCollector#getDependencies()
	 */
	public Set<Field> getDependencies(){
		return dependencies;
	}

	/* (non-Javadoc)
	 * @see org.jdexter.annotation.processor.MetaDataCollector#getOptionalDependencies()
	 */
	public Set<Field> getOptionalDependencies() {
		return optionalDependencies;
	}
	
	/* (non-Javadoc)
	 * @see org.jdexter.annotation.processor.MetaDataCollector#getDecisionMethod()
	 */
	public Method getDecisionMethod(){
		return decisionMethod;
	}

	/* (non-Javadoc)
	 * @see org.jdexter.annotation.processor.MetaDataCollector#getInnerConfigurations()
	 */
	public Set<Field> getInnerConfigurations() {
		return innerConfigurations;
	}

	/* (non-Javadoc)
	 * @see org.jdexter.annotation.processor.MetaDataCollector#getConditionalConfigurations()
	 */
	public Set<Field> getConditionalConfigurations() {
		return conditionalCongurations;
	}
}
