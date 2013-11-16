package org.jdexter.reader;

import static org.testng.Assert.assertNotNull;

import org.jdexter.context.ConfigurationContextUnitTest.TestConfigurationClass;
import org.jdexter.context.ConfigurationContextUnitTest.TestConfigurationClassWithNoReaderSpecified;
import org.jdexter.reader.DefaultReader;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class DefaultReaderUnitTest {

	private DefaultReader nullReader;

	@BeforeMethod
	public void setUp(){
		nullReader = new DefaultReader();
	}

	@Test(expectedExceptions = {IllegalArgumentException.class})
	public void testRead_ShouldThrowIllegalArgumentException_WhenConfigurationClassPassedIsNull() throws Exception{
		nullReader.read(null);
	}
	
	@Test(dataProvider = "dataFor_testRead_ShouldAlwaysThrowUnsupportedOperationException")
	public void testRead_ShouldReturnNonNullInstance(Class<?> configurationClass) throws Exception{
		assertNotNull(nullReader.read(configurationClass));
	}
	
	@DataProvider
	public Object[][] dataFor_testRead_ShouldAlwaysThrowUnsupportedOperationException(){
		return new Object[][]{
				{TestConfigurationClass.class},
				{TestConfigurationClassWithNoReaderSpecified.class},
		};
	}
	
}
