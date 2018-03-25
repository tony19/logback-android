/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2013, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */
package ch.qos.logback.core.joran.action;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;

import org.xml.sax.Attributes;

import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.Loader;
import ch.qos.logback.core.util.OptionHelper;

public abstract class AbstractIncludeAction extends Action {

  private static final String FILE_ATTR = "file";
  private static final String URL_ATTR = "url";
  private static final String RESOURCE_ATTR = "resource";
  private static final String OPTIONAL_ATTR = "optional";

  private String attributeInUse;
  private boolean optional;

  abstract protected void processInclude(InterpretationContext ic, URL url) throws JoranException;

  protected void handleError(String message, Exception e) {
    if ((e != null)
            && ((e instanceof FileNotFoundException) || (e instanceof UnknownHostException))) {
      addWarn(message, e);
    } else {
      addError(message, e);
    }
  }

  @Override
  public void begin(InterpretationContext ec, String name, Attributes attributes)
          throws ActionException {

    this.attributeInUse = null;
    this.optional = OptionHelper.toBoolean(attributes.getValue(OPTIONAL_ATTR), false);

    if (!checkAttributes(attributes)) {
      return;
    }

    try {
      URL url = getInputURL(ec, attributes);
      if (url != null) {
        processInclude(ec, url);
      }
    } catch (JoranException e) {
      optionalWarning("Error while parsing " + attributeInUse, e);
    }

  }

  protected void close(InputStream in) {
    if (in != null) {
      try {
        in.close();
      } catch (IOException e) {
      }
    }
  }

  private boolean checkAttributes(Attributes attributes) {
    String fileAttribute = attributes.getValue(FILE_ATTR);
    String urlAttribute = attributes.getValue(URL_ATTR);
    String resourceAttribute = attributes.getValue(RESOURCE_ATTR);

    int count = 0;

    if (!OptionHelper.isEmpty(fileAttribute)) {
      count++;
    }
    if (!OptionHelper.isEmpty(urlAttribute)) {
      count++;
    }
    if (!OptionHelper.isEmpty(resourceAttribute)) {
      count++;
    }

    if (count == 0) {
      optionalWarning(String.format("One of \"%1$s\", \"%2$s\" or \"%3$s\" attributes must be set.", FILE_ATTR, RESOURCE_ATTR, URL_ATTR), null);
      return false;
    } else if (count > 1) {
      optionalWarning(String.format("Only one of \"%1$s\", \"%2$s\" or \"%3$s\" attributes should be set.", FILE_ATTR, RESOURCE_ATTR, URL_ATTR), null);
      return false;
    } else if (count == 1) {
      return true;
    }
    throw new IllegalStateException("Count value [" + count
            + "] is not expected");
  }

  private URL attributeToURL(String urlAttribute) {
    try {
      URL url = new URL(urlAttribute);

      // Test URL connection to make sure it points to something real.
      // If it fails, we'll hit the IOException.
      InputStream stream = url.openStream();
      stream.close();
      stream = null;

      return url;
    } catch (MalformedURLException mue) {
      optionalWarning("URL [" + urlAttribute + "] is not well formed.", mue);
    } catch (IOException e) {
      optionalWarning("URL [" + urlAttribute + "] cannot be opened.", e);
    }
    return null;
  }

  private URL resourceAsURL(String resourceAttribute) {
    URL url = Loader.getResourceBySelfClassLoader(resourceAttribute);
    if (url == null) {
      optionalWarning("Could not find resource corresponding to ["
                + resourceAttribute + "]", null);
      return null;
    } else
      return url;
  }

  private URL filePathAsURL(String path) {
    File file = new File(path);
    if (!file.exists() || !file.isFile()) {
      optionalWarning("File does not exist [" + path + "]", new FileNotFoundException(path));
      return null;
    }

    URI uri = file.toURI();
    try {
      return uri.toURL();
    } catch (MalformedURLException e) {
      // impossible to get here
      e.printStackTrace();
      return null;
    }
  }

  protected String getAttributeInUse() {
    return this.attributeInUse;
  }

  protected boolean isOptional() {
    return this.optional;
  }

  private URL getInputURL(InterpretationContext ec, Attributes attributes) {
    String fileAttribute = attributes.getValue(FILE_ATTR);
    String urlAttribute = attributes.getValue(URL_ATTR);
    String resourceAttribute = attributes.getValue(RESOURCE_ATTR);

    if (!OptionHelper.isEmpty(fileAttribute)) {
      this.attributeInUse = ec.subst(fileAttribute);
      return filePathAsURL(attributeInUse);
    }

    if (!OptionHelper.isEmpty(urlAttribute)) {
      this.attributeInUse = ec.subst(urlAttribute);
      return attributeToURL(attributeInUse);
    }

    if (!OptionHelper.isEmpty(resourceAttribute)) {
      this.attributeInUse = ec.subst(resourceAttribute);
      return resourceAsURL(attributeInUse);
    }
    // given previous checkAttributes() check we cannot reach this line
    throw new IllegalStateException("A URL stream should have been returned");

  }

  @Override
  public void end(InterpretationContext ec, String name) throws ActionException {
    // do nothing
  }

  protected void optionalWarning(String msg, Exception e) {
    if (!isOptional()) {
      handleError(msg, e);
    }
  }
}
