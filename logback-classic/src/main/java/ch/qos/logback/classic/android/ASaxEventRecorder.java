/**
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 2011-2012, Anthony Trinh. All rights reserved.
 * Copyright (C) 1999-2011, QOS.ch. All rights reserved.
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
package ch.qos.logback.classic.android;

import java.io.InputStream;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;
import org.xmlpull.v1.XmlPullParser;

import brut.androlib.res.decoder.AXmlResourceParser;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.spi.JoranException;

/**
 * SAX event recorder for compressed Android XML resource files.
 * Supports filtering to capture only the sub-events of an event
 * of interest in order to conserve memory usage.
 *
 * @author Anthony Trinh
 */
public class ASaxEventRecorder extends SaxEventRecorder {
  private int holderForStartAndLength[] = new int[2];
  private StatePassFilter filter = new StatePassFilter();
  private String attrNameToWatch = null;
  private String elemNameToWatch = null;
  private String attrWatchValue = null;

  /**
   * Constructor
   */
  public ASaxEventRecorder() {
    super();
  }

  /**
   * Sets a filter so that only sub-elements of a specific element
   * are captured
   *
   * <p>
   * For example, if the desired elements were inside
   * <br>
   * <blockquote>{@code <x><y>}</blockquote>
   * and the input were
   * <br>
   * <blockquote>{@code <x><y><a/><b/><c/></x></y>}</blockquote>
   * the filter would pass
   * <br>
   * <blockquote>{@code <a/><b/><c/>}</blockquote>
   *
   * The call in this example would be: {@code setFilter("x", "y")}.
   *
   * @param names names of elements leading to the target elements;
   * use {@code null} to disable filtering (capture all events)
   */
  public void setFilter(String... names) {
    filter = new StatePassFilter(names);
  }

  /**
   * Sets a "watch" for an element's attribute, which can be retrieved
   * with {@link #getAttributeWatchValue()}. During the parsing of the SAX
   * events, the START-elements are searched for the target attribute.
   * If found, the attribute's value is stored. This checks all START-
   * elements, regardless of filtering.
   *
   * @param elemName name of the element that contains the attribute
   * @param attrName name of the attribute
   */
  public void setAttributeWatch(String elemName, String attrName) {
    elemNameToWatch = elemName;
    attrNameToWatch = attrName;
  }

  /**
   * Gets the value of the attribute set by {@link #setAttributeWatch(String, String)}
   *
   * @return the string value of the attribute; if the attribute was
   * not encountered, this returns null
   */
  public String getAttributeWatchValue() {
    return attrWatchValue;
  }

  /**
   * Parses SAX events from a compressed Android XML resource
   *
   * @param src input source pointing to a compressed Android XML resource
   */
  @Override
  public List<SaxEvent> recordEvents(InputSource src) throws JoranException {
    InputStream stream = src.getByteStream();
    if (stream == null) {
      throw new IllegalArgumentException("Input source must specify an input stream");
    }

    List<SaxEvent> events = null;
    try {
      AXmlResourceParser xpp = new AXmlResourceParser(stream);

      attrWatchValue = null;
      int eventType = -1;
      while ((eventType = xpp.next()) > -1) {
        if (XmlPullParser.START_DOCUMENT == eventType) {
          filter.reset();
          startDocument(xpp);
        } else if (XmlPullParser.END_DOCUMENT == eventType) {
          filter.reset();
          endDocument();
          break;
        } else if (XmlPullParser.START_TAG == eventType) {
          startElement(xpp);
        } else if (XmlPullParser.END_TAG == eventType) {
          endElement(xpp);
        } else if (XmlPullParser.TEXT == eventType) {
          characters(xpp);
        }
      }

      events = getSaxEventList();

    } catch (Exception e) {
      addError(e.getMessage(), e);
      throw new JoranException("Can't parse Android XML resource", e);
    }
    return events;
  }

  /**
   * Processes the START_DOCUMENT event
   *
   * @param xpp parser that contains the event
   */
  private void startDocument(XmlPullParser xpp) {
    super.startDocument();
    super.setDocumentLocator(new LocatorImpl());
  }

