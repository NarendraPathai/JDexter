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

public class MetaDataCollectorUnitTest {

	@Test
	public void testOf_ShouldReturnNonNullInstanceOfMetaDataCollector_WhenConfigurationClassPassedContainsConfigurationPropertiesAnnotation(){
		assertNotNull(MetaDataCollector.of(TestConfigurationClass.class));
	}
	
	@Test
	public void testOf_ShouldReturnNonNullInstanceOfMetaDataCollector_WhenConfigurationClassPassedContainsConfigurationPropertiesWithDefaultReader(){
		assertNotNull(MetaDataCollector.of(TestConfigurationClassWithNoReaderSpecified.class));
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testOf_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties(){
		MetaDataCollector.of(TestConfigurationClassWithoutConfigurationProperties.class);
	}
	
	@Test
	public void testOf_ShouldReturnNonNullInstance_WhenConfigurationPropertiesAnnotationIsOnAPrivateConfigurationClass(){
		assertNotNull(MetaDataCollector.of(TestConfigurationClassWithPrivateAccess.class));
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testCollect_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties(){
		MetaDataCollector mdc = new MetaDataCollector(TestConfigurationClassWithoutConfigurationProperties.class);
		mdc.collect();
	}
	
	@Test(dependsOnMethods = {
			"testOf_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties",
			"testCollect_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties"},
			dataProvider = "dataFor_testGetReader_ShouldReturnProperReaderClass_WhenConfigurationPropertiesAnnotationIsFound")
	public void testGetReader_ShouldReturnNonNullReaderClass_WhenConfigurationPropertiesAnnotationIsFound(Class<?> configurationClass,
			Class<? extends Reader> reader){
		assertNotNull(MetaDataCollector.of(configurationClass).getReader());
	}
	
	@Test(dependsOnMethods = {
			"testOf_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties",
			"testCollect_ShouldThrowIllegalArgumentException_WhenConfigurationClassIsNotAnnotatedWithConfigurationProperties"},
			dataProvider = "dataFor_testGetReader_ShouldReturnProperReaderClass_WhenConfigurationPropertiesAnnotationIsFound")
	public void testGetReader_ShouldReturnExactReaderClass_WhenConfigurationPropertiesAnnotationIsFound(Class<?> configurationClass,
			Class<? extends Reader> reader){
		assertEquals(MetaDataCollector.of(configurationClass).getReader(),reader);
	}
	
	private static final int ANY = 5;
	
	@Test(invocationCount = ANY)
	public void testOf_ShouldReturnNewInstanceOnEachInvocation_WhenSameConfigurationClassIsPassed(){
		assertNotEquals(MetaDataCollector.of(TestConfigurationClass.class), MetaDataCollector.of(TestConfigurationClass.class));
	}
	
	@Test(invocationCount = ANY)
	public void testOf_ShouldReturnNewInstanceOnEachInvocation_WhenDifferentConfigurationClassIsPassed(){
		assertNotEquals(MetaDataCollector.of(TestConfigurationClass.class), MetaDataCollector.of(TestConfigurationClass1.class));
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testCollect_ShouldThrowIllegalArgumentException_WhenNullConfigurationClassIsPassed(){
		MetaDataCollector mdc = new MetaDataCollector(null);
		mdc.collect();
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testOf_ShouldThrowIllegalArgumentException_WhenNullConfigurationClassIsPassedAsParameter(){
		MetaDataCollector.of(null);
	}
	
	@Test
	public void testOf_ShouldReturnSetWithProperSize_WhenConfigurationClassWithPrivateDependentFields(){
		MetaDataCollector collector = MetaDataCollector.of(TestRequiresDependency.class);
		assertEquals(1, collector.getRequiredConfigurations().size());
	}
	
	@Test
	public void testOf_ShouldReturnSetWithProperSize_WhenConfigurationClassWithPublicDependentFields(){
		MetaDataCollector collector = MetaDataCollector.of(TestRequiresDependencyWithPublicAccess.class);
		assertEquals(1, collector.getRequiredConfigurations().size());
	}
	
	@Test
	public void testOf_ShouldReturnSetWithProperSize_WhenConfigurationClassWithProtectedDependentFields(){
		MetaDataCollector collector = MetaDataCollector.of(TestRequiresDependencyWithProtectedAccess.class);
		assertEquals(1, collector.getRequiredConfigurations().size());
	}
	
	@Test
	public void testOf_ShouldReturnSetWithProperSize_WhenConfigurationClassWithDefaultAccessDependentFields(){
		MetaDataCollector collector = MetaDataCollector.of(TestRequiresDependencyWithDefaultAccess.class);
		assertEquals(1, collector.getRequiredConfigurations().size());
	}
	
	@Test
	public void testOf_ShouldReturnSetWithProperFields_WhenConfigurationClassWithPrivateDependentFields(){
		MetaDataCollector collector = MetaDataCollector.of(TestRequiresDependency.class);
		Set<Field> requiredConfiguration = collector.getRequiredConfigurations();
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
		MetaDataCollector collector = MetaDataCollector.of(TestCompositeConfigurationWithOneOptionalDependencies.class);
		Set<Field> optionallyRequiredConfigurations = collector.getOptionallyRequiredConfigurations();
		assertEquals(optionallyRequiredConfigurations.size(), 1);
		
		collector = MetaDataCollector.of(TestCompositeConfigurationWithTwoOptionalDependencies.class);
		optionallyRequiredConfigurations = collector.getOptionallyRequiredConfigurations();
		assertEquals(optionallyRequiredConfigurations.size(), 2);
	}
	
	@Test
	public void testOf_ShouldNotThrowAnyException_WhenConfigurationClassWithNoOptionalDependencyIsPassed(){
		MetaDataCollector collector = MetaDataCollector.of(TestConfigurationClass.class);
		Set<Field> optionallyRequiredConfigurations = collector.getOptionallyRequiredConfigurations();
		assertEquals(optionallyRequiredConfigurations.size(), 0);
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testOf_ShouldThrowIllegalArgumentException_WhenConfigurationClassWithConditionalConfigurationAndNoDecisionMethodIsPassed(){
		MetaDataCollector.of(TestCompositeConfigurationWithConditionalConfigurationAndNoDecisionMethod.class);
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class}, dataProvider = "dataFor_testOf_ShouldThrowIllegalArgumentException_WhenDependenciesAreNotProperlyStated")
	public void testOf_ShouldThrowIllegalArgumentException_WhenDependenciesAreNotProperlyStated(Class<?> configurationClass){
		MetaDataCollector.of(configurationClass);
	}
	
	@Test
	public void testOf_ShouldReturnEmptySetOfInnerConfigurationsForSimpleConfigurationClass(){
		MetaDataCollector mdc = MetaDataCollector.of(TestConfigurationClass.class);
		Assert.assertTrue(mdc.getInnerConfigurations().isEmpty());
	}
	
	@Test
	public void testOf_ShouldReturnEmptySetOfConditionalInnerConfigurationsForSimpleConfigurationClass(){
		MetaDataCollector mdc = MetaDataCollector.of(TestConfigurationClass.class);
		Assert.assertTrue(mdc.getConditionalConfigurations().isEmpty());
	}
	
	@Test
	public void testOf_ShouldReturnSetOfInnerConfigurationsForCompositeConfigurationClass(){
		MetaDataCollector mdc = MetaDataCollector.of(MainConfiguration.class);
		assertEquals(mdc.getInnerConfigurations().size(), 2);
	}
	
	@Test
	public void testOf_ShouldReturnSetOfConditionalInnerConfigurationsForCompositeConfigurationClass(){
		MetaDataCollector mdc = MetaDataCollector.of(MainConfiguration.class);
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
