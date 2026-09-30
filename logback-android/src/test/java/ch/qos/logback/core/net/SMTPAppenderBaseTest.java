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
package ch.qos.logback.core.net;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Marker;

import ch.qos.logback.classic.ClassicConstants;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SMTPAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.boolex.EvaluationException;
import ch.qos.logback.core.boolex.EventEvaluator;
import ch.qos.logback.core.helpers.CyclicBuffer;
import ch.qos.logback.core.sift.DefaultDiscriminator;
import ch.qos.logback.core.sift.Discriminator;
import ch.qos.logback.core.spi.CyclicBufferTracker;
import ch.qos.logback.core.status.Status;
import ch.qos.logback.core.status.StatusChecker;

public class SMTPAppenderBaseTest {

  private static final String CHECK_SERVER_IDENTITY = "mail.smtp.ssl.checkserveridentity";
  private static final String DEFAULT_SENDER = "default-sender@example.com";
  private static final String NO_PARSE_ADDRESS = "<unterminated";

  private final LoggerContext context = new LoggerContext();
  private final StatusChecker checker = new StatusChecker(context);
  private final List<Message> sentMessages = new ArrayList<Message>();

  private final SMTPAppender appender = new SMTPAppender();

  /**
   * Stands in for the SMTP server: every message handed to
   * {@link Transport#send(Message)} on the test thread is recorded in
   * {@link #sentMessages} instead of being delivered.
   */
  private MockedStatic<Transport> transport;

  @Before
  public void setUp() {
    transport = mockStatic(Transport.class);
    transport.when(() -> Transport.send(any(Message.class))).thenAnswer(invocation -> {
      sentMessages.add(invocation.getArgument(0));
      return null;
    });
  }

  @After
  public void tearDown() {
    transport.close();
    System.clearProperty(CHECK_SERVER_IDENTITY);
  }

  private void start() {
    appender.setContext(context);
    appender.setSMTPHost("localhost");
    appender.addTo("nospam@qos.ch");
    appender.start();
    assertTrue(appender.isStarted());
  }

  /**
   * Sets up {@code a} so that it can append: context, name, a "[%msg]"
   * layout, and synchronous sending. Recipients are left to the caller.
   */
  private <A extends SMTPAppender> A configure(A a) {
    a.setContext(context);
    a.setName("smtp");
    a.setLayout(patternLayout("[%msg]"));
    a.setAsynchronousSending(false);
    return a;
  }

  /**
   * Starts {@code a} and gives its mail session a default sender, used when no
   * "from" is set (this also avoids a lookup of the local host name).
   */
  private <A extends SMTPAppender> A startForSending(A a) {
    a.start();
    assertTrue(a.isStarted());
    a.session.getProperties().put("mail.from", DEFAULT_SENDER);
    return a;
  }

  /** {@link #appender} seen as the class under test, to reach its package-private members. */
  private SMTPAppenderBase<ILoggingEvent> base() {
    return appender;
  }

  private PatternLayout patternLayout(String pattern) {
    PatternLayout layout = new PatternLayout();
    layout.setContext(context);
    layout.setPattern(pattern);
    layout.start();
    return layout;
  }

  private LoggingEvent event(Level level, String message) {
    return new LoggingEvent(getClass().getName(), context.getLogger("test"), level, message, null, null);
  }

  private MimeMessage onlySentMessage() {
    assertEquals(1, sentMessages.size());
    return (MimeMessage) sentMessages.get(0);
  }

  private static MimeBodyPart onlyBodyPart(MimeMessage message) throws Exception {
    MimeMultipart multipart = (MimeMultipart) message.getContent();
    assertEquals(1, multipart.getCount());
    return (MimeBodyPart) multipart.getBodyPart(0);
  }

  private static String body(MimeMessage message) throws Exception {
    return (String) onlyBodyPart(message).getContent();
  }

