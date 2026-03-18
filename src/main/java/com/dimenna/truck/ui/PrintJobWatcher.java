package com.dimenna.truck.ui;

import javax.print.DocPrintJob;
import javax.print.event.PrintJobAdapter;
import javax.print.event.PrintJobEvent;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class PrintJobWatcher {
  private final CountDownLatch latch = new CountDownLatch(1);
  private volatile String err;

  PrintJobWatcher(DocPrintJob job) {
    job.addPrintJobListener(new PrintJobAdapter() {
      @Override public void printJobCompleted(PrintJobEvent e) { latch.countDown(); }
      @Override public void printJobCanceled (PrintJobEvent e) { err = "cancelado";  latch.countDown(); }
      @Override public void printJobFailed   (PrintJobEvent e) { err = "falló en el spooler"; latch.countDown(); }
      @Override public void printJobNoMoreEvents(PrintJobEvent e) { latch.countDown(); }
    });
  }

  void awaitOrThrow(long timeoutMs) throws Exception {
    if (!latch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
      throw new RuntimeException("timeout: la cola no terminó dentro de " + timeoutMs + " ms");
    }
    if (err != null) throw new RuntimeException("Trabajo " + err);
  }
}
