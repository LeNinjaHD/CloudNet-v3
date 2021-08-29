/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.driver.network.rpc.registry;

import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.RPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.DefaultRPCProviderFactory;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.DefaultRPCHandlerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.defaults.object.DefaultObjectMapper;
import de.dytanic.cloudnet.driver.network.rpc.handler.DefaultRPCHandlerTest.TestApiClass;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@TestMethodOrder(OrderAnnotation.class)
public class DefaultRPCHandlerRegistryTest {

  @Test
  @Order(0)
  void testRegisterHandler() {
    RPCProviderFactory factory = this.provideFactory();
    RPCHandlerRegistry registry = new DefaultRPCHandlerRegistry();

    registry.registerHandler(factory.newHandler(TestApiClass.class, new TestApiClass(new AtomicLong())));

    Assertions.assertEquals(1, registry.getRegisteredHandlers().size());
    Assertions.assertNotNull(registry.getRegisteredHandlers().get(TestApiClass.class.getName()));

    Assertions.assertTrue(registry.hasHandler(TestApiClass.class));
    Assertions.assertTrue(registry.hasHandler(TestApiClass.class.getName()));

    Assertions.assertNotNull(registry.getHandler(TestApiClass.class));
    Assertions.assertNotNull(registry.getHandler(TestApiClass.class.getName()));
  }

  @Test
  @Order(10)
  void testUnregisterHandler() {
    RPCProviderFactory factory = this.provideFactory();
    RPCHandlerRegistry registry = new DefaultRPCHandlerRegistry();

    registry.registerHandler(factory.newHandler(TestApiClass.class, new TestApiClass(new AtomicLong())));

    Assertions.assertEquals(1, registry.getRegisteredHandlers().size());
    Assertions.assertNotNull(registry.getRegisteredHandlers().get(TestApiClass.class.getName()));

    registry.unregisterHandler(TestApiClass.class);

    Assertions.assertEquals(0, registry.getRegisteredHandlers().size());
    Assertions.assertNull(registry.getRegisteredHandlers().get(TestApiClass.class.getName()));
  }

  private RPCProviderFactory provideFactory() {
    return new DefaultRPCProviderFactory(new DefaultObjectMapper(), DataBufFactory.defaultFactory());
  }
}