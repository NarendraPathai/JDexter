package org.jdexter.annotation.processor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jdexter.annotation.Conditional;
import org.jdexter.annotation.Configuration;
import org.jdexter.annotation.Decision;
import org.jdexter.annotation.Depends;
import org.jdexter.annotation.Optional;
import org.jdexter.reader.Reader;
import org.jdexter.util.Maps;
import org.jdexter.util.ReflectionUtil;
import org.reflections.ReflectionUtils;

import com.google.common.base.Function;

public class AnnotationMetaDataCollector implements MetaDataCollector {
	private Class<?> clazz;
	private Class<? extends Reader> reader;
	private Set<Field> dependencies;
	private Set<Field> optionalDependencies;
	private Method decisionMethod;
	private Set<Field> innerConfigurations;
	
	private Map<String, FieldConditionalAnnotationEntry> conditionalConfigurationFieldNameToConditionalAnnotation;
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
		
		conditionalConfigurationFieldNameToConditionalAnnotation = 
			Maps.asHashMap(conditionalCongurations, new FieldNameFunction(), new FieldToConditionalAnnotationEntryFunction());
	}

	private void validate() {
		if(!conditionalConfigurationFieldNameToConditionalAnnotation.isEmpty() 
				&& decisionMethod == null)
			throw new IllegalArgumentException("No boolean returning method accepting Class<?> as parameter annotated with @Decision");
		
		validateConditionalConfigurationDependencies();
	}

	private void validateConditionalConfigurationDependencies() {
		for(FieldConditionalAnnotationEntry entry : conditionalConfigurationFieldNameToConditionalAnnotation.values()){
			String[] conditionalFieldNames = entry.getAnnotation().dependsOn();

			Set<Field> dependencies = new HashSet<Field>();

			for(String fieldName : conditionalFieldNames){
				FieldConditionalAnnotationEntry dependentEntry = conditionalConfigurationFieldNameToConditionalAnnotation.get(fieldName);

				checkArgument(dependentEntry != null, fieldName + " provided as dependency of " + entry.getField().getName()
						+ " is not a conditional configuration.");
				
				checkArgument(dependentEntry != entry, "Field " + fieldName + " dependent on itself.");

				dependencies.add(dependentEntry.getField());
			}
			
			entry.setConditionalConfigurationDependencies(dependencies);
		}
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

	public Set<Field> getDependenciesForConditionalConfiguration(Field field) {
		checkNotNull(field, "field should not be null");
		
		FieldConditionalAnnotationEntry entry = conditionalConfigurationFieldNameToConditionalAnnotation.get(field.getName());
		
		checkArgument(entry != null, field.getName() + " is not a conditional configuration");
		
		return entry.getConditionalConfigurationDependencies();
	}
	
	static class FieldNameFunction implements Function<Field, String>{
		
		public String apply(Field input) {
			return input.getName();
		}
	}
	
	static class FieldToConditionalAnnotationEntryFunction implements Function<Field, FieldConditionalAnnotationEntry>{
		public FieldConditionalAnnotationEntry apply(Field input) {
			return new FieldConditionalAnnotationEntry(input, input.getAnnotation(Conditional.class));
		}
	}
	
	private static class FieldConditionalAnnotationEntry extends FieldAnnotationEntry<Conditional>{
		private Set<Field> conditionalConfigurationDependencies;
		
		public FieldConditionalAnnotationEntry(Field field, Conditional annotation) {
			super(field, annotation);
		}
		
		public Set<Field> getConditionalConfigurationDependencies() {
			return conditionalConfigurationDependencies;
		}
		
		public void setConditionalConfigurationDependencies(Set<Field> dependencies){
			this.conditionalConfigurationDependencies = dependencies;
		}
	}
	
	private static class FieldAnnotationEntry<T extends Annotation>{
		private Field field;
		private T annotation;
		
		FieldAnnotationEntry(Field field, T annotation) {
			this.annotation = annotation;
			this.field = field;
		}
		
		public Field getField() {
			return field;
		}
		
		public T getAnnotation() {
			return annotation;
		}
	}
	
}
