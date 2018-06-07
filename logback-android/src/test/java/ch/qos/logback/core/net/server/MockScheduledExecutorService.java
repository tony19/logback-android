package ch.qos.logback.core.net.server;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MockScheduledExecutorService extends AbstractExecutorService implements ScheduledExecutorService {

  private Runnable lastCommand;

  public Runnable getLastCommand() {
    return lastCommand;
  }

  public void shutdown() {
  }

  public List<Runnable> shutdownNow() {
    return Collections.emptyList();
  }

  public boolean isShutdown() {
    return true;
  }

  public boolean isTerminated() {
    return true;
  }

  public boolean awaitTermination(long timeout, TimeUnit unit)
      throws InterruptedException {
    return true;
  }

  public void execute(Runnable command) {
    command.run();
    lastCommand = command;
  }

  @Override
  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
    throw new UnsupportedOperationException();
  }
}
