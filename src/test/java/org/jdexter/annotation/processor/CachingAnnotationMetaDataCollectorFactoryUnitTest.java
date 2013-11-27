package org.jdexter.annotation.processor;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.jdexter.annotation.processor.AnnotationMetaDataCollectorUnitTest.TestCompositeConfigurationWithConditionalConfigurationAndNoDecisionMethod;
import org.jdexter.context.ConfigurationContextUnitTest.TestConfigurationClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CachingAnnotationMetaDataCollectorFactoryUnitTest {

	private CachingAnnotationMetaDataCollectorFactory factory;

	@BeforeMethod
	public void setUp(){
		factory = new CachingAnnotationMetaDataCollectorFactory();
	}
	
	@Test
	public void testCreate_ShouldCacheCollectorInstanceOnSuccessfulCreation(){
		factory.create(TestConfigurationClass.class);
		assertNotNull(factory.cacheQuery(TestConfigurationClass.class));
	}
	
	@Test
	public void testCreate_ShouldReturnCachedInstanceOnSuccessiveCall(){
		Object o1 = factory.create(TestConfigurationClass.class);
		Object o2 = factory.create(TestConfigurationClass.class);
		assertTrue(o1 == o2);
	}
	
	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testCreate_ShouldRethrowExceptionIfThrownMyCollector(){
		factory.create(TestCompositeConfigurationWithConditionalConfigurationAndNoDecisionMethod.class);
	}
	
	@Test(dependsOnMethods = {"testCreate_ShouldRethrowExceptionIfThrownMyCollector"})
	public void testCreate_ShouldReturnNullForClassWhoseCollectorThrewException(){
		try{
			factory.create(TestCompositeConfigurationWithConditionalConfigurationAndNoDecisionMethod.class);
		}catch(IllegalArgumentException ex){
			//chomp
		}
		assertNull(factory.cacheQuery(TestCompositeConfigurationWithConditionalConfigurationAndNoDecisionMethod.class));
	}
}
