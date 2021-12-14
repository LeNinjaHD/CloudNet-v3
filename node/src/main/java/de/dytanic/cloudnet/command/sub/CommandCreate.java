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

package de.dytanic.cloudnet.command.sub;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import cloud.commandframework.annotations.specifier.Range;
import de.dytanic.cloudnet.command.annotation.Description;
import de.dytanic.cloudnet.command.source.CommandSource;
import de.dytanic.cloudnet.common.JavaVersion;
import de.dytanic.cloudnet.common.collection.Pair;
import de.dytanic.cloudnet.common.language.I18n;
import de.dytanic.cloudnet.driver.service.ServiceConfiguration;
import de.dytanic.cloudnet.driver.service.ServiceInfoSnapshot;
import de.dytanic.cloudnet.driver.service.ServiceTask;
import java.util.ArrayList;
import java.util.List;

@CommandPermission("cloudnet.command.create")
@Description("Creates one or more new services based on a task or completely independent")
public final class CommandCreate {

  @CommandMethod("create by <task> <amount>")
  public void createByTask(
    CommandSource source,
    @Argument("task") ServiceTask task,
    @Argument("amount") @Range(min = "1") int amount,
    @Flag("start") boolean startService,
    @Flag("id") Integer id,
    @Flag(value = "javaCommand", parserName = "javaCommand") Pair<String, JavaVersion> javaCommand,
    @Flag("node") String nodeId,
    @Flag("memory") Integer memory
  ) {
    var configurationBuilder = ServiceConfiguration.builder(task);
    if (id != null) {
      configurationBuilder.taskId(id);
    }

    if (javaCommand != null) {
      configurationBuilder.javaCommand(javaCommand.getFirst());
    }

    if (nodeId != null) {
      configurationBuilder.node(nodeId);
    }

    if (memory != null) {
      configurationBuilder.maxHeapMemory(memory);
    }

    List<ServiceInfoSnapshot> createdServices = new ArrayList<>();
    for (var i = 0; i < amount; i++) {
      var service = configurationBuilder.build().createNewService();
      if (service != null) {
        createdServices.add(service);
      }
    }

    if (createdServices.isEmpty()) {
      source.sendMessage(I18n.trans("command-create-by-task-failed"));
      return;
    }

    source.sendMessage(I18n.trans("command-create-by-task-success"));
    if (startService) {
      for (var createdService : createdServices) {
        createdService.provider().start();
      }
    }
  }
}
