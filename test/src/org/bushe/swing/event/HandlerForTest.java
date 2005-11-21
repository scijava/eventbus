package org.bushe.swing.event;

/**
 * @author Michael Bushe
 * @since Nov 19, 2005 11:01:06 PM
 */
class HandlerForTest implements EventHandler {
   private boolean throwException;
   private Long waitTime;
   private EBTestCounter testDefaultEventService;

   public HandlerForTest(EBTestCounter testDefaultEventService, Long waitTime) {
      this.testDefaultEventService = testDefaultEventService;
      this.waitTime = waitTime;
   }

   public HandlerForTest(EBTestCounter testDefaultEventService, boolean throwException) {
      this.testDefaultEventService = testDefaultEventService;
      this.throwException = throwException;
   }

   public void handleEvent(EventServiceEvent evt) {
      if (waitTime != null) {
         try {
            Thread.sleep(waitTime.longValue());
         } catch (InterruptedException e) {
         }
      }
      testDefaultEventService.eventsHandledCount++;
      if (throwException) {
         testDefaultEventService.handleExceptionCount++;
         throw new IllegalArgumentException();
      }
   }
}