  /**
   * Processes the TEXT event
   *
   * @param xpp parser that contains the event
   */
  private void characters(XmlPullParser xpp) {
    if (filter.passed()) {
      char ch[] = xpp.getTextCharacters(holderForStartAndLength);
      int start = holderForStartAndLength[0];
      int length = holderForStartAndLength[1];
      super.characters(ch, start, length);
    }
  }

  /**
   * Process the END_ELEMENT event
   *
   * @param xpp parser that contains the event
   */
  private void endElement(XmlPullParser xpp) {
    String name = xpp.getName();
    if (filter.checkEnd(name)) {
      endElement(xpp.getNamespace(), name, name);
    }
  }

  /**
   * Processes the START_ELEMENT event
   *
   * @param xpp parser that contains the event
   */
  private void startElement(XmlPullParser xpp) {
    String name = xpp.getName();
    if (filter.checkStart(name)) {
      AttributesImpl atts = new AttributesImpl();
      for (int i = 0; i < xpp.getAttributeCount(); i++) {
        atts.addAttribute(xpp.getAttributeNamespace(i),
            xpp.getAttributeName(i), xpp.getAttributeName(i),
            xpp.getAttributeType(i), xpp.getAttributeValue(i));
      }
      startElement(xpp.getNamespace(), name, name, atts);
    }
    // check for attributes (and ignore filter)
    checkForWatchedAttribute(xpp);
  }

  /**
   * Checks a START-element for the watched attribute, set by
   * {@link #setAttributeWatch(String, String)}. If the attribute
   * was already found, this does nothing.
   *
   * @param xpp parser that contains the START_ELEMENT event
   */
  private void checkForWatchedAttribute(XmlPullParser xpp) {
    if (elemNameToWatch != null &&
        attrWatchValue == null &&
        xpp.getName().equals(elemNameToWatch)) {
      for (int i = 0; i < xpp.getAttributeCount(); i++) {
        if (xpp.getAttributeName(i).equals(attrNameToWatch)) {
            attrWatchValue = xpp.getAttributeValue(i);
        }
      }
    }
  }

  /**
   * Filter that passes start-tags and end-tags within a specific
   * set of tags.
   *
   * <p>
   * This is useful in an XML pull parser for capturing XML sub-elements
   * of a specific element. For example, if the desired elements were in
   * <br>
   * <blockquote>{@code <x><y>}</blockquote>
   * and the input were
   * <br>
   * <blockquote>{@code <x><y><a/><b/><c/></x></y>}</blockquote>
   * the filter would pass
   * <br>
   * <blockquote>{@code <a/><b/><c/>}</blockquote>
   *
   * The initialization in this example would be: {@code new StatePassFilter("x", "y")}.
   */
  static class StatePassFilter {
    private final String[] _states;
    private int _depth = 0;

    public StatePassFilter(String... states) {
      _states = states == null ? new String[0] : states;
    }

    /**
     * Checks if a start-tag element name passes the filter. If the
     * name matches the filter's current state, the filter depth
     * is advanced.
     *
     * @param name element name to check
     * @return {@true code} if passed; {@code false} otherwise
     */
    public boolean checkStart(String name) {
      if (_depth == _states.length) {
        return true;
      } else if (name.equals(_states[_depth])) {
        _depth++;
      }
      return false;
    }

    /**
     * Checks if an end-tag element name passes the filter. If the
     * name matches the filter's current state, the filter depth
     * is decremented.
     *
     * @param name element name to check
     * @return {@true code} if passed; {@code false} otherwise
     */
    public boolean checkEnd(String name) {
      if ((_depth > 0) && name.equals(_states[_depth - 1])) {
        _depth--;
        return false;
      }
      return _depth == _states.length;
    }

    /**
     * Gets the number of states in the filter
     *
     * @return state count
     */
    public int size() {
      return _states.length;
    }

    /**
     * Gets the current depth (state) into the filter
     *
     * @return depth
     */
    public int depth() {
      return _depth;
    }

    /**
     * Resets the depth (state)
     */
    public void reset() {
      _depth = 0;
    }

    /**
     * Gets the pass state. Equivalent to {@code depth() == size()}.
     *
     * @return {@code true} if passed; {@code false} otherwise
     */
    public boolean passed() {
      return _depth == _states.length;
    }
  }
}
