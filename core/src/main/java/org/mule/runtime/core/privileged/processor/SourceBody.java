/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import java.util.List;

import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;

public class SourceBody {

  private MessageSource source;
  private List<Processor> messageProcessors;

  public void setSource(MessageSource source) {
    this.source = source;
  }

  public void setMessageProcessors(List<Processor> messageProcessors) {
    this.messageProcessors = messageProcessors;
  }

  public MessageSource getSource() {
    return source;
  }

  public List<Processor> getMessageProcessors() {
    return messageProcessors;
  }
}
