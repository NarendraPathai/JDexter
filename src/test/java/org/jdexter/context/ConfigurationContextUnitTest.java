package org.jdexter.context;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import javax.xml.bind.annotation.XmlRootElement;

import org.jdexter.annotation.Conditional;
import org.jdexter.annotation.Configuration;
import org.jdexter.annotation.Decision;
import org.jdexter.annotation.Depends;
import org.jdexter.annotation.Optional;
import org.jdexter.annotation.PostRead;
import org.jdexter.annotation.PreRead;
import org.jdexter.context.data.TestConfigurationClassWithPackageProtectedConstructor;
import org.jdexter.context.data.TestConfigurationContextClassWithProtectedConstructor;
import org.jdexter.exception.ReadConfigurationException;
import org.jdexter.reader.JAXBReader;
import org.jdexter.reader.Reader;
import org.jdexter.util.ReflectionUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


//TODO Reader class is inner class with non static
//TODO PostRead should not be called when pre read throws an exception
//TODO before the call to decision callback the other dependencies should be injected
//TODO what happens when an non optional is dependent on an optional configuration? Is that feasible?
//TODO when optional configuration reading fails in read call for first time, the context again tries to read it on consecutive calls to read.
//TODO the test cases for optionally depends, dependency will remain null if context fails to read it
//TODO ordering when a conditional dependency is dependent on other conditional dependency

public class ConfigurationContextUnitTest {

	private ConfigurationContext configurationContext;

	@BeforeMethod
	public void setUp(){
		configurationContext = new ConfigurationContext();
	}

	@Test(dataProvider = "dataFor_testRead_ShouldReturn_InstanceOfClassPassedToIt")
	public void testRead_ShouldReturn_InstanceOfClassPassedToIt(Class<?> configurationClassToRead) throws ReadConfigurationException{
		Object instance = configurationContext.read(configurationClassToRead);

		assertEquals(instance.getClass(), configurationClassToRead);
	}

	@Test(expectedExceptions = {ReadConfigurationException.class})
	public void tesRead_ShouldThrowException_OnPassingInterfaceForReading() throws ReadConfigurationException{
		configurationContext.read(TestConfigurationInterface.class);
	}

	@Test(dataProvider="dataFor_testRead_ShouldThrowProperlyWrappedExceptions_IfAnyProblemInReading")
	public void testRead_ShouldThrowProperlyWrappedExceptions_IfAnyProblemInDefaultConstructor(Class<?> configurationClassToRead, Class<?> expectedWrappedException){
		ReadConfigurationException rce = null;
		try{
			configurationContext.read(configurationClassToRead);
		}catch(ReadConfigurationException rce1){
			rce = rce1;
		}
		assertNotNull(rce,"This method should only be used when exception is supposed to be thrown");
		assertEquals(rce.getCause().getClass(), expectedWrappedException);
	}

	@Test
	public void testRead_ShouldReturnProperCastedInstance_WhenAnyConfigurationClassIsRead() throws ReadConfigurationException{
		assertNotNull(configurationContext.read(TestConfigurationClass.class));
	}

	@Test
	public void testRead_ShouldCallDefaultConstructor_WhenMultipleConstructorsAreAvailable() throws ReadConfigurationException{
		TestConfigurationClassWithMultipleConstructorsWithNonDefaultConstructorThrowingException instance = configurationContext.read(TestConfigurationClassWithMultipleConstructorsWithNonDefaultConstructorThrowingException.class);
		assertNotNull(instance);
	}

	@Test
	public void testRead_ShouldCallPreReadLifeCycleEvent() throws ReadConfigurationException{
		TestConfigurationClassWithAllLifeCycleAnnotations instance = configurationContext.read(TestConfigurationClassWithAllLifeCycleAnnotations.class);
		assertTrue(instance.isPreReadCalled(),"Pre read life cycle event not being called");
	}

