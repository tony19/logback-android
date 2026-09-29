/**
 * Copyright 2019 Anthony Trinh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.qos.logback.core.joran.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.LocatorImpl;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import ch.qos.logback.core.joran.event.EndEvent;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.SaxEventRecorder;
import ch.qos.logback.core.joran.event.StartEvent;
import ch.qos.logback.core.joran.spi.ConfigurationWatchList;
import ch.qos.logback.core.joran.spi.ElementPath;
import ch.qos.logback.core.joran.spi.InterpretationContext;
import ch.qos.logback.core.joran.spi.Interpreter;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.joran.spi.SimpleRuleStore;
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil;
import ch.qos.logback.core.status.Status;

/**
 * Plain-JVM tests (no Robolectric) of {@link IncludeAction#processInclude}:
 * which recorded events are inserted into the event player, and how failures
 * to open or record the included document are reported. The recorder is
 * scripted instead of parsing XML, which needs Robolectric's xmlpull parser;
 * the end-to-end tests are in {@link IncludeActionTest}.
 */
public class IncludeActionJvmTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private final Context context = new ContextBase();
  private final Interpreter interpreter = new Interpreter(context, new SimpleRuleStore(context), new ElementPath());
  private final InterpretationContext ic = interpreter.getInterpretationContext();
  private final ScriptedIncludeAction action = new ScriptedIncludeAction();
  private final TrackedStreamHandler handler = new TrackedStreamHandler();
  private URL url;

  /** The event player's list: the <include> element being played, then the end of its parent. */
  private final List<SaxEvent> played = new ArrayList<SaxEvent>();

  @Before
  public void setUp() throws Exception {
    action.setContext(context);
    url = new URL(null, "inc:included.xml", handler);
    SaxEventRecorder recorder = new SaxEventRecorder(context);
    recorder.setDocumentLocator(new LocatorImpl());
    recorder.startElement("", "include", "include", new AttributesImpl());
    recorder.endElement("", "include", "include");
    recorder.endElement("", "x", "x");
    // an empty list leaves the player's current index at 0, i.e. at <include>
    interpreter.getEventPlayer().play(played);
    played.addAll(recorder.getSaxEventList());
  }

  @Test
  public void includedWrapperIsTrimmedAndTheContentInsertedAfterTheIncludeElement() throws JoranException {
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        start(recorder, "included");
        start(recorder, "stack");
        end(recorder, "stack");
        end(recorder, "included");
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "stack", "/stack", "/x"), describe(played));
    assertSame(handler.opened, action.recordedStream);
    assertSame(url, action.recordedUrl);
    assertSame(context, action.recorder.getContext());
    assertTrue(handler.opened.closed);
  }

  @Test
  public void includedFileIsAddedToTheConfigurationWatchList() throws Exception {
    ConfigurationWatchList watchList = new ConfigurationWatchList();
    watchList.setContext(context);
    ConfigurationWatchListUtil.registerConfigurationWatchList(context, watchList);
    File included = tmp.newFile("watched.xml");
    URL fileUrl = included.toURI().toURL();
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        start(recorder, "included");
        start(recorder, "stack");
        end(recorder, "stack");
        end(recorder, "included");
      }
    };

    action.processInclude(ic, fileUrl);

    assertEquals(Collections.singletonList(included.getAbsoluteFile()), watchList.getCopyOfFileWatchList());
    assertEquals(Status.INFO, findStatus("Adding [" + fileUrl + "] to configuration watch list.").getLevel());
    assertEquals(Arrays.asList("include", "/include", "stack", "/stack", "/x"), describe(played));
  }

  @Test
  public void configurationWrapperIsTrimmed() throws JoranException {
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        start(recorder, "configuration");
        start(recorder, "stack");
        end(recorder, "stack");
        end(recorder, "configuration");
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "stack", "/stack", "/x"), describe(played));
  }

  @Test
  public void closingElementNotMatchingTheIncludedWrapperIsKept() throws JoranException {
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        start(recorder, "included");
        start(recorder, "stack");
        end(recorder, "configuration");
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "stack", "/configuration", "/x"), describe(played));
  }

  @Test
  public void closingElementNotMatchingTheConfigurationWrapperIsKept() throws JoranException {
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        start(recorder, "configuration");
        start(recorder, "stack");
        end(recorder, "included");
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "stack", "/included", "/x"), describe(played));
  }

  @Test
  public void documentWithoutWrapperIsInsertedAsIs() throws JoranException {
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        start(recorder, "stack");
        end(recorder, "stack");
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "stack", "/stack", "/x"), describe(played));
  }

  @Test
  public void wrapperWithoutClosingElementLeavesNothingToInsert() throws JoranException {
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        start(recorder, "included");
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "/x"), describe(played));
  }

  @Test
  public void missingFirstEventIsNotTrimmed() throws JoranException {
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        recorder.getSaxEventList().add(null);
        start(recorder, "stack");
        end(recorder, "included");
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", null, "stack", "/included", "/x"), describe(played));
  }

  @Test
  public void missingLastEventIsKeptAfterTrimmingTheOpeningWrapper() throws JoranException {
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        start(recorder, "included");
        start(recorder, "stack");
        recorder.getSaxEventList().add(null);
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "stack", null, "/x"), describe(played));
  }

  @Test
  public void wrapperIsRecognizedByLocalNameWhenQualifiedNameIsEmpty() throws Exception {
    final StartEvent start = newStartEvent("included");
    final EndEvent end = newEndEvent("included");
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) {
        recorder.getSaxEventList().add(start);
        start(recorder, "stack");
        recorder.getSaxEventList().add(end);
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "stack", "/x"), describe(played));
  }

  @Test
  public void recordingFailureIsReportedAndTheStreamClosed() throws JoranException {
    final JoranException failure = new JoranException("broken document");
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) throws JoranException {
        start(recorder, "included");
        throw failure;
      }
    };

    action.processInclude(ic, url);

    assertEquals(Arrays.asList("include", "/include", "/x"), describe(played));
    assertTrue(handler.opened.closed);
    Status error = findStatus("Failed processing [inc:included.xml]");
    assertEquals(Status.ERROR, error.getLevel());
    assertSame(failure, error.getThrowable());
  }

  @Test
  public void recordingFailureIsIgnoredForAnOptionalInclude() throws Exception {
    // begin() decides whether the include is optional; with a file it goes on to processInclude()
    action.script = new Script() {
      public void record(SaxEventRecorder recorder) throws JoranException {
        throw new JoranException("broken document");
      }
    };
    DummyAttributes atts = new DummyAttributes();
    atts.setValue("file", tmp.newFile("included.xml").getPath());
    atts.setValue("optional", "true");

    action.begin(ic, "include", atts);

    assertEquals(Arrays.asList("include", "/include", "/x"), describe(played));
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      assertFalse(s.getMessage(), s.getMessage().startsWith("Failed processing"));
    }
  }

  @Test
  public void unopenableUrlIsReportedAndNothingIsRecorded() throws Exception {
    URL missing = new URL(tmp.getRoot().toURI().toURL(), "missing.xml");

    action.processInclude(ic, missing);

    assertEquals(Arrays.asList("include", "/include", "/x"), describe(played));
    assertNull(action.recorder);
    Status warning = findStatus("Failed to open [" + missing + "]");
    assertEquals(Status.WARN, warning.getLevel());
    assertTrue(warning.getThrowable() instanceof FileNotFoundException);
  }

  private Status findStatus(String message) {
    for (Status s : context.getStatusManager().getCopyOfStatusList()) {
      if (message.equals(s.getMessage())) {
        return s;
      }
    }
    throw new AssertionError("no status [" + message + "] in " + context.getStatusManager().getCopyOfStatusList());
  }

  private static void start(SaxEventRecorder recorder, String name) {
    recorder.startElement("", name, name, new AttributesImpl());
  }

  private static void end(SaxEventRecorder recorder, String name) {
    recorder.endElement("", name, name);
  }

  /** Describes events by qualified name, "/name" for end events. */
  private static List<String> describe(List<SaxEvent> events) {
    List<String> names = new ArrayList<String>();
    for (SaxEvent e : events) {
      names.add(e == null ? null : (e instanceof EndEvent ? "/" : "") + e.getQName());
    }
    return names;
  }

  // SaxEventRecorder always fills in the qualified name, so events without one are built directly

  private static StartEvent newStartEvent(String localName) throws Exception {
    Constructor<StartEvent> c = StartEvent.class.getDeclaredConstructor(ElementPath.class, String.class,
        String.class, String.class, org.xml.sax.Attributes.class, Locator.class);
    c.setAccessible(true);
    return c.newInstance(new ElementPath(localName), "", localName, "", new AttributesImpl(), new LocatorImpl());
  }

  private static EndEvent newEndEvent(String localName) throws Exception {
    Constructor<EndEvent> c = EndEvent.class.getDeclaredConstructor(String.class, String.class, String.class,
        Locator.class);
    c.setAccessible(true);
    return c.newInstance("", localName, "", new LocatorImpl());
  }

  /** Stands in for parsing the included document. */
  interface Script {
    void record(SaxEventRecorder recorder) throws JoranException;
  }

  /** An include action whose recorder runs a script instead of parsing XML. */
  static class ScriptedIncludeAction extends IncludeAction {
    Script script;
    SaxEventRecorder recorder;
    InputStream recordedStream;
    URL recordedUrl;

    @Override
    protected SaxEventRecorder createRecorder(InputStream in, URL url) {
      recordedStream = in;
      recordedUrl = url;
      recorder = new SaxEventRecorder() {
        @Override
        public List<SaxEvent> recordEvents(InputSource inputSource) throws JoranException {
          assertSame(recordedStream, inputSource.getByteStream());
          // as a parser would
          setDocumentLocator(new LocatorImpl());
          script.record(this);
          return getSaxEventList();
        }
      };
      return recorder;
    }
  }

  /** Serves an empty document and remembers the stream it opened. */
  static class TrackedStreamHandler extends URLStreamHandler {
    TrackedStream opened;

    @Override
    protected URLConnection openConnection(URL u) {
      return new URLConnection(u) {
        @Override
        public void connect() {
        }

        @Override
        public InputStream getInputStream() {
          opened = new TrackedStream();
          return opened;
        }
      };
    }
  }

  static class TrackedStream extends ByteArrayInputStream {
    boolean closed;

    TrackedStream() {
      super(new byte[0]);
    }

    @Override
    public void close() throws IOException {
      closed = true;
      super.close();
    }
  }
}
