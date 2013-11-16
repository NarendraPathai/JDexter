package org.jdexter.context.data;

import org.jdexter.annotation.Configuration;
import org.jdexter.context.ConfigurationContextUnitTest.TestReader;

@Configuration(readWith = TestReader.class)
public class TestConfigurationContextClassWithProtectedConstructor{
	protected TestConfigurationContextClassWithProtectedConstructor() {
	}
}