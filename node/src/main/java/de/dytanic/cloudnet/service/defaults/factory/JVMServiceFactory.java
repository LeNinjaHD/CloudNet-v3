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

package de.dytanic.cloudnet.service.defaults.factory;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.driver.event.IEventManager;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.service.ICloudService;
import de.dytanic.cloudnet.service.ICloudServiceManager;
import de.dytanic.cloudnet.service.ServiceConfigurationPreparer;
import de.dytanic.cloudnet.service.defaults.JVMService;
import org.jetbrains.annotations.NotNull;

public class JVMServiceFactory extends AbstractServiceFactory {

  private final CloudNet nodeInstance;
  private final IEventManager eventManager;

  public JVMServiceFactory(CloudNet nodeInstance, IEventManager eventManager) {
    this.nodeInstance = nodeInstance;
    this.eventManager = eventManager;
  }

  @Override
  public @NotNull ICloudService createCloudService(
    @NotNull ICloudServiceManager manager,
    @NotNull ServiceConfiguration configuration
  ) {
    // validates the settings of the configuration
    this.validateConfiguration(manager, configuration);
    // select the configuration preparer for the environment
    var preparer = manager
      .getServicePreparer(configuration.getServiceId().getEnvironment())
      .orElseThrow(() -> new IllegalArgumentException("Unable to prepare config for " + configuration.getServiceId()));
    // create the service
    return new JVMService(configuration, manager, this.eventManager, this.nodeInstance, preparer);
  }
}