  /**
   * The content type the appender gave the part, as held by its DataHandler.
   * Read reflectively: DataHandler implements java.awt.datatransfer.Transferable,
   * which Android's Java library (that the unit tests are compiled against)
   * doesn't have, so the test can't name the type.
   */
  private static String contentType(MimeBodyPart part) throws Exception {
    Object dataHandler = MimeBodyPart.class.getMethod("getDataHandler").invoke(part);
    return (String) dataHandler.getClass().getMethod("getContentType").invoke(dataHandler);
  }

  private static List<String> addresses(Address[] addresses) {
    List<String> result = new ArrayList<String>();
    if (addresses != null) {
      for (Address address : addresses) {
        result.add(((InternetAddress) address).getAddress());
      }
    }
    return result;
  }

  private int bufferedEventCount() {
    return appender.getCyclicBufferTracker().getOrCreate(DefaultDiscriminator.DEFAULT, 0).length();
  }

  // ---------------------------------------------------------------- start()

  @Test
  public void checksServerIdentityWithSSL() {
    appender.setSSL(true);
    start();
    assertEquals("true", appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }

  @Test
  public void checksServerIdentityWithSTARTTLS() {
    appender.setSTARTTLS(true);
    start();
    assertEquals("true", appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }

  @Test
  public void systemPropertyOverridesServerIdentityCheck() {
    System.setProperty(CHECK_SERVER_IDENTITY, "false");
    appender.setSSL(true);
    start();
    assertEquals("false", appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }

  @Test
  public void plainSMTPLeavesServerIdentityCheckUnset() {
    start();
    assertNull(appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }

  @Test
  public void sslIsEnabledOnTheMailSession() {
    appender.setSSL(true);
    start();
    assertEquals("true", appender.session.getProperty("mail.smtp.ssl.enable"));
    assertNull(appender.session.getProperty("mail.smtp.starttls.enable"));
    checker.assertIsErrorFree();
  }

  @Test
  public void startTlsIsEnabledOnTheMailSession() {
    appender.setSTARTTLS(true);
    start();
    assertEquals("true", appender.session.getProperty("mail.smtp.starttls.enable"));
    assertEquals("true", appender.session.getProperty("mail.transport.protocol"));
    assertNull(appender.session.getProperty("mail.smtp.ssl.enable"));
    checker.assertIsErrorFree();
  }

  @Test
  public void plainSMTPEnablesNeitherSslNorStartTls() {
    start();
    assertNull(appender.session.getProperty("mail.smtp.ssl.enable"));
    assertNull(appender.session.getProperty("mail.smtp.starttls.enable"));
  }

  @Test
  public void sslTogetherWithStartTlsIsReportedAndEnablesNeither() {
    appender.setSSL(true);
    appender.setSTARTTLS(true);
    assertTrue(appender.isSSL());
    assertTrue(appender.isSTARTTLS());

    start();

    checker.assertContainsMatch(Status.ERROR, "Both SSL and StartTLS cannot be enabled simultaneously");
    assertNull(appender.session.getProperty("mail.smtp.ssl.enable"));
    assertNull(appender.session.getProperty("mail.smtp.starttls.enable"));
    assertNull(appender.session.getProperty(CHECK_SERVER_IDENTITY));
  }

  @Test
  public void hostPortAndLocalhostArePassedToTheMailSession() {
    appender.setContext(context);
    appender.setSmtpHost("mail.example.com");
    appender.setSmtpPort(2525);
    appender.setLocalhost("client.example.com");
    appender.start();

    assertTrue(appender.isStarted());
    assertEquals("mail.example.com", appender.getSmtpHost());
    assertEquals("mail.example.com", appender.getSMTPHost());
    assertEquals("mail.example.com", appender.session.getProperty("mail.smtp.host"));
    assertEquals(2525, appender.getSmtpPort());
    assertEquals(2525, appender.getSMTPPort());
    assertEquals("2525", appender.session.getProperty("mail.smtp.port"));
    assertEquals("client.example.com", appender.getLocalhost());
    assertEquals("client.example.com", appender.session.getProperty("mail.smtp.localhost"));
  }

  @Test
  public void setSMTPPortIsAnAliasOfSetSmtpPort() {
    appender.setSMTPPort(465);
    assertEquals(465, appender.getSmtpPort());
  }

  @Test
  public void unsetHostAndLocalhostAreLeftOutOfTheMailSession() {
    appender.setContext(context);
    appender.start();

    assertTrue(appender.isStarted());
    assertNull(appender.getSmtpHost());
    assertNull(appender.session.getProperty("mail.smtp.host"));
    assertEquals("25", appender.session.getProperty("mail.smtp.port"));
    assertNull(appender.getLocalhost());
    assertNull(appender.session.getProperty("mail.smtp.localhost"));
  }

  @Test
  public void usernameEnablesAuthenticationWithTheConfiguredCredentials() {
    appender.setUsername("alice");
    appender.setPassword("secret");
    assertEquals("alice", appender.getUsername());
    assertEquals("secret", appender.getPassword());

    start();

    assertEquals("true", appender.session.getProperty("mail.smtp.auth"));
    PasswordAuthentication credentials =
        appender.session.requestPasswordAuthentication(null, 25, "smtp", null, null);
    assertNotNull(credentials);
    assertEquals("alice", credentials.getUserName());
    assertEquals("secret", credentials.getPassword());
  }

  @Test
  public void withoutUsernameTheSessionHasNoAuthentication() {
    start();

    assertNull(appender.session.getProperty("mail.smtp.auth"));
    assertNull(appender.session.requestPasswordAuthentication(null, 25, "smtp", null, null));
  }

  @Test
  public void startKeepsACyclicBufferTrackerSetBeforehand() {
    CyclicBufferTracker<ILoggingEvent> tracker = new CyclicBufferTracker<ILoggingEvent>();
    appender.setCyclicBufferTracker(tracker);
    start();
    assertSame(tracker, appender.getCyclicBufferTracker());
  }

  @Test
  public void startCreatesACyclicBufferTrackerWhenNoneIsSet() {
    assertNull(appender.getCyclicBufferTracker());
    start();
    assertNotNull(appender.getCyclicBufferTracker());
  }

  @Test
  public void startFailsWhenNoMailSessionCanBeObtained() {
    appender.setContext(context);
    try (MockedStatic<Session> sessions = mockStatic(Session.class)) {
      sessions.when(() -> Session.getInstance(any(Properties.class), nullable(Authenticator.class)))
          .thenReturn(null);
      appender.start();
    }

    assertFalse(appender.isStarted());
    assertNull(appender.subjectLayout);
    checker.assertContainsMatch(Status.ERROR, "Failed to obtain javax.mail.Session. Cannot start.");
  }

  @Test
  public void stopMarksTheAppenderAsNotStarted() {
    start();
    appender.stop();
    assertFalse(appender.isStarted());
  }

  // ------------------------------------------------------ append() entry checks

  @Test
  public void appendingToAStoppedAppenderIsReportedAndBuffersNothing() throws Exception {
    startForSending(configure(appender));
    appender.addTo("to@example.com");
    appender.stop();

    appender.append(event(Level.ERROR, "dropped"));

    checker.assertContainsMatch(Status.ERROR, "Attempting to append to a non-started appender: smtp");
    assertFalse(appender.checkEntryConditions());
    assertEquals(0, appender.getCyclicBufferTracker().getComponentCount());
    assertTrue(sentMessages.isEmpty());
  }

  @Test
  public void appendingWithoutAnEvaluatorIsReportedAndBuffersNothing() throws Exception {
    startForSending(configure(appender));
    appender.addTo("to@example.com");
    appender.setEvaluator(null);

    assertFalse(appender.checkEntryConditions());
    checker.assertContainsMatch(Status.ERROR, "No EventEvaluator is set for appender \\[smtp\\]\\.");

    appender.append(event(Level.ERROR, "dropped"));

    assertEquals(0, appender.getCyclicBufferTracker().getComponentCount());
    assertTrue(sentMessages.isEmpty());
  }

  @Test
  public void appendingWithoutALayoutIsReportedAndBuffersNothing() throws Exception {
    startForSending(configure(appender));
    appender.addTo("to@example.com");
    appender.setLayout(null);

    assertFalse(appender.checkEntryConditions());
    checker.assertContainsMatch(Status.ERROR, "No layout set for appender named \\[smtp\\]\\. "
        + "For more information, please visit http://logback.qos.ch/codes.html#smtp_no_layout");

    appender.append(event(Level.ERROR, "dropped"));

    assertEquals(0, appender.getCyclicBufferTracker().getComponentCount());
    assertTrue(sentMessages.isEmpty());
  }

  @Test
  public void checkEntryConditionsPassesForAFullyConfiguredStartedAppender() throws Exception {
    startForSending(configure(appender));
    assertTrue(appender.checkEntryConditions());
    checker.assertIsErrorFree();
  }

  // ------------------------------------------------------ append() evaluation

  @Test
  public void nonTriggeringEventIsBufferedWithoutSending() throws Exception {
    startForSending(configure(appender));
    appender.addTo("to@example.com");

    appender.append(event(Level.DEBUG, "kept"));

    assertTrue(sentMessages.isEmpty());
    assertEquals(1, bufferedEventCount());
  }

  @Test
  public void triggeringEventSendsTheBufferSynchronouslyAndClearsIt() throws Exception {
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);
    assertFalse(appender.isAsynchronousSending());

    appender.append(event(Level.DEBUG, "one"));
    appender.append(event(Level.ERROR, "two"));

    MimeMessage message = onlySentMessage();
    assertEquals("[one][two]", body(message));
    assertEquals(Collections.singletonList("to@example.com"),
        addresses(message.getRecipients(Message.RecipientType.TO)));
    assertEquals(0, bufferedEventCount());

    // the next e-mail only carries what was logged after the previous one
    appender.append(event(Level.ERROR, "three"));
    assertEquals(2, sentMessages.size());
    assertEquals("[three]", body((MimeMessage) sentMessages.get(1)));
  }

  @Test
  public void triggeringEventIsSentAsynchronouslyThroughTheContextExecutor() throws Exception {
    final ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
    LoggerContext asyncContext = new LoggerContext() {
      @Override
      public synchronized ScheduledExecutorService getScheduledExecutorService() {
        return executor;
      }
    };
    SMTPAppender asyncAppender = new SMTPAppender();
    asyncAppender.setContext(asyncContext);
    asyncAppender.setName("smtp");
    asyncAppender.setLayout(patternLayout("[%msg]"));
    asyncAppender.addTo("to@example.com");
    assertTrue(asyncAppender.isAsynchronousSending());
    startForSending(asyncAppender);

    asyncAppender.append(new LoggingEvent("fqcn", asyncContext.getLogger("test"), Level.DEBUG, "one", null, null));
    asyncAppender.append(new LoggingEvent("fqcn", asyncContext.getLogger("test"), Level.ERROR, "two", null, null));

    ArgumentCaptor<Runnable> sender = ArgumentCaptor.forClass(Runnable.class);
    verify(executor).execute(sender.capture());
    assertTrue("nothing is sent before the executor runs the task", sentMessages.isEmpty());
    // the buffer is handed over (cloned) to the task and cleared right away
    assertEquals(0, asyncAppender.getCyclicBufferTracker().getOrCreate(DefaultDiscriminator.DEFAULT, 0).length());

    sender.getValue().run();

    assertEquals("[one][two]", body(onlySentMessage()));
  }

  @Test
  public void evaluatorExceptionsAreReportedOnlyUpToTheMaximumErrorCount() throws Exception {
    @SuppressWarnings("unchecked")
    EventEvaluator<ILoggingEvent> evaluator = mock(EventEvaluator.class);
    when(evaluator.evaluate(any(ILoggingEvent.class))).thenThrow(new EvaluationException("boom"));
    appender.setEvaluator(evaluator);
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);

    for (int i = 0; i < CoreConstants.MAX_ERROR_COUNT + 2; i++) {
      appender.append(event(Level.ERROR, "e" + i));
    }

    assertEquals(CoreConstants.MAX_ERROR_COUNT - 1,
        checker.matchCount("SMTPAppender's EventEvaluator threw an Exception-"));
    assertTrue(sentMessages.isEmpty());
    assertEquals(CoreConstants.MAX_ERROR_COUNT + 2, bufferedEventCount());
  }

  @Test
  public void eventsAreBufferedUnderTheDiscriminatingValue() throws Exception {
    @SuppressWarnings("unchecked")
    Discriminator<ILoggingEvent> discriminator = mock(Discriminator.class);
    when(discriminator.getDiscriminatingValue(any(ILoggingEvent.class))).thenReturn("session-42");
    appender.setDiscriminator(discriminator);
    assertSame(discriminator, appender.getDiscriminator());
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.DEBUG, "kept"));

    CyclicBufferTracker<ILoggingEvent> tracker = appender.getCyclicBufferTracker();
    assertEquals(1, tracker.getComponentCount());
    assertEquals(1, tracker.find("session-42").length());
  }

