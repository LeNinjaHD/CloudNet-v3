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

package de.dytanic.cloudnet.driver.network.rpc.listener;

import de.dytanic.cloudnet.driver.network.INetworkChannel;
import de.dytanic.cloudnet.driver.network.buffer.DataBuf;
import de.dytanic.cloudnet.driver.network.buffer.DataBufFactory;
import de.dytanic.cloudnet.driver.network.protocol.IPacket;
import de.dytanic.cloudnet.driver.network.protocol.IPacketListener;
import de.dytanic.cloudnet.driver.network.protocol.Packet;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandler.HandlingResult;
import de.dytanic.cloudnet.driver.network.rpc.RPCHandlerRegistry;
import de.dytanic.cloudnet.driver.network.rpc.RPCInvocationContext;
import de.dytanic.cloudnet.driver.network.rpc.defaults.handler.util.ExceptionalResultUtils;
import de.dytanic.cloudnet.driver.network.rpc.object.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RPCPacketListener implements IPacketListener {

  private final RPCHandlerRegistry rpcHandlerRegistry;

  public RPCPacketListener(@NotNull RPCHandlerRegistry rpcHandlerRegistry) {
    this.rpcHandlerRegistry = rpcHandlerRegistry;
  }

  @Override
  public void handle(@NotNull INetworkChannel channel, @NotNull IPacket packet) throws Exception {
    // the result of the invocation, encoded
    DataBuf result = null;
    // the input information we get
    var buf = packet.getContent();
    // check if the invocation is chained
    if (buf.readBoolean()) {
      // get the chain size
      var chainSize = buf.readInt();
      // invoke the method on the current result
      HandlingResult lastResult = null;
      for (var i = 1; i < chainSize; i++) {
        if (i == 1) {
          // always invoke the first method
          lastResult = this.handleRaw(buf.readString(), this.buildContext(channel, buf, null, false));
        } else if (lastResult != null) {
          if (lastResult.wasSuccessful()) {
            // only invoke upcoming methods if there was a previous result
            lastResult = this.handleRaw(
              buf.readString(),
              this.buildContext(channel, buf, lastResult.getInvocationResult(), true));
          } else {
            // an exception was thrown previously, break
            buf.readString(); // remove the handler information which is not necessary
            result = this.serializeResult(
              lastResult,
              lastResult.getHandler().getDataBufFactory(),
              lastResult.getHandler().getObjectMapper(),
              this.buildContext(channel, buf, null, true));
            break;
          }
        } else {
          // just process over to remove the content from the buffer
          this.handleRaw(buf.readString(), this.buildContext(channel, buf, null, true));
        }
      }
      // check if there is already a result (which is caused by an exception - we can skip the handling step then)
      if (result == null && lastResult != null) {
        // the last handler decides over the method invocation result
        result = this.handle(
          buf.readString(),
          this.buildContext(channel, buf, lastResult.getInvocationResult(), true));
      }
    } else {
      // just invoke the method
      result = this.handle(buf.readString(), this.buildContext(channel, buf, null, false));
    }
    // check if we need to send a result
    if (result != null && packet.getUniqueId() != null) {
      channel.getQueryPacketManager().sendQueryPacket(new Packet(-1, result), packet.getUniqueId());
    }
  }

  protected @Nullable DataBuf handle(@NotNull String clazz, @NotNull RPCInvocationContext context) {
    // get the handler associated with the class of the rpc
    var handler = this.rpcHandlerRegistry.getHandler(clazz);
    // check if the method gets called on a specific instance
    if (handler != null) {
      // invoke the method
      var handlingResult = handler.handle(context);
      // serialize the result
      return this.serializeResult(handlingResult, handler.getDataBufFactory(), handler.getObjectMapper(), context);
    }
    // no handler for the class - no result
    return null;
  }

  protected @Nullable DataBuf serializeResult(
    @NotNull HandlingResult result,
    @NotNull DataBufFactory dataBufFactory,
    @NotNull ObjectMapper objectMapper,
    @NotNull RPCInvocationContext context
  ) {
    // check if the sender expect the result of the method
    if (context.expectsMethodResult()) {
      // check if the method return void
      if (result.wasSuccessful() && result.getTargetMethodInformation().isVoidMethod()) {
        return dataBufFactory.createWithExpectedSize(2)
          .writeBoolean(true) // was successful
          .writeBoolean(false);
      } else if (result.wasSuccessful()) {
        // successful - write the result of the invocation
        return objectMapper.writeObject(dataBufFactory.createEmpty().writeBoolean(true), result.getInvocationResult());
      } else {
        // not successful - send some basic information about the result
        var throwable = (Throwable) result.getInvocationResult();
        return ExceptionalResultUtils.serializeThrowable(dataBufFactory.createEmpty().writeBoolean(false), throwable);
      }
    }
    // no result expected or no handler
    return null;
  }

  protected @Nullable HandlingResult handleRaw(@NotNull String clazz, @NotNull RPCInvocationContext context) {
    // get the handler associated with the class of the rpc
    var handler = this.rpcHandlerRegistry.getHandler(clazz);
    // invoke the handler with the information
    return handler == null ? null : handler.handle(context);
  }

  protected @NotNull RPCInvocationContext buildContext(
    @NotNull INetworkChannel channel,
    @NotNull DataBuf content,
    @Nullable Object on,
    boolean strictInstanceUsage
  ) {
    return RPCInvocationContext.builder()
      .workingInstance(on)
      .channel(channel)
      .methodName(content.readString())
      .expectsMethodResult(content.readBoolean())
      .argumentInformation(content)
      .normalizePrimitives(Boolean.TRUE)
      .strictInstanceUsage(strictInstanceUsage)
      .build();
  }
}
