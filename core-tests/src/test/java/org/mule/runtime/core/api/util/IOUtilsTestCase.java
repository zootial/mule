/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static java.lang.Thread.currentThread;
import static java.nio.charset.Charset.forName;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_STREAMING_BUFFER_SIZE;
import static org.mule.runtime.core.api.util.ClassUtils.loadClass;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.List;

import org.junit.Test;
import sun.misc.Unsafe;

@SmallTest
public class IOUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void testLoadingResourcesAsStream() throws Exception {
    InputStream is = IOUtils.getResourceAsStream("log4j2-test.xml", getClass(), false, false);
    assertNotNull(is);

    is = IOUtils.getResourceAsStream("does-not-exist.properties", getClass(), false, false);
    assertNull(is);
  }

  @Test
  public void bufferSize() throws Exception {
    InputStream in = new ByteArrayInputStream(new byte[8 * 1024]);
    OutputStream out = mock(OutputStream.class);

    IOUtils.copyLarge(in, out);

    // Default buffer size of 4KB required two reads to copy 8KB input stream
    verify(out, times(2)).write(any(byte[].class), anyInt(), anyInt());
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void increaseBufferSizeViaSystemProperty() throws Exception {
    final int newBufferSize = 8 * 1024;

    testWithSystemProperty(MULE_STREAMING_BUFFER_SIZE, Integer.toString(newBufferSize), () -> {
      InputStream in = new ByteArrayInputStream(new byte[newBufferSize]);
      OutputStream out = mock(OutputStream.class);

      ClassLoader contextClassLoader = currentThread().getContextClassLoader();
      ClassLoader newClassLoader = new URLClassLoader(getClassloaderURLs(contextClassLoader), null);
      Class clazz = loadClass(IOUtils.class.getCanonicalName(), newClassLoader);

      try {
        currentThread().setContextClassLoader(newClassLoader);
        clazz.getMethod("copyLarge", InputStream.class, OutputStream.class).invoke(clazz, in, out);
      } finally {
        currentThread().setContextClassLoader(contextClassLoader);
      }

      // With 8KB buffer define via system property only 1 read is required for 8KB
      // input stream
      verify(out, times(1)).write(any(byte[].class), anyInt(), anyInt());
    });
  }

  private URL[] getClassloaderURLs(ClassLoader classLoader) {
    if (classLoader instanceof URLClassLoader) {
      return ((URLClassLoader) classLoader).getURLs();
    }

    if (classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
      try {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);

        // jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
        Field ucpField = classLoader.getClass().getDeclaredField("ucp");
        long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
        Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

        // jdk.internal.loader.URLClassPath.path
        Field pathField = ucpField.getType().getDeclaredField("path");
        long pathFieldOffset = unsafe.objectFieldOffset(pathField);
        List<URL> path = (List<URL>) unsafe.getObject(ucpObject, pathFieldOffset);

        return path.toArray(new URL[path.size()]);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    throw new IllegalArgumentException("Unknown classloader type: " + classLoader);
  }

  @Test
  public void convertsToStringWithEncoding() throws Exception {

    final Charset encoding = forName("EUC-JP");
    final String encodedText = "\u3053";
    InputStream in = new ByteArrayInputStream(encodedText.getBytes(encoding));

    String converted = IOUtils.toString(in, encoding);

    assertThat(converted, equalTo(encodedText));
  }
}