  @Test
  public void finalizeSessionMarkerEndsTheLifeOfTheEventsBuffer() throws Exception {
    CyclicBufferTracker<ILoggingEvent> tracker = spy(new CyclicBufferTracker<ILoggingEvent>());
    appender.setCyclicBufferTracker(tracker);
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.DEBUG, "ordinary"));
    verify(tracker, never()).endOfLife(anyString());

    LoggingEvent last = event(Level.DEBUG, "last");
    last.setMarkers(Collections.<Marker>singletonList(ClassicConstants.FINALIZE_SESSION_MARKER));
    appender.append(last);
    verify(tracker).endOfLife(DefaultDiscriminator.DEFAULT);
  }

  @Test
  public void appendRemovesBuffersThatWentStale() throws Exception {
    CyclicBufferTracker<ILoggingEvent> tracker = new CyclicBufferTracker<ILoggingEvent>();
    // last used at the epoch: long past the tracker's timeout
    tracker.getOrCreate("stale", 0);
    appender.setCyclicBufferTracker(tracker);
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.DEBUG, "fresh"));

    assertNull(tracker.find("stale"));
    assertEquals(1, tracker.getComponentCount());
    assertEquals(1, tracker.find(DefaultDiscriminator.DEFAULT).length());
    checker.assertContainsMatch(Status.INFO, "SMTPAppender \\[smtp\\] is tracking \\[1\\] buffers");
  }

  // --------------------------------------------- append() tracker status messages

  @Test
  public void trackerStatusIsReportedAndItsDelayQuadrupled() throws Exception {
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);
    long initialDelay = base().delayBetweenStatusMessages;

    appender.append(event(Level.DEBUG, "first"));

    checker.assertContainsMatch(Status.INFO, "SMTPAppender \\[smtp\\] is tracking \\[1\\] buffers");
    assertEquals(4 * initialDelay, base().delayBetweenStatusMessages);
    assertTrue(base().lastTrackerStatusPrint > 0);
  }

  @Test
  public void trackerStatusIsNotRepeatedBeforeTheDelayElapses() throws Exception {
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);
    long delay = base().delayBetweenStatusMessages;
    // the next status message is due at the end of time
    base().lastTrackerStatusPrint = Long.MAX_VALUE - delay;

    appender.append(event(Level.DEBUG, "quiet"));

    assertEquals(0, checker.matchCount("SMTPAppender \\[smtp\\] is tracking"));
    assertEquals(delay, base().delayBetweenStatusMessages);
    assertEquals(Long.MAX_VALUE - delay, base().lastTrackerStatusPrint);
  }

  @Test
  public void trackerStatusDelayStopsGrowingAtTheMaximum() throws Exception {
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);
    base().delayBetweenStatusMessages = SMTPAppenderBase.MAX_DELAY_BETWEEN_STATUS_MESSAGES;

    appender.append(event(Level.DEBUG, "first"));

    assertEquals(1, checker.matchCount("SMTPAppender \\[smtp\\] is tracking"));
    assertEquals(SMTPAppenderBase.MAX_DELAY_BETWEEN_STATUS_MESSAGES, base().delayBetweenStatusMessages);
  }

  // ------------------------------------------------------------ sendBuffer()

  @Test
  public void sentMessageWrapsTheEventsInTheLayoutHeadersAndFooters() throws Exception {
    PatternLayout layout = new PatternLayout();
    layout.setContext(context);
    layout.setPattern("[%msg]");
    layout.setFileHeader("FH|");
    layout.setPresentationHeader("PH|");
    layout.setPresentationFooter("|PF");
    layout.setFileFooter("|FF");
    layout.start();
    configure(appender);
    appender.setLayout(layout);
    appender.setFrom("sender@example.com");
    appender.setSubject("report %msg");
    assertEquals("report %msg", appender.getSubject());
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.INFO, "one"));
    appender.append(event(Level.ERROR, "two"));

    MimeMessage message = onlySentMessage();
    assertEquals("FH|PH|[one][two]|PF|FF", body(message));
    assertEquals("report two", message.getSubject());
    assertEquals(Collections.singletonList("sender@example.com"), addresses(message.getFrom()));
    assertNotNull(message.getSentDate());
    checker.assertContainsMatch(Status.INFO,
        "About to send out SMTP message \"report two\" to \\[to@example.com\\]");
  }

  @Test
  public void textualContentIsSentWithTheLayoutSubtypeAndTheCharsetEncoding() throws Exception {
    PatternLayout htmlLayout = new PatternLayout() {
      @Override
      public String getContentType() {
        return "text/html";
      }
    };
    htmlLayout.setContext(context);
    htmlLayout.setPattern("<p>%msg</p>");
    htmlLayout.start();
    configure(appender);
    appender.setLayout(htmlLayout);
    appender.setCharsetEncoding("ISO-8859-1");
    assertEquals("ISO-8859-1", appender.getCharsetEncoding());
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "caf\u00e9"));

    MimeBodyPart part = onlyBodyPart(onlySentMessage());
    assertEquals("text/html; charset=ISO-8859-1", contentType(part));
    assertEquals("<p>caf\u00e9</p>", part.getContent());
  }

  @Test
  public void plainTextLayoutContentDefaultsToUtf8() throws Exception {
    configure(appender);
    assertEquals("UTF-8", appender.getCharsetEncoding());
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "plain"));

    assertEquals("text/plain; charset=UTF-8", contentType(onlyBodyPart(onlySentMessage())));
  }

  @Test
  public void nonTextualContentIsSentWithTheLayoutContentType() throws Exception {
    LayoutBase<ILoggingEvent> jsonLayout = new LayoutBase<ILoggingEvent>() {
      @Override
      public String doLayout(ILoggingEvent event) {
        return "{\"msg\":\"" + event.getMessage() + "\"}";
      }

      @Override
      public String getContentType() {
        return "application/json";
      }
    };
    jsonLayout.setContext(context);
    jsonLayout.start();
    configure(appender);
    appender.setLayout(jsonLayout);
    assertSame(jsonLayout, appender.getLayout());
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "x"));

    MimeBodyPart part = onlyBodyPart(onlySentMessage());
    assertEquals("application/json", contentType(part));
    assertEquals("{\"msg\":\"x\"}", part.getContent());
  }

  @Test
  public void subjectIsEncodedWithTheCharsetEncoding() throws Exception {
    configure(appender);
    appender.setCharsetEncoding("ISO-8859-1");
    appender.setSubject("%msg");
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "caf\u00e9"));

    MimeMessage message = onlySentMessage();
    assertEquals("caf\u00e9", message.getSubject());
    assertEquals("=?ISO-8859-1?Q?caf=E9?=", message.getHeader("Subject", null));
  }

  @Test
  public void subjectIsTruncatedAtTheFirstNewLine() throws Exception {
    configure(appender);
    appender.setSubject("%msg");
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "first line\nsecond line"));

    assertEquals("first line", onlySentMessage().getSubject());
  }

  @Test
  public void subjectStartingWithANewLineIsSentEmpty() throws Exception {
    configure(appender);
    appender.setSubject("%msg");
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "\nsecond line"));

    assertEquals("", onlySentMessage().getSubject());
  }

  @Test
  public void subjectIsUndefinedWithoutASubjectLayout() throws Exception {
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);
    appender.subjectLayout = null;

    appender.append(event(Level.ERROR, "x"));

    assertEquals("Undefined subject", onlySentMessage().getSubject());
  }

  @Test
  public void nullSubjectFromTheSubjectLayoutIsSentWithoutSubject() throws Exception {
    @SuppressWarnings("unchecked")
    Layout<ILoggingEvent> nullSubject = mock(Layout.class);
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);
    appender.subjectLayout = nullSubject;

    appender.append(event(Level.ERROR, "x"));

    assertNull(onlySentMessage().getSubject());
    checker.assertIsErrorFree();
  }

  @Test
  public void withoutFromTheSessionDefaultSenderIsUsed() throws Exception {
    configure(appender);
    assertNull(appender.getFrom());
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "x"));

    assertEquals(Collections.singletonList(DEFAULT_SENDER), addresses(onlySentMessage().getFrom()));
  }

  @Test
  public void unparsableFromIsReportedAndTheMessageIsSentWithoutSender() throws Exception {
    configure(appender);
    appender.setFrom(NO_PARSE_ADDRESS);
    assertEquals(NO_PARSE_ADDRESS, appender.getFrom());
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "x"));

    checker.assertContainsMatch(Status.ERROR, "Could not parse address \\[" + NO_PARSE_ADDRESS + "\\].");
    assertNull(onlySentMessage().getFrom());
  }

  @Test
  public void getAddressParsesAValidAddress() {
    appender.setContext(context);
    InternetAddress address = base().getAddress("Alice <alice@example.com>");
    assertEquals("alice@example.com", address.getAddress());
    assertEquals("Alice", address.getPersonal());
    checker.assertIsErrorFree();
  }

  @Test
  public void getAddressReturnsNullForAnUnparsableAddress() {
    appender.setContext(context);
    assertNull(base().getAddress(NO_PARSE_ADDRESS));
    checker.asssertContainsException(AddressException.class);
  }

  @Test
  public void everyParsedRecipientReceivesTheMessage() throws Exception {
    configure(appender);
    appender.addTo("a@example.com");
    appender.addTo("B <b@example.com>, c@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "x"));

    assertEquals(Arrays.asList("a@example.com", "b@example.com", "c@example.com"),
        addresses(onlySentMessage().getRecipients(Message.RecipientType.TO)));
  }

  @Test
  public void recipientsThatResolveToNullOrEmptyAreSkipped() throws Exception {
    SMTPAppender nullRecipientAppender = new SMTPAppender() {
      @Override
      protected PatternLayout makeNewToPatternLayout(String toPattern) {
        if (!"null-recipient".equals(toPattern)) {
          return super.makeNewToPatternLayout(toPattern);
        }
        PatternLayout nullLayout = new PatternLayout() {
          @Override
          public String doLayout(ILoggingEvent event) {
            return null;
          }
        };
        nullLayout.setPattern(toPattern);
        return nullLayout;
      }
    };
    configure(nullRecipientAppender);
    nullRecipientAppender.addTo("null-recipient");
    nullRecipientAppender.addTo("%X{absentKey}");
    nullRecipientAppender.addTo("to@example.com");
    startForSending(nullRecipientAppender);

    nullRecipientAppender.append(event(Level.ERROR, "x"));

    assertEquals(Collections.singletonList("to@example.com"),
        addresses(onlySentMessage().getRecipients(Message.RecipientType.TO)));
  }

  @Test
  public void noResolvedRecipientAbortsTheTransmission() throws Exception {
    configure(appender);
    appender.addTo("%X{absentKey}");
    startForSending(appender);

    appender.append(event(Level.ERROR, "x"));

    assertTrue(sentMessages.isEmpty());
    checker.assertContainsMatch(Status.INFO, "Empty destination address. Aborting email transmission");
    assertEquals(0, checker.matchCount("About to send out SMTP message"));
  }

  @Test
  public void unparsableRecipientStopsAddressParsing() throws Exception {
    configure(appender);
    appender.addTo(NO_PARSE_ADDRESS);
    appender.addTo("to@example.com");
    startForSending(appender);

    appender.append(event(Level.ERROR, "x"));

    checker.assertContainsMatch(Status.ERROR,
        "Could not parse email address for \\[.*" + NO_PARSE_ADDRESS + ".*\\] for event \\[.*x\\]");
    // recipients after the unparsable one are not considered
    assertTrue(sentMessages.isEmpty());
    checker.assertContainsMatch(Status.INFO, "Empty destination address. Aborting email transmission");
  }

  @Test
  public void updateMimeMsgCanAlterTheMessageBeforeItIsSent() throws Exception {
    final List<String> hookEvents = new ArrayList<String>();
    SMTPAppender headerAppender = new SMTPAppender() {
      @Override
      protected void updateMimeMsg(MimeMessage mimeMsg, CyclicBuffer<ILoggingEvent> cb, ILoggingEvent lastEventObject) {
        assertNotNull(cb);
        hookEvents.add(lastEventObject.getMessage());
        try {
          mimeMsg.setHeader("X-Logback-Test", "added");
        } catch (MessagingException e) {
          throw new IllegalStateException(e);
        }
      }
    };
    configure(headerAppender);
    headerAppender.addTo("to@example.com");
    startForSending(headerAppender);

    headerAppender.append(event(Level.INFO, "one"));
    headerAppender.append(event(Level.ERROR, "two"));

    assertEquals(Collections.singletonList("two"), hookEvents);
    assertArrayEquals(new String[] {"added"}, onlySentMessage().getHeader("X-Logback-Test"));
  }

  @Test
  public void transportFailureIsReportedAsAnError() throws Exception {
    configure(appender);
    appender.addTo("to@example.com");
    startForSending(appender);
    transport.when(() -> Transport.send(any(Message.class))).thenThrow(new MessagingException("server unreachable"));

    appender.append(event(Level.ERROR, "x"));

    assertTrue(sentMessages.isEmpty());
    checker.assertContainsMatch(Status.ERROR, "Error occurred while sending e-mail notification.");
    checker.asssertContainsException(MessagingException.class);
  }

  // ------------------------------------------------------------ recipients

  @Test
  public void addToTrimsThePatternAndExposesIt() {
    appender.setContext(context);
    appender.addTo("  to@example.com  ");

    assertEquals(Collections.singletonList("to@example.com%nopex"), appender.getToAsListOfString());
    assertEquals(1, appender.getToList().size());
    assertEquals("to@example.com%nopex", appender.getToList().get(0).getPattern());
    assertTrue(appender.getToList().get(0).isStarted());
  }

  @Test
  public void addToRejectsANullRecipient() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> appender.addTo(null));
    assertEquals("Null or empty <to> property", e.getMessage());
    assertTrue(appender.getToList().isEmpty());
  }

  @Test
  public void addToRejectsAnEmptyRecipient() {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> appender.addTo(""));
    assertEquals("Null or empty <to> property", e.getMessage());
    assertTrue(appender.getToList().isEmpty());
  }
}
