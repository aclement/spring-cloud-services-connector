/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pivotal.spring.cloud.service.hystrix;

import static io.pivotal.spring.cloud.service.hystrix.HystrixStreamServiceConnector.*;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.service.ServiceInfo;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.pivotal.spring.cloud.MockCloudConnector;
import io.pivotal.spring.cloud.service.common.EurekaServiceInfo;
import io.pivotal.spring.cloud.service.common.HystrixAmqpServiceInfo;

public class HystrixStreamServiceConnectorIntegrationTest {
	
	public static class WithoutRabbitBinding extends AbstractHystrixStreamServiceConnectorIntegrationTest {

		@Test
		public void propertySourceIsAdded() {
			assertPropertyEquals("hystrix", SPRING_CLOUD_STREAM_BINDINGS_HYSTRIXSTREAMOUTPUT + "binder");
			assertPropertyEquals(SPRING_CLOUD_HYSTRIX_STREAM, SPRING_CLOUD_STREAM_BINDINGS_HYSTRIXSTREAMOUTPUT + "destination");
			assertPropertyEquals("rabbit", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX + "type");
			assertPropertyEquals("false", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX + "inheritEnvironment");
			assertPropertyEquals("false", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX + "defaultCandidate");
			assertPropertyEquals("p-rabbitmq.mydomain.com:5672", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX_ENVIRONMENT_SPRING_RABBITMQ + "addresses");
			assertPropertyEquals("username", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX_ENVIRONMENT_SPRING_RABBITMQ + "username");
			assertPropertyEquals("password", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX_ENVIRONMENT_SPRING_RABBITMQ + "password");
			assertPropertyEquals("testvhost", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX_ENVIRONMENT_SPRING_RABBITMQ + "virtualHost");
			assertPropertyEquals("false", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX_ENVIRONMENT_SPRING_RABBITMQ + "ssl.enabled");
			assertPropertyEquals("true", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX + "environment.spring.cloud.stream.overrideCloudConnectors");
			assertPropertyEquals("", SPRING_CLOUD_STREAM_BINDERS_HYSTRIX + "default.prefix");
			assertPropertyEquals("org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration", SPRING_AUTOCONFIGURE_EXCLUDE);
		}
		
	}
	
	@TestPropertySource(properties="spring.rabbitmq.host=some_rabbit_host")
	public static class WithRabbitBinding extends AbstractHystrixStreamServiceConnectorIntegrationTest {

		@Test
		public void springAutoConfigureExcludeIsNull() {
			assertPropertyEquals(null, SPRING_AUTOCONFIGURE_EXCLUDE);
		}
		
	}
	
	@TestPropertySource(properties="spring.autoconfigure.exclude=com.foo.Bar")
	public static class WithExistingAutoConfigExcludes extends AbstractHystrixStreamServiceConnectorIntegrationTest {

		@Test
		public void springAutoConfigureExcludePreservesExistingExcludes() {
			assertPropertyEquals("org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration,com.foo.Bar", SPRING_AUTOCONFIGURE_EXCLUDE);
		}
		
	}

	@RunWith(SpringJUnit4ClassRunner.class)
	@SpringApplicationConfiguration(classes = {
			AbstractHystrixStreamServiceConnectorIntegrationTest.TestConfig.class
	})
	@IntegrationTest
	public static abstract class AbstractHystrixStreamServiceConnectorIntegrationTest {

		private static final String URI = "amqp://username:password@p-rabbitmq.mydomain.com/testvhost";

		@Autowired
		private Environment environment;
	 
		@BeforeClass
		@SuppressWarnings("unchecked")
		public static void beforeClass() throws IOException {
			when(MockCloudConnector.instance.isInMatchingCloud()).thenReturn(true);
			when(MockCloudConnector.instance.getServiceInfos()).thenReturn(
					Arrays.asList(
							(ServiceInfo) new HystrixAmqpServiceInfo("circuit-breaker", URI),
							(ServiceInfo) new EurekaServiceInfo("service-registry", "http://example.com", "clientId", "clientSecret", "http://example.com/token")
					)
			);
		}

		@AfterClass
		public static void afterClass() {
			MockCloudConnector.reset();
		}

		protected void assertPropertyEquals(String expected, String key) {
			assertEquals(expected, environment.getProperty(key));
		}

		@EnableCircuitBreaker
		public static class TestConfig {
		}
	}

}