	@Test
	public void testRead_ShouldCallPreReadLifeCycleEventOnce() throws ReadConfigurationException{
		TestConfigurationClassWithAllLifeCycleAnnotations instance = configurationContext.read(TestConfigurationClassWithAllLifeCycleAnnotations.class);
		assertEquals(instance.getPreReadCallCount(), 1, "Pre Read life cycle event called more than once, actual count:" + instance.getPreReadCallCount());
	}
	
	@Test
	public void testRead_ShouldCallPostReadLifeCycleEventOnce() throws ReadConfigurationException{
		TestConfigurationClassWithAllLifeCycleAnnotations instance = configurationContext.read(TestConfigurationClassWithAllLifeCycleAnnotations.class);
		assertEquals(instance.getPostReadCallCount(), 1, "Post Read life cycle event called more than once, actual count:" + instance.getPostReadCallCount());
	}
	
	@Test
	public void testRead_ShouldCallPostReadLifeCycleEvent() throws ReadConfigurationException{
		TestConfigurationClassWithAllLifeCycleAnnotations instance = configurationContext.read(TestConfigurationClassWithAllLifeCycleAnnotations.class);
		assertTrue(instance.isPostReadCalled(),"Post read life cycle event not being called");
	}

	@Test
	public void testRead_ShouldCallPreReadLifeCycleEventBeforePostReadLifeCycleEvent() throws ReadConfigurationException{
		TestConfigurationClassWithAllLifeCycleAnnotations instance = configurationContext.read(TestConfigurationClassWithAllLifeCycleAnnotations.class);
		assertTrue(instance.isPreCalledBeforePost(), "Pre life cycle event must be called before Post read");
	}

	@Test(dependsOnMethods = {
			"testRead_ShouldCallPreReadLifeCycleEvent",
			"testRead_ShouldCallPostReadLifeCycleEvent",
	"testRead_ShouldCallPreReadLifeCycleEventBeforePostReadLifeCycleEvent"})
	public void testRead_ShouldSkipLifeCycleEvents_WhenNoMethodsFound() throws ReadConfigurationException{
		TestConfigurationClassWithNoLifeCycleMethod instance = configurationContext.read(TestConfigurationClassWithNoLifeCycleMethod.class);
		assertNotNull(instance);
	}

	@Test(dependsOnMethods = {"testRead_ShouldCallPreReadLifeCycleEvent"})
	public void testRead_ShouldSkipPreLifeCycleEvents_WhenNoMethodsFound() throws ReadConfigurationException{
		TestConfigurationClassWithNoPreReadLifeCycleMethod instance = configurationContext.read(TestConfigurationClassWithNoPreReadLifeCycleMethod.class);
		assertNotNull(instance);
	}

	@Test(dependsOnMethods = {"testRead_ShouldCallPostReadLifeCycleEvent"})
	public void testRead_ShouldSkipPostLifeCycleEvents_WhenNoMethodsFound() throws ReadConfigurationException{
		TestConfigurationClassWithNoPostReadLifeCycleMethod instance = configurationContext.read(TestConfigurationClassWithNoPostReadLifeCycleMethod.class);
		assertNotNull(instance);
	}
	
	@Test
	public void testRead_ShouldInjectAlreadyReadSingleDependencyBeforePostReadLifeCycleEvent() throws ReadConfigurationException{
		configurationContext.read(TestConfigurationClass.class);
		configurationContext.read(TestRequiresDependency.class);
	}
	
	@Test
	public void testRead_ShouldInjectUnReadSingleDependencyPostReadLifeCycleEvent() throws ReadConfigurationException{
		configurationContext.read(TestRequiresDependency.class);
	}
	
	@Test(dataProvider = "dataFor_testRead_ShouldThrowProperException_WhenDependencyThrowsSomeException")
	public void testRead_ShouldThrowProperException_WhenDependencyThrowsSomeException(Class<?> configurationClass, Class<?> expectedException) throws ReadConfigurationException{
		try{
			configurationContext.read(configurationClass);
			fail("Expected to throw: " + expectedException);
		}catch(ReadConfigurationException rce){
			assertEquals(rce.getCause().getClass(), expectedException);
		}
	}
	
