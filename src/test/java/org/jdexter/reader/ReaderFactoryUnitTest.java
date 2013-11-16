package org.jdexter.reader;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

import org.jdexter.context.ConfigurationContextIntegrationTest.ReaderWithNoDefaultConstructor;
import org.jdexter.context.ConfigurationContextIntegrationTest.ReaderWithPackageProtectedConstructor;
import org.jdexter.context.ConfigurationContextIntegrationTest.ReaderWithPrivateConstructor;
import org.jdexter.context.ConfigurationContextIntegrationTest.ReaderWithProtectedConstructor;
import org.jdexter.context.ConfigurationContextUnitTest.ReaderWhichThrowsCheckedExceptionWhileReading;
import org.jdexter.context.ConfigurationContextUnitTest.ReaderWhichThrowsErrorWhileReading;
import org.jdexter.context.ConfigurationContextUnitTest.ReaderWhichThrowsUnCheckedExceptionWhileReading;
import org.jdexter.context.ConfigurationContextUnitTest.TestReader;
import org.jdexter.reader.Reader;
import org.jdexter.reader.ReaderFactory;
import org.jdexter.reader.exception.ReaderInstantiationException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ReaderFactoryUnitTest {
	
	private ReaderFactory readerFactory;

	@BeforeMethod
	public void setUp(){
		readerFactory = new ReaderFactory();
	}
	
	@Test(dataProvider = "dataFor_testGetInstanceOf_ShouldReturnNonNullInstanceOfReader")
	public void testGetInstanceOf_ShouldReturnNonNullInstanceOfReader(Class<? extends Reader> readerClass) throws ReaderInstantiationException{
		assertNotNull(readerFactory.getInstanceOf(readerClass));
	}
	
	@Test(dataProvider = "dataFor_testGetInstanceOf_ShouldThrowExactExceptionOccuredWhileInstantiationOfReader")
	public void testGetInstanceOf_ShouldThrowExactExceptionOccuredWhileInstantiationOfReader(Class<? extends Reader> readerClass, Class<?> expectedException){
		try{
			readerFactory.getInstanceOf(readerClass);
			fail("Excepted exception: " + expectedException.getName() + " was not thrown");
		}catch (ReaderInstantiationException e) {
			assertEquals(e.getCause().getClass(), expectedException);
		}
	}
	
	@DataProvider
	public Object[][] dataFor_testGetInstanceOf_ShouldReturnNonNullInstanceOfReader(){
		return new Object[][]{
				{TestReader.class},
				{ReaderWhichThrowsUnCheckedExceptionWhileReading.class},
				{ReaderWhichThrowsCheckedExceptionWhileReading.class},
				{ReaderWhichThrowsErrorWhileReading.class}
		};
	}
	
	@DataProvider
	public Object[][] dataFor_testGetInstanceOf_ShouldThrowExactExceptionOccuredWhileInstantiationOfReader(){
		return new Object[][]{
				{null,															IllegalArgumentException.class},
				{ReaderWithNoDefaultConstructor.class,							NoSuchMethodException.class},
				{ReaderWithProtectedConstructor.class,							NoSuchMethodException.class},
				{ReaderWithPackageProtectedConstructor.class,					NoSuchMethodException.class},
				{ReaderWithPrivateConstructor.class,							NoSuchMethodException.class},
				{ReaderThrowingCheckedExceptionFromDefaultConstructor.class,	Exception.class},
				{ReaderThrowingUnCheckedExceptionFromDefaultConstructor.class,	RuntimeException.class},
				{ReaderThrowingErrorFromDefaultConstructor.class,				Error.class}
		};
	}
	
	public static class ReaderThrowingCheckedExceptionFromDefaultConstructor extends Reader{
		public ReaderThrowingCheckedExceptionFromDefaultConstructor() throws Exception {
			throw new Exception("This can be any checked exception");
		}
		
		@Override
		public Object read(Class<?> classToRead) throws Exception {
			return null;
		}
	}
	
	public static class ReaderThrowingUnCheckedExceptionFromDefaultConstructor extends Reader{
		public ReaderThrowingUnCheckedExceptionFromDefaultConstructor(){
			throw new RuntimeException("This can be any unchecked exception");
		}
		
		@Override
		public Object read(Class<?> classToRead) throws Exception {
			return null;
		}
	}
	
	public static class ReaderThrowingErrorFromDefaultConstructor extends Reader{
		public ReaderThrowingErrorFromDefaultConstructor() throws Exception {
			throw new Error("This can be any error");
		}
		
		@Override
		public Object read(Class<?> classToRead) throws Exception {
			return null;
		}
	}
	
}
