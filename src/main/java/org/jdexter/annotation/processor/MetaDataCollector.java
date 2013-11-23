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

public class MetaDataCollector {
	private Class<?> clazz;
	private Class<? extends Reader> reader;
	private Set<Field> requiredConfigurations;
	private Set<Field> optionallyRequiredConfigurations;
	private Method decisionMethod;
	private Set<Field> innerConfigurations;
	private Set<Field> conditionalCongurations;
	
	public MetaDataCollector(Class<?> clazz) {
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
	}

	private void validate() {
		if(conditionalCongurations.size() > 0 
				&& decisionMethod == null)
			throw new IllegalArgumentException("No boolean returning method accepting Class<?> as parameter annotated with @Decision");
	}

	@SuppressWarnings("unchecked")
	public void extractDependencies() {
		requiredConfigurations = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(Depends.class));
		optionallyRequiredConfigurations = ReflectionUtils.getAllFields(clazz, ReflectionUtils.withAnnotation(Depends.class), ReflectionUtils.withAnnotation(Optional.class));
		requiredConfigurations.removeAll(optionallyRequiredConfigurations);
	}

	public void extractReader(Configuration configurationProperties) {
		reader = configurationProperties.readWith();
	}
	
	public static MetaDataCollector of(Class<?> clazz){
		MetaDataCollector self = new MetaDataCollector(clazz);
		self.collect();
		return self;
	}
	
	public Class<? extends Reader> getReader() {
		return reader;
	}
	
	public Set<Field> getRequiredConfigurations(){
		return requiredConfigurations;
	}

	public Set<Field> getOptionallyRequiredConfigurations() {
		return optionallyRequiredConfigurations;
	}
	
	public Method getDecisionMethod(){
		return decisionMethod;
	}

	public Set<Field> getInnerConfigurations() {
		return innerConfigurations;
	}

	public Set<Field> getConditionalConfigurations() {
		return conditionalCongurations;
	}
}