	@Test
	public void testRead_MustSaveTheReadConfiguration() throws ReadConfigurationException{
		configurationContext.read(TestConfigurationClass.class);
		assertNotNull(configurationContext.fetch(TestConfigurationClass.class));
	}

	/**
	 * Is unable to detect circular dependencies
	 * @throws ReadConfigurationException
	 */
	@Test(expectedExceptions = {ReadConfigurationException.class})
	public void testRead_IsUnableToDetectCircularDependencies() throws ReadConfigurationException{
		configurationContext.read(TestCircularDependency1.class);
	}
	
	@Test
	public void testRead_ShouldBeAbleToReadCompositeConfigurationWithDefaultReader() throws ReadConfigurationException{
		configurationContext.read(TestCompositeConfigurationClassWithDefaultReader.class);
	}
	
	@Test
	public void testRead_ShouldInjectAlreadyReadObjectWhenRequiredAsDependency() throws ReadConfigurationException{
		configurationContext.read(TestCompositeConfigurationWhichUsesSavedInstance.class);
	}
	/** --------------------------- Supporting methods and classes ------------------------ **/
	
	@Configuration
	public static class TestCompositeConfigurationWhichUsesSavedInstance{
		@Depends private TestConfigurationClass dependency1;
		@Configuration private TestConfigurationClassUsesDependency dependency2;
		
		@PostRead
		public void verify(){
			assertTrue(dependency1 == dependency2.dependency1);
		}
	}
	
	@Configuration
	public static class TestConfigurationClassUsesDependency{
		@Depends TestConfigurationClass dependency1;
	}
	
	/* 
	 * Tests whether the post read of the dependent classes are called before the container class
	 */
	@Configuration
	public static class TestCompositeConfigurationPostReadLifecycleInvocationChain{
		@Configuration private TestConfigurationClass dependency1;
		
		@PostRead
		public void verify(){
			assertTrue(dependency1.isPostReadCalled());
		}
	}
	
	@DataProvider
	public static Object[][] dataFor_testRead_ShouldThrowProperException_WhenDependencyThrowsSomeException(){
		return new Object[][]{
				{TestRequiresDependencyWhichThrowsCheckedException.class, Exception.class},
				{TestRequiresDependencyWhichThrowsUncheckedException.class, RuntimeException.class},
				{TestRequiresDependencyWhichThrowsError.class, Error.class},
		};
	}
	
	@DataProvider
	public static Object[][] dataFor_testRead_ShouldReturn_InstanceOfClassPassedToIt(){
		return new Object[][]{
				{TestConfigurationClass.class},
				{TestConfigurationClass1.class}
		};
	}

	@DataProvider
	public static Object[][] dataFor_testRead_ShouldThrowProperlyWrappedExceptions_IfAnyProblemInReading(){
		return new Object[][]{
				{null,																			IllegalArgumentException.class},
				{TestConfigurationContextClassWithProtectedConstructor.class,	 				NoSuchMethodException.class},
				{TestConfigurationClassWithPackageProtectedConstructor.class,	 				NoSuchMethodException.class},
				{TestConfigurationClassWithPrivateConstructor.class,			 				NoSuchMethodException.class},
				{TestConfigurationClassWithoutDefaultConstructor.class,			 				NoSuchMethodException.class},
				{TestConfigurationClassWhoseDefaultConstructorThrowsCheckedException.class, 	Exception.class},
				{TestConfigurationClassWhoseDefaultConstructorThrowsUncheckedException.class,	RuntimeException.class},
				{TestConfigurationClassWhoseDefaultConstructorThrowsError.class,				Error.class},
				{TestConfigurationClassWithPrivateAccess.class,									IllegalAccessException.class},
//				{TestConfigurationClassWithDefaultPackageAccess.class,							NoSuchMethodException.class},
				{TestConfigurationClassWithMultipleLifeCycleMethods.class,						IllegalArgumentException.class},
				{TestConfigurationClassWithPreLifeCycleMethodThrowsCheckedException.class,		Exception.class},
				{TestConfigurationClassWithPreLifeCycleMethodThrowsUncheckedException.class,	RuntimeException.class},
				{TestConfigurationClassWithPreLifeCycleMethodThrowsError.class,					Error.class},
				{TestConfigurationClassWithPostLifeCycleMethodThrowsCheckedException.class,		Exception.class},
				{TestConfigurationClassWithPostLifeCycleMethodThrowsUncheckedException.class,	RuntimeException.class},
				{TestConfigurationClassWithPostLifeCycleMethodThrowsError.class,				Error.class},
				{TestConfigurationClassWithPreLifeCycleMethodsStatic.class,						IllegalArgumentException.class},
				{TestConfigurationClassWithPostLifeCycleMethodsStatic.class,					IllegalArgumentException.class},
				{TestConfigurationClassWithPostLifeCycleMethodsWithPackageDefaultAccess.class,	IllegalArgumentException.class},
				{TestConfigurationClassWithPostLifeCycleMethodsWithProtectedAccess.class,		IllegalArgumentException.class},
				{TestConfigurationClassWithPostLifeCycleMethodsWithPrivateAccess.class,			IllegalArgumentException.class},
				{TestConfigurationClassWithPreLifeCycleMethodsWithPackageDefaultAccess.class,	IllegalArgumentException.class},
				{TestConfigurationClassWithPreLifeCycleMethodsWithProtectedAccess.class,		IllegalArgumentException.class},
				{TestConfigurationClassWithPreLifeCycleMethodsWithPrivateAccess.class,			IllegalArgumentException.class},
				{TestConfigurationClassWithoutConfigurationProperties.class,					IllegalArgumentException.class},
				{TestConfigurationClassWithReaderThrowingCheckedException.class,				Exception.class},
				{TestConfigurationClassWithReaderThrowingUnCheckedException.class,				RuntimeException.class},
				{TestConfigurationClassWithReaderThrowingError.class,							Error.class},
				{TestCompositeConfigurationDecisionMethodThrowingCheckedException.class,		Exception.class},
				{TestCompositeConfigurationDecisionMethodThrowingUncheckedException.class,		RuntimeException.class},
				{TestCompositeConfigurationDecisionMethodThrowingError.class,					Error.class}
		};
	}

	public static class TestReader extends Reader{

		@Override
		public Object read(Class<?> classToRead) throws Throwable {
			Object instance = ReflectionUtil.createDefaultInstance(classToRead);
			ReflectionUtil.invokeLifeCycleEvent(instance, PreRead.class);
			return instance;
		}
		
	}
	
	@Configuration(readWith = TestReader.class)
	static interface TestConfigurationInterface{
	}

	@Configuration(readWith = TestReader.class)
	public static class TestRequiresDependency{
		@Depends private TestConfigurationClass dependency;

		@PostRead
		public void postRead(){
			assertNotNull(dependency);
		}
	}
	
	@SuppressWarnings("unused")
	@Configuration(readWith = TestReader.class)
	public static class TestCircularDependency1{
		@Depends private TestCircularDependency2 dependency;
	}
	
	@SuppressWarnings("unused")
	@Configuration(readWith = TestReader.class)
	public static class TestCircularDependency2{
		@Depends private TestCircularDependency1 dependency;
	}
	
	@SuppressWarnings("unused")
	@Configuration(readWith = TestReader.class)
	public static class TestRequiresDependencyWhichThrowsCheckedException{
		@Depends private TestConfigurationClassWithPostLifeCycleMethodThrowsCheckedException dependency;
		
		public void postRead(){
			fail("Post read lifecycle event called even after read failure of dependent configuration");
		}
	}
	
	@SuppressWarnings("unused")
	@Configuration(readWith = TestReader.class)
	public static class TestRequiresDependencyWhichThrowsUncheckedException{
		@Depends private TestConfigurationClassWithPostLifeCycleMethodThrowsUncheckedException dependency;
		
