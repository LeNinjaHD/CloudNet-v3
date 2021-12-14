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

package de.dytanic.cloudnet.config;

import com.google.common.primitives.Ints;
import de.dytanic.cloudnet.driver.network.HostAndPort;
import java.util.Collection;
import java.util.HashSet;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ConfigurationUtils {

  static final Function<String, HostAndPort[]> HOST_AND_PORT_PARSER = value -> {
    // the result
    Collection<HostAndPort> listeners = new HashSet<>();
    // read from the value
    var hostAndPorts = value.split(",");
    for (var hostAndPort : hostAndPorts) {
      var splitHostAndPort = hostAndPort.split("%");
      if (splitHostAndPort.length == 2) {
        // try to parse the host and port info
        var port = Ints.tryParse(splitHostAndPort[1]);
        if (port != null) {
          listeners.add(new HostAndPort(splitHostAndPort[0], port));
        }
      }
    }
    // collect to an array
    return listeners.toArray(new HostAndPort[0]);
  };

  private ConfigurationUtils() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull String get(@NotNull String propertyName, @NotNull String def) {
    // try to read the value from the system properties
    var val = getProperty(propertyName);
    return val == null ? def : val;
  }

  public static @NotNull <T> T get(
    @NotNull String propertyName,
    @NotNull T def,
    @NotNull Function<String, T> mapper
  ) {
    // try to read the value from the system properties
    var val = getProperty(propertyName);
    if (val == null) {
      return def;
    }
    // try to map the input
    try {
      var mapped = mapper.apply(val);
      return mapped == null ? def : mapped;
    } catch (Exception exception) {
      return def;
    }
  }

  private static @Nullable String getProperty(@NotNull String propertyName) {
    var val = System.getProperty(propertyName);
    return val == null ? System.getenv(propertyName.replace('.', '_').toUpperCase()) : val;
  }
}
