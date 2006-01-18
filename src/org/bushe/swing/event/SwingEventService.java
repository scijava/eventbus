/**
 * Copyright 2005 Bushe Enterprises, Inc., Hopkinton, MA, USA, www.bushe.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bushe.swing.event;

import java.util.List;
import java.util.logging.Level;
import javax.swing.SwingUtilities;


/**
 * An {@link EventService} implementation for Swing.
 * <p/>
 * This class is Swing thread-safe.  All publish() calls NOT on the Swing EventDispatchThread thread are
 * queued onto the EDT.  If the calling thread is the EDT, then this is a simple pass-through
 * (i.e the subscribers are notified on the same stack frame, just like they would be had they added
 * themselves via Swing addXXListener methods).
 *
 * @author Michael Bushe michael@bushe.com
 */
public class SwingEventService extends ThreadSafeEventService {

   /**
    * By default, the SwingEventService is contructed such that any listener that
    * takes over 200 ms causes an SubscriberTimingEvent to be published.  You will
    * need to add a subscriber to this event.  Note that if you use event to launch
    * a modal dialog, the timings will be as long as the dialog is up - this is the way
    * Swing works.
    */
   public SwingEventService() {
      super(new Long(200), false);
   }

   public SwingEventService(Long timeThresholdForEventTimingEventPublication) {
      super(timeThresholdForEventTimingEventPublication, false);
   }

   /**
    * Create a SwingEventService is such that any listener that takes over timeThresholdForEventTimingEventPublication
    * milliseconds causes an EventSubscriberTimingEvent to be published.  You can add a subscriber to this event or set
    * subscribeTimingEventsInternally to true to cause the default logging to occur through the protected
    * {@link #subscribeTiming(SubscriberTimingEvent)} call.
    * <p>
    * Note that if you use event to launch a modal dialog, the timings will be as long as the dialog is up - this is the way
    * Swing works.
    * @param timeThresholdForEventTimingEventPublication the longest time a subscriber should spend handling an event,
    * The service will pulish an SubscriberTimingEvent after listener processing if the time was exceeded.  If null,
    * no SubscriberTimingEvent will be issued.
    * @param subscribeTimingEventsInternally add a subscriber to the EventSubscriberTimingEvent internally and call the
    * protected {@link #subscribeTiming(SubscriberTimingEvent)} method when they occur.  This logs a warning to a java.util.logging logger by default.
    * @throws IllegalArgumentException if timeThresholdForEventTimingEventPublication is null and subscribeTimingEventsInternally
    * is true.
    */
   public SwingEventService(Long timeThresholdForEventTimingEventPublication, boolean subscribeTimingEventsInternally) {
      super(timeThresholdForEventTimingEventPublication, subscribeTimingEventsInternally);
   }

   /**
    * Same as ThreadSafeEventService.publish(), except if the call is coming from a thread that is not the
    * Swing Event Dispatch Thread, the request is put on the EDT through a a call to SwingUtilities.invokeLater().
    * Otherwise this DOES NOT post a new event on the EDT.  The subscribers are called on the same EDT event,
    * just like addXXXListeners would be.
    */
   protected void publish(final EventServiceEvent event, final String topic, final Object evtObj,
           final List subscribers, final List vetoSubscribers, final StackTraceElement[] callingStack) {
      if (SwingUtilities.isEventDispatchThread()) {
         super.publish(event, topic, evtObj, subscribers, vetoSubscribers, callingStack);
      } else {
         //Make call to this method - stick on the EDT if not on the EDT
         //Check the params first so that this thread can get the exception thrown
         SwingUtilities.invokeLater(new Runnable() {
            public void run() {
               if (LOG.isLoggable(Level.FINE)) {
                  LOG.fine("publish(" + event + "," + topic + "," + evtObj
                          + "), called from non-EDT Thread:" + callingStack);
               }
               SwingEventService.super.publish(event, topic, evtObj, subscribers, vetoSubscribers, callingStack);
            }
         });
      }
   }
}