		public void postRead(){
			fail("Post read lifecycle event called even after read failure of dependent configuration");
		}
	}
	
	@SuppressWarnings("unused")
	@Configuration(readWith = TestReader.class)
	public static class TestRequiresDependencyWhichThrowsError{
		@Depends private TestConfigurationClassWithPostLifeCycleMethodThrowsError dependency;
		
		public void postRead(){
			fail("Post read lifecycle event called even after read failure of dependent configuration");
		}
	}

	@Configuration
	public static class TestCompositeConfigurationClassWithDefaultReader{
		@Depends public TestConfigurationClass dependency1;
		@Depends public TestRequiresDependency dependency2;
		
		@PostRead
		public void testNonNullessOfDependencies(){
			assertNotNull(dependency1);
			assertNotNull(dependency2);
		}
	}
	
	@Configuration(readWith = JAXBReader.class)
	@XmlRootElement(name = "test-xml-configuration")
	public static class TestCompositeConfigurationClassWithJAXBReader{
		public static final String xml = "<test-xml-configuration><x>1</x></test-xml-configuration>";
		
		@Depends public TestConfigurationClass dependency1;
		@Depends public TestRequiresDependency dependency2;
		
		private int x;
		
		public int getX() {
			return x;
		}

		public void setX(int x) {
			this.x = x;
		}

