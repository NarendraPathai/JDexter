package org.jdexter.annotation.processor;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Set;

import org.jdexter.annotation.Conditional;
import org.jdexter.annotation.Configuration;
import org.jdexter.annotation.Decision;
import org.jdexter.annotation.Depends;
import org.jdexter.annotation.Optional;
import org.jdexter.context.ConfigurationContextUnitTest.MainConfiguration;
import org.jdexter.context.ConfigurationContextUnitTest.TestCompositeConfigurationWithOneOptionalDependencies;
import org.jdexter.context.ConfigurationContextUnitTest.TestConfigurationClass;
import org.jdexter.context.ConfigurationContextUnitTest.TestConfigurationClass1;
import org.jdexter.context.ConfigurationContextUnitTest.TestConfigurationClassWithNoReaderSpecified;
import org.jdexter.context.ConfigurationContextUnitTest.TestConfigurationClassWithoutConfigurationProperties;
import org.jdexter.context.ConfigurationContextUnitTest.TestReader;
import org.jdexter.context.ConfigurationContextUnitTest.TestRequiresDependency;
import org.jdexter.reader.DefaultReader;
import org.jdexter.reader.Reader;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class AnnotationMetaDataCollectorUnitTest {

	@Test
	public void testOf_ShouldReturnNonNullInstanceOfMetaDataCollector_WhenConfigurationClassPassedContainsConfigurationPropertiesAnnotation(){
		assertNotNull(AnnotationMetaDataCollector.of(TestConfigurationClass.class));
	}
	
	@Test
	public void testOf_ShouldReturnNonNullInstanceOfMetaDataCollector_WhenConfigurationClassPassedContainsConfigurationPropertiesWithDefaultReader(){
		assertNotNull(AnnotationMetaDataCollector.of(TestConfigurationClassWithNoReaderSpecified.class));
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testOf_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties(){
		AnnotationMetaDataCollector.of(TestConfigurationClassWithoutConfigurationProperties.class);
	}
	
	@Test
	public void testOf_ShouldReturnNonNullInstance_WhenConfigurationPropertiesAnnotationIsOnAPrivateConfigurationClass(){
		assertNotNull(AnnotationMetaDataCollector.of(TestConfigurationClassWithPrivateAccess.class));
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testCollect_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties(){
		AnnotationMetaDataCollector mdc = new AnnotationMetaDataCollector(TestConfigurationClassWithoutConfigurationProperties.class);
		mdc.collect();
	}
	
	@Test(dependsOnMethods = {
			"testOf_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties",
			"testCollect_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties"},
			dataProvider = "dataFor_testGetReader_ShouldReturnProperReaderClass_WhenConfigurationPropertiesAnnotationIsFound")
	public void testGetReader_ShouldReturnNonNullReaderClass_WhenConfigurationPropertiesAnnotationIsFound(Class<?> configurationClass,
			Class<? extends Reader> reader){
		assertNotNull(AnnotationMetaDataCollector.of(configurationClass).getReader());
	}
	
	@Test(dependsOnMethods = {
			"testOf_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties",
			"testCollect_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties"},
			dataProvider = "dataFor_testGetReader_ShouldReturnProperReaderClass_WhenConfigurationPropertiesAnnotationIsFound")
	public void testGetReader_ShouldReturnExactReaderClass_WhenConfigurationPropertiesAnnotationIsFound(Class<?> configurationClass,
			Class<? extends Reader> reader){
		assertEquals(AnnotationMetaDataCollector.of(configurationClass).getReader(),reader);
	}
	
	private static final int ANY = 5;
	
	@Test(invocationCount = ANY)
	public void testOf_ShouldReturnNewInstanceOnEachInvocation_WhenSameConfigurationClassIsPassed(){
		assertNotEquals(AnnotationMetaDataCollector.of(TestConfigurationClass.class), AnnotationMetaDataCollector.of(TestConfigurationClass.class));
	}
	
	@Test(invocationCount = ANY)
	public void testOf_ShouldReturnNewInstanceOnEachInvocation_WhenDifferentConfigurationClassIsPassed(){
		assertNotEquals(AnnotationMetaDataCollector.of(TestConfigurationClass.class), AnnotationMetaDataCollector.of(TestConfigurationClass1.class));
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testCollect_ShouldThrowIllegalArgumentException_WhenNullConfigurationClassIsPassed(){
		AnnotationMetaDataCollector mdc = new AnnotationMetaDataCollector(null);
		mdc.collect();
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testOf_ShouldThrowIllegalArgumentException_WhenNullConfigurationClassIsPassedAsParameter(){
		AnnotationMetaDataCollector.of(null);
	}
	
	
	@Test
	public void testOf_ShouldExtractTheDependencies(){
		MetaDataCollector collector = AnnotationMetaDataCollector.of(TestRequiresDependency.class);
		assertEquals(1, collector.getDependencies().size());
		
		collector = AnnotationMetaDataCollector.of(TestRequiresDependencyWithPublicAccess.class);
		assertEquals(1, collector.getDependencies().size());
		
		collector = AnnotationMetaDataCollector.of(TestRequiresDependencyWithProtectedAccess.class);
		assertEquals(1, collector.getDependencies().size());
		
		collector = AnnotationMetaDataCollector.of(TestRequiresDependencyWithDefaultAccess.class);
		assertEquals(1, collector.getDependencies().size());
	}
		
	@Test
	public void testOf_ShouldReturnSetWithProperFields_WhenConfigurationClassWithPrivateDependentFields(){
		MetaDataCollector collector = AnnotationMetaDataCollector.of(TestRequiresDependency.class);
		Set<Field> requiredConfiguration = collector.getDependencies();
		Iterator<Field> itr = requiredConfiguration.iterator();
		while(itr.hasNext()){
			Field f = itr.next();
			if(f.getName().equals("dependency")){
				itr.remove();
			}
		}
		
		assertEquals(0, requiredConfiguration.size());
	}

	@Test
	public void testOf_ShouldReadTheOptionalDependencies(){
		MetaDataCollector collector = AnnotationMetaDataCollector.of(TestConfigurationClass.class);
		Set<Field> optionallyRequiredConfigurations = collector.getOptionalDependencies();
		assertEquals(optionallyRequiredConfigurations.size(), 0);
		
		collector = AnnotationMetaDataCollector.of(TestCompositeConfigurationWithOneOptionalDependencies.class);
		optionallyRequiredConfigurations = collector.getOptionalDependencies();
		assertEquals(optionallyRequiredConfigurations.size(), 1);
		
		collector = AnnotationMetaDataCollector.of(TestCompositeConfigurationWithTwoOptionalDependencies.class);
		optionallyRequiredConfigurations = collector.getOptionalDependencies();
		assertEquals(optionallyRequiredConfigurations.size(), 2);
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testOf_ShouldThrowIllegalArgumentException_WhenConfigurationClassWithConditionalConfigurationAndNoDecisionMethodIsPassed(){
		AnnotationMetaDataCollector.of(TestCompositeConfigurationWithConditionalConfigurationAndNoDecisionMethod.class);
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class}, dataProvider = "dataFor_testOf_ShouldThrowIllegalArgumentException_WhenDependenciesAreNotProperlyStated")
	public void testOf_ShouldThrowIllegalArgumentException_WhenDependenciesAreNotProperlyStated(Class<?> configurationClass){
		AnnotationMetaDataCollector.of(configurationClass);
	}
	
	@Test
	public void testOf_ShouldReturnEmptySetOfInnerConfigurationsForSimpleConfigurationClass(){
		MetaDataCollector mdc = AnnotationMetaDataCollector.of(TestConfigurationClass.class);
		Assert.assertTrue(mdc.getInnerConfigurations().isEmpty());
	}
	
	@Test
	public void testOf_ShouldReturnEmptySetOfConditionalInnerConfigurationsForSimpleConfigurationClass(){
		MetaDataCollector mdc = AnnotationMetaDataCollector.of(TestConfigurationClass.class);
		Assert.assertTrue(mdc.getConditionalConfigurations().isEmpty());
	}
	
	@Test
	public void testOf_ShouldReturnSetOfInnerConfigurationsForCompositeConfigurationClass(){
		MetaDataCollector mdc = AnnotationMetaDataCollector.of(TestConfigurationClass.class);
		assertEquals(mdc.getInnerConfigurations().size(), 0);
		
		mdc = AnnotationMetaDataCollector.of(MainConfiguration.class);
		assertEquals(mdc.getInnerConfigurations().size(), 1);
	}
	
	@Test
	public void testOf_ShouldReturnSetOfConditionalInnerConfigurationsForCompositeConfigurationClass(){
		MetaDataCollector mdc = AnnotationMetaDataCollector.of(TestConfigurationClass.class);
		assertEquals(mdc.getConditionalConfigurations().size(), 0);
		
		mdc = AnnotationMetaDataCollector.of(MainConfiguration.class);
		assertEquals(mdc.getConditionalConfigurations().size(), 1);
	}
	
	
	
	@DataProvider 
	public static Object[][] dataFor_testOf_ShouldThrowIllegalArgumentException_WhenDependenciesAreNotProperlyStated(){
		return new Object[][]{
				{TestCompositeConfigurationWithConditionalConfigurationAndDecisionMethodReturningNonBooleanValue.class},
				{TestCompositeConfigurationWithConditionalConfigurationAndDecisionMethodReturningVoid.class},
				{TestCompositeConfigurationWithConditionalConfigurationAndNoDecisionMethod.class},
				{TestCompositeConfigurationWithConditionalConfigurationAndDecisionMethodWithBlankParameters.class},
				{TestCompositeConfigurationWithConditionalConfigurationAndDecisionMethodWithMoreThanOneParameters.class}
		};
	}
	
	
	@Configuration
	public static class TestCompositeConfigurationWithTwoOptionalDependencies{
		@Depends TestConfigurationClass dependency1;
		@Depends @Optional TestConfigurationClass1 dependency2;
		@Depends @Optional TestConfigurationClass1 dependency3;
	}
	
	@Configuration
	public static class TestCompositeConfigurationWithConditionalConfigurationAndDecisionMethodReturningNonBooleanValue{
		@Configuration TestConfigurationClass dependency1;
		@Configuration @Conditional TestConfigurationClass1 dependency2;
		@Configuration @Conditional TestConfigurationClass1 dependency3;
		
		@Decision
		public Object eligible(Class<?> classToRead){
			return false;
		}
	}
	
	@Configuration
	public static class TestCompositeConfigurationWithConditionalConfigurationAndDecisionMethodReturningVoid{
		@Configuration TestConfigurationClass dependency1;
		@Configuration @Conditional TestConfigurationClass1 dependency2;
		@Configuration @Conditional TestConfigurationClass1 dependency3;
		
		@Decision
		public void eligible(Class<?> classToRead){
		}
	}
	
	@Configuration
	public static class TestCompositeConfigurationWithConditionalConfigurationAndDecisionMethodWithBlankParameters{
		@Configuration TestConfigurationClass dependency1;
		@Configuration @Conditional TestConfigurationClass1 dependency2;
		@Configuration @Conditional TestConfigurationClass1 dependency3;
		
		@Decision
		public void eligible(Class<?> classToRead){
		}
	}
	
	@Configuration
	public static class TestCompositeConfigurationWithConditionalConfigurationAndDecisionMethodWithMoreThanOneParameters{
		@Configuration TestConfigurationClass dependency1;
		@Configuration @Conditional TestConfigurationClass1 dependency2;
		@Configuration @Conditional TestConfigurationClass1 dependency3;
		
		@Decision
		public void eligible(Class<?> classToRead){
		}
	}
	
	@Configuration
	public static class TestCompositeConfigurationWithConditionalConfigurationAndNoDecisionMethod{
		@Configuration TestConfigurationClass dependency1;
		@Configuration @Conditional TestConfigurationClass1 dependency2;
		@Configuration TestConfigurationClass1 dependency3;
	}
	
	@DataProvider
	public Object[][] dataFor_testGetReader_ShouldReturnProperReaderClass_WhenConfigurationPropertiesAnnotationIsFound(){
		return new Object[][]{
				{TestConfigurationClass.class,						TestReader.class},
				{TestConfigurationClassWithNoReaderSpecified.class,	DefaultReader.class}
		};
	}
	
	@Configuration
	private class TestConfigurationClassWithPrivateAccess{
	}
	
	@Configuration
	public static class TestRequiresDependencyWithPublicAccess{
		@Depends public TestConfigurationClass dependency;
	}
	
	@Configuration
	public static class TestRequiresDependencyWithProtectedAccess{
		@Depends protected TestConfigurationClass dependency;
	}
	
	@Configuration
	public static class TestRequiresDependencyWithDefaultAccess{
		@Depends TestConfigurationClass dependency;
	}
}
