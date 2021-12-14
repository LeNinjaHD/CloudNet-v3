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

package eu.cloudnetservice.cloudnet.ext.syncproxy.platform.waterdog;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.ProxyPingEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyLoginConfiguration;
import eu.cloudnetservice.cloudnet.ext.syncproxy.config.SyncProxyMotd;
import org.jetbrains.annotations.NotNull;

public final class WaterDogPESyncProxyListener {

  private final WaterDogPESyncProxyManagement syncProxyManagement;

  public WaterDogPESyncProxyListener(
    @NotNull WaterDogPESyncProxyManagement syncProxyManagement,
    @NotNull ProxyServer proxyServer
  ) {
    this.syncProxyManagement = syncProxyManagement;

    // subscribe to the events and redirect them to the methods to handle them
    proxyServer.getEventManager().subscribe(ProxyPingEvent.class, this::handleProxyPing);
    proxyServer.getEventManager().subscribe(PlayerLoginEvent.class, this::handleProxyLogin);
  }

  private void handleProxyPing(@NotNull ProxyPingEvent event) {
    var loginConfiguration = this.syncProxyManagement.getCurrentLoginConfiguration();
    // check if we need to handle the proxy ping on this proxy instance
    if (loginConfiguration == null) {
      return;
    }

    var motd = this.syncProxyManagement.getRandomMotd();
    // only display a motd if there is one in the config
    if (motd != null) {
      var onlinePlayers = this.syncProxyManagement.getOnlinePlayerCount();
      int maxPlayers;

      event.setPlayerCount(onlinePlayers);

      if (motd.isAutoSlot()) {
        maxPlayers = Math.min(loginConfiguration.getMaxPlayers(), onlinePlayers + motd.getAutoSlotMaxPlayersDistance());
      } else {
        maxPlayers = loginConfiguration.getMaxPlayers();
      }
      event.setMaximumPlayerCount(maxPlayers);

      // bedrock has just to lines that are separated  from each other
      var mainMotd = motd.format(motd.getFirstLine(), onlinePlayers, maxPlayers);
      var subMotd = motd.format(motd.getSecondLine(), onlinePlayers, maxPlayers);

      event.setMotd(mainMotd);
      event.setSubMotd(subMotd);
    }
  }

  private void handleProxyLogin(@NotNull PlayerLoginEvent event) {
    var loginConfiguration = this.syncProxyManagement.getCurrentLoginConfiguration();
    if (loginConfiguration == null) {
      return;
    }

    var proxiedPlayer = event.getPlayer();
    if (loginConfiguration.isMaintenance()) {
      // the player is either whitelisted or has the permission to join during maintenance, ignore him
      if (this.syncProxyManagement.checkPlayerMaintenance(proxiedPlayer)) {
        return;
      }
      event.setCancelReason(
        this.syncProxyManagement.getConfiguration().getMessage("player-login-not-whitelisted", null));
      event.setCancelled(true);

      return;
    }
    // check if the proxy is full and if the player is allowed to join or not
    if (this.syncProxyManagement.getOnlinePlayerCount() >= loginConfiguration.getMaxPlayers()
      && !proxiedPlayer.hasPermission("cloudnet.syncproxy.fulljoin")) {
      event.setCancelReason(this.syncProxyManagement.getConfiguration().getMessage("player-login-full-server", null));
      event.setCancelled(true);
    }
  }
}