		@PostRead
		public void testValidityOfFields(){
			assertNotNull(dependency1);
			assertNotNull(dependency2);
			assertEquals(getX(), 1);
		}
	}
	
	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClass{
		private boolean isPostReadCalled = false;
		
		public boolean isPostReadCalled() {
			return isPostReadCalled;
		}

		@PostRead
		public void postRead(){
			isPostReadCalled = true;
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClass1{
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithoutDefaultConstructor{
		public TestConfigurationClassWithoutDefaultConstructor(Object someParameter) {
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPrivateConstructor{
		private TestConfigurationClassWithPrivateConstructor() {
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithMultipleConstructorsWithNonDefaultConstructorThrowingException{
		public TestConfigurationClassWithMultipleConstructorsWithNonDefaultConstructorThrowingException() {
		}

		public TestConfigurationClassWithMultipleConstructorsWithNonDefaultConstructorThrowingException(Object someParam) throws Exception {
			throw new Exception("This constructor should not be called, default constructor should be called");
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWhoseDefaultConstructorThrowsCheckedException{
		public TestConfigurationClassWhoseDefaultConstructorThrowsCheckedException() throws Exception {
			throw new Exception("Exception for testing purposes");
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWhoseDefaultConstructorThrowsUncheckedException{
		public TestConfigurationClassWhoseDefaultConstructorThrowsUncheckedException() {
			throw new RuntimeException("Runtime exception for testing purposes");
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWhoseDefaultConstructorThrowsError{
		public TestConfigurationClassWhoseDefaultConstructorThrowsError() {
			throw new Error("Error for testing purposes");
		}
	}

	@Configuration(readWith = TestReader.class)
	private static class TestConfigurationClassWithPrivateAccess{
		@SuppressWarnings("unused")
		public TestConfigurationClassWithPrivateAccess() {
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithNoLifeCycleMethod{
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithNoPreReadLifeCycleMethod{
		@PostRead
		public void postRead(){
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithNoPostReadLifeCycleMethod{
		@PostRead
		public void postRead(){
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithAllLifeCycleAnnotations{
		private boolean preReadCalled = false;
		private boolean postReadCalled = false;
		private boolean preCalledBeforePost = false;
		private int preReadCallCount = 0;
		private int postReadCallCount = 0;


		public TestConfigurationClassWithAllLifeCycleAnnotations() {
		}

		@PreRead
		public void preRead(){
			preReadCalled = true;
			preReadCallCount++;
			preCalledBeforePost = !postReadCalled;
		}

		@PostRead
		public void postRead() throws Exception{
			postReadCalled = true;
			postReadCallCount++;
		}
		
		public int getPreReadCallCount() {
			return preReadCallCount;
		}
		
		public int getPostReadCallCount() {
			return postReadCallCount;
		}

		public boolean isPreReadCalled() {
			return preReadCalled;
		}

		public boolean isPreCalledBeforePost() {
			return preCalledBeforePost;
		}

		public boolean isPostReadCalled() {
			return postReadCalled;
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithMultipleLifeCycleMethods{
		@PreRead
		public void preRead1(){
		}

		@PreRead
		public void preRead2(){
		}

		@PostRead
		public void postRead1(){

		}

		@PostRead
		public void postRead2(){

		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPreLifeCycleMethodThrowsCheckedException{
		@PreRead
		public void preRead() throws Exception{
			throw new Exception();
		}

		@PostRead
		public void postRead(){
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPreLifeCycleMethodThrowsUncheckedException{
		@PreRead
		public void preRead(){
			throw new RuntimeException();
		}

		@PostRead
		public void postRead(){
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPreLifeCycleMethodThrowsError{
		@PreRead
		public void preRead(){
			throw new Error();
		}

		@PostRead
		public void postRead(){
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPostLifeCycleMethodThrowsCheckedException{
		@PreRead
		public void preRead(){

		}

		@PostRead
		public void postRead() throws Exception{
			throw new Exception();
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPostLifeCycleMethodThrowsUncheckedException{
		@PreRead
		public void preRead(){
		}

		@PostRead
		public void postRead(){
			throw new RuntimeException();
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPostLifeCycleMethodThrowsError{
		@PreRead
		public void preRead(){
		}

		@PostRead
		public void postRead(){
			throw new Error();
		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPreLifeCycleMethodsStatic{
		@PreRead
		public static void preRead(){

		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPostLifeCycleMethodsStatic{
		@PostRead
		public static void postRead(){

		}
	}
	
	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPreLifeCycleMethodsWithPackageDefaultAccess{
		@PreRead
		void preRead(){

		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPostLifeCycleMethodsWithPackageDefaultAccess{
		@PostRead
		void postRead(){

		}
	}
	
	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPreLifeCycleMethodsWithProtectedAccess{
		@PreRead
		protected void preRead(){

		}
	}

	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPostLifeCycleMethodsWithProtectedAccess{
		@PostRead 
		protected void postRead(){

		}
	}
	
	@SuppressWarnings("unused")
	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPreLifeCycleMethodsWithPrivateAccess{
		@PreRead
		private void preRead(){

		}
	}

	@SuppressWarnings("unused")
	@Configuration(readWith = TestReader.class)
	public static class TestConfigurationClassWithPostLifeCycleMethodsWithPrivateAccess{
		@PostRead
		private void postRead(){

		}
	}
	
	public class TestConfigurationClassWithoutConfigurationProperties{
		
	}
	
	@Configuration
	public static class TestConfigurationClassWithNoReaderSpecified{
		
	}
	
	@Configuration(readWith = ReaderWhichThrowsCheckedExceptionWhileReading.class)
	public static class TestConfigurationClassWithReaderThrowingCheckedException{
		
	}
	
	@Configuration(readWith = ReaderWhichThrowsUnCheckedExceptionWhileReading.class)
	public static class TestConfigurationClassWithReaderThrowingUnCheckedException{
		
	}
	
	@Configuration(readWith = ReaderWhichThrowsErrorWhileReading.class)
	public static class TestConfigurationClassWithReaderThrowingError{
		
	}
	
	public static class ReaderWhichThrowsCheckedExceptionWhileReading extends Reader{

		@Override
		public Object read(Class<?> classToRead) throws Exception {
			throw new Exception("This can be any checked exception");
		}
	}
	
	public static class ReaderWhichThrowsUnCheckedExceptionWhileReading extends Reader{

		@Override
		public Object read(Class<?> classToRead) throws Exception {
			throw new RuntimeException("This can be any unchecked exception");
		}
	}
	
	public static class ReaderWhichThrowsErrorWhileReading extends Reader{

		@Override
		public Object read(Class<?> classToRead) throws Exception {
			throw new Error("This can be any error");
		}
	}
	
	
	
	@Test
	public void testRead_ShouldInjectOptionallyDependentConfiguration_WhenReadProperly() throws ReadConfigurationException{
		configurationContext.read(TestCompositeConfigurationWithOneOptionalDependencies.class);
	}
	
	@Test
	public void testRead_ShouldSkipReadingOptionalDependentConfiguration_WhenDecisionIsFalse() throws ReadConfigurationException{
		configurationContext.read(TestCompositeConfigurationWithOneOptionalDependencyAndFalseDecision.class);
	}
	
	@Test
	public void testRead_TwoConsecutiveCallsToReadShouldNotReturnSameInstance() throws ReadConfigurationException{
		Object instannce1 = configurationContext.read(TestConfigurationClass.class);
		Object instannce2 = configurationContext.read(TestConfigurationClass.class);
		Assert.assertNotEquals(instannce1, instannce2);
	}
	
	@Configuration
	public static class TestCompositeConfigurationWithOneOptionalDependencies{
		@Depends TestConfigurationClass dependency1;
		@Depends @Optional TestConfigurationClass1 dependency2;
		
		@Decision
		public boolean eligible(Class<?> classToRead){
			return true;
		}
		
		@PostRead
		public void verify(){
			assertNotNull(dependency2);
		}
	}
	
	@Configuration
	public static class TestCompositeConfigurationWithOneOptionalDependencyAndFalseDecision{
		@Depends TestConfigurationClass dependency1;
		@Depends @Optional TestConfigurationClass1 dependency2;
		
		@Decision
		public boolean eligible(Class<?> classToRead){
			return false;
		}
		
		@PostRead
		public void verify(){
			assertNull(dependency2);
		}
	}
	
	@Configuration
	public static class TestCompositeConfigurationDecisionMethodThrowingCheckedException{
		@Depends TestConfigurationClass dependency1;
		@Depends @Optional TestConfigurationClass1 dependency2;
		
		@Decision
		public boolean eligible(Class<?> classToRead) throws Exception{
			throw new Exception();
		}
	}
	
	@Configuration
	public static class TestCompositeConfigurationDecisionMethodThrowingUncheckedException{
		@Depends TestConfigurationClass dependency1;
		@Depends @Optional TestConfigurationClass1 dependency2;
		
		@Decision
		public boolean eligible(Class<?> classToRead) throws Exception{
			throw new RuntimeException();
		}
	}
	
	@Configuration
	public static class TestCompositeConfigurationDecisionMethodThrowingError{
		@Depends TestConfigurationClass dependency1;
		@Depends @Optional TestConfigurationClass1 dependency2;
		
		@Decision
		public boolean eligible(Class<?> classToRead) throws Exception{
			throw new Error();
		}
	}
	
	
	/*
	 * The use of conditionals
	 */
	
	@Test
	public void testRead_ShouldReadConditionalDependenciesWhenDecisionIsTrue() throws ReadConfigurationException{
		configurationContext.read(MainConfiguration.class);
	}
	
	@Configuration
	public static class ServiceConfiguration{
		@Depends private TestConfigurationClass dependency;
		
		public boolean isEnabled(){
			return false;
		}
		
		@PostRead
		public void verify(){
			assertNotNull(dependency);
		}
	}
	
	@Configuration
	public static class SomeServiceConfiguration{
		
	}
	
	@Configuration
	public static class MainConfiguration{
		
		@Configuration private ServiceConfiguration sc;
		@Configuration @Conditional private SomeServiceConfiguration ssc;
		
		@Decision
		public boolean decision(Class<?> config){
			return true;
		}
		
		@PostRead
		public void verify(){
			assertNotNull(sc);
			assertNotNull(ssc);
		}
		
	}
}
