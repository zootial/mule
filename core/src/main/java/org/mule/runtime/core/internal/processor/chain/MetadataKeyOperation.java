/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.chain;

import org.mule.runtime.core.api.processor.AbstractMessageProcessorOwner;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

import java.util.Collections;
import java.util.List;

public class MetadataKeyOperation extends AbstractMessageProcessorOwner {

  private MessageProcessorChain processorChain;

  public MessageProcessorChain getProcessorChain() {
    return processorChain;
  }

  public void setProcessorChain(MessageProcessorChain processorChain) {
    this.processorChain = processorChain;
  }

  @Override
  protected List<Processor> getOwnedMessageProcessors() {
    return Collections.singletonList(processorChain);
  }


}
