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

package de.dytanic.cloudnet.driver.network.netty;

import de.dytanic.cloudnet.common.concurrent.CompletableTask;
import de.dytanic.cloudnet.common.log.LogManager;
import de.dytanic.cloudnet.common.log.Logger;
import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.IOException;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

@Internal
public abstract class NettyNetworkHandler extends SimpleChannelInboundHandler<Packet> {

  private static final Logger LOGGER = LogManager.getLogger(NettyNetworkHandler.class);

  protected volatile NettyNetworkChannel channel;

  @Override
  public void channelInactive(@NotNull ChannelHandlerContext ctx) throws Exception {
    if (!ctx.channel().isActive() || !ctx.channel().isOpen() || !ctx.channel().isWritable()) {
      if (this.channel.getHandler() != null) {
        this.channel.getHandler().handleChannelClose(this.channel);
      }

      ctx.channel().close();
      this.getChannels().remove(this.channel);
    }
  }

  @Override
  public void exceptionCaught(@NotNull ChannelHandlerContext ctx, @NotNull Throwable cause) {
    if (!(cause instanceof IOException)) {
      LOGGER.severe("Exception in network handler", cause);
    }
  }

  @Override
  public void channelReadComplete(@NotNull ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  protected void channelRead0(@NotNull ChannelHandlerContext ctx, @NotNull Packet msg) {
    this.getPacketDispatcher().execute(() -> {
      try {
        var uuid = msg.getUniqueId();
        if (uuid != null) {
          var task = this.channel.getQueryPacketManager().getWaitingHandler(uuid);
          if (task != null) {
            task.complete(msg);
            // don't post a query response packet to another handler at all
            return;
          }
        }

        if (this.channel.getHandler() == null || this.channel.getHandler().handlePacketReceive(this.channel, msg)) {
          this.channel.getPacketRegistry().handlePacket(this.channel, msg);
        }
      } catch (Exception exception) {
        LOGGER.severe("Exception whilst handling packet " + msg, exception);
      }
    });
  }

  protected abstract @NotNull Collection<INetworkChannel> getChannels();

  protected abstract @NotNull Executor getPacketDispatcher();
}
