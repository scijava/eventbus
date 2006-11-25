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
import java.util.regex.Pattern;
import java.io.Serializable;

import junit.framework.TestCase;

/** The DefaultEventService is NOT Swing-safe!  But it's easier to test... */
public class TestEventBus extends TestCase {

   private EventSubscriber eventSubscriber = null;
   private EventTopicSubscriber eventTopicSubscriber;
   private EBTestCounter testCounter = new EBTestCounter();

   public TestEventBus(String name) {
      super(name);
   }

   protected void setUp() throws Exception {
      EventBus.getGlobalEventService().clearAllSubscribers();
   }

   protected void tearDown() throws Exception {
   }

   private EventServiceEvent createEvent() {
      return new EventServiceEvent() {
         public Object getSource() {
            return "";
         }
      };
   }

   private Class getEventClass() {
      return createEvent().getClass();
   }

   private EventSubscriber createEventSubscriber(boolean throwException) {
      return new SubscriberForTest(testCounter, throwException);
   }

   private EventTopicSubscriber createEventTopicSubscriber(boolean throwException) {
      return new TopicSubscriberForTest(testCounter, throwException);
   }

   private EventSubscriber getEventSubscriber() {
      return getEventSubscriber(true);
   }

   private EventSubscriber getEventSubscriber(boolean throwException) {
      if (eventSubscriber == null) {
         eventSubscriber = createEventSubscriber(throwException);
      }
      return eventSubscriber;
   }

   public void testSubscribe() {
      boolean actualReturn;
      EventSubscriber subscriber = createEventSubscriber(false);

      actualReturn = EventBus.subscribe(getEventClass(), subscriber);
      assertTrue("testSubscribe(new subscriber)", actualReturn);

      actualReturn = EventBus.subscribe(getEventClass(), subscriber);
      assertFalse("testSubscribe(duplicate subscriber)", actualReturn);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testPublish(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);

      try {
         actualReturn = EventBus.subscribe((Class) null, getEventSubscriber());
         fail("subscribeStrongly(null, x) should have thrown exception");
      } catch (Exception e) {
      }

      try {
         actualReturn = EventBus.subscribe(getEventClass(), null);
         fail("subscribeStrongly(x, null) should have thrown exception");
      } catch (Exception e) {
      }

   }

   /**
    * Since we are using the event bus from a non-awt thread, stay alive for a sec
    * to give time for the EDT to start and post the message
    */
   private void waitForEDT() {
      try {
         Thread.sleep(1000);
      } catch (Throwable e){
      }
   }

   public void testSubscribeWeakly() {
      boolean actualReturn;
      EventSubscriber subscriber = createEventSubscriber(false);

      actualReturn = EventBus.subscribe(getEventClass(), subscriber);
      assertTrue("testSubscribeWeakly(new subscriber)", actualReturn);

      actualReturn = EventBus.subscribe(getEventClass(), subscriber);
      assertFalse("testSubscribe(duplicate subscriber)", actualReturn);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testPublish(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);
      subscriber = null;
      System.gc();
      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testPublish(total)", 0, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);

      try {
         actualReturn = EventBus.subscribeStrongly((Class) null, getEventSubscriber());
         fail("subscribeStrongly(null, x) should have thrown exception");
      } catch (Exception e) {
      }

      try {
         actualReturn = EventBus.subscribeStrongly(getEventClass(), null);
         fail("subscribeStrongly(x, null) should have thrown exception");
      } catch (Exception e) {
      }
   }

   public void testIllegalArgs() {
      try {
         EventBus.subscribeVetoListenerStrongly((Class) null, new VetoEventListenerForTest());
         fail();
      } catch (Throwable t) {
      }
      try {
         EventBus.subscribeVetoListenerStrongly((String) null, new VetoTopicEventListenerForTest());
         fail();
      } catch (Throwable t) {
      }
      try {
         EventBus.subscribeVetoListenerStrongly("foo", null);
         fail();
      } catch (Throwable t) {
      }
      try {
         EventBus.subscribeVetoListenerStrongly(getEventClass(), null);
         fail();
      } catch (Throwable t) {
      }


      try {
         EventBus.unsubscribeVetoListener((Class) null, new VetoEventListenerForTest());
         fail();
      } catch (Throwable t) {
      }
      try {
         EventBus.unsubscribeVetoListener((String) null, new VetoTopicEventListenerForTest());
         fail();
      } catch (Throwable t) {
      }
      try {
         EventBus.unsubscribeVetoListener("foo", null);
         fail();
      } catch (Throwable t) {
      }
      try {
         EventBus.unsubscribeVetoListener(getEventClass(), null);
         fail();
      } catch (Throwable t) {
      }

   }

   public void testVeto() {
      boolean actualReturn;
      EventSubscriber subscriber = createEventSubscriber(false);

      actualReturn = EventBus.subscribe(getEventClass(), subscriber);

      VetoEventListener vetoListener = new VetoEventListenerForTest();
      actualReturn = EventBus.subscribeVetoListenerStrongly(getEventClass(), vetoListener);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 0, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);
      EventBus.unsubscribeVetoListener(getEventClass(), vetoListener);
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);

   }

   public void testVetoException() {
      boolean actualReturn;
      EventSubscriber subscriber = createEventSubscriber(false);

      actualReturn = EventBus.subscribe(getEventClass(), subscriber);

      VetoEventListener vetoListener = new VetoEventListenerForTest(true);
      actualReturn = EventBus.subscribeVetoListenerStrongly(getEventClass(), vetoListener);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);
      EventBus.unsubscribeVetoListener(getEventClass(), vetoListener);
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 2, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);

   }

   public void testVetoTopic() {
      boolean actualReturn;
      EventTopicSubscriber subscriber = createEventTopicSubscriber(false);

      actualReturn = EventBus.subscribeStrongly("FooTopic", subscriber);

      VetoTopicEventListener vetoListener = new VetoTopicEventListener() {
         public boolean shouldVeto(String topic, Object data) {
            return true;
         }
      };
      actualReturn = EventBus.subscribeVetoListenerStrongly("FooTopic", vetoListener);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish("FooTopic", "Bar");
      waitForEDT();

      //The test passes if 0 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 0, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);
      EventBus.unsubscribeVetoListener("FooTopic", vetoListener);
      EventBus.publish("FooTopic", "Bar");
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);
   }


   public void testVetoWeak() {
      boolean actualReturn;
      EventSubscriber subscriber = createEventSubscriber(false);

      actualReturn = EventBus.subscribe(getEventClass(), subscriber);

      VetoEventListener vetoListener = new VetoEventListener() {
         public boolean shouldVeto(Object evt) {
            return true;
         }
      };
      actualReturn = EventBus.subscribeVetoListener(getEventClass(), vetoListener);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 0, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);
      vetoListener = null;
      System.gc();
      try {
         Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);
   }

   public void testVetoTopicWeak() {
      boolean actualReturn;
      EventTopicSubscriber subscriber = createEventTopicSubscriber(false);

      actualReturn = EventBus.subscribeStrongly("FooTopic", subscriber);

      VetoTopicEventListener vetoListener = new VetoTopicEventListener() {
         public boolean shouldVeto(String topic, Object data) {
            return true;
         }
      };
      actualReturn = EventBus.subscribeVetoListener("FooTopic", vetoListener);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish("FooTopic", "Bar");
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 0, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);
      vetoListener = null;
      System.gc();
      EventBus.publish("FooTopic", "Bar");
      waitForEDT();
      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testVeto(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testVeto(exceptions)", 0, testCounter.subscribeExceptionCount);
   }


   public void testUnsubscribe() {
      EventBus.subscribe(getEventClass(), getEventSubscriber(false));

      boolean actualReturn;

      try {
         actualReturn = EventBus.unsubscribe((Class) null, getEventSubscriber());
         fail("unsubscribe(null, x) should have thrown exception");
      } catch (Exception e) {
      }

      try {
         actualReturn = EventBus.unsubscribe(getEventClass(), null);
         fail("unsubscribe(x, null) should have thrown exception");
      } catch (Exception e) {
      }

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testPublish(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);

      actualReturn = EventBus.unsubscribe(getEventClass(), getEventSubscriber());
      assertTrue("return value", actualReturn);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testPublish(total)", 0, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);
   }

   public void testUnsubscribeTopic() {
      EventTopicSubscriber eventTopicSubscriber = createEventTopicSubscriber(false);
      EventBus.subscribeStrongly("FooTopic", eventTopicSubscriber);

      boolean actualReturn;

      try {
         actualReturn = EventBus.unsubscribe((String) null, eventTopicSubscriber);
         fail("unsubscribe(null, x) should have thrown exception");
      } catch (Exception e) {
      }

      try {
         actualReturn = EventBus.unsubscribe("FooTopic", null);
         fail("unsubscribe(x, null) should have thrown exception");
      } catch (Exception e) {
      }

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish("FooTopic", "Foo");
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testPublish(total)", 1, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);

      actualReturn = EventBus.unsubscribe("FooTopic", eventTopicSubscriber);
      assertTrue("return value", actualReturn);

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish("FooTopic", "Foo");
      waitForEDT();

      //The test passes if 1 subscribers completed and 0 subscribers threw exception.
      assertEquals("testPublish(total)", 0, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);
   }

   /**
    * Test that the publish method works and that execptions thrown in event subscribers don't halt publishing. In the
    * test 2 subscribers are good and 2 subscribers throw exceptions.
    */
   public void testPublish() {
      try {
         EventBus.publish(null);
         waitForEDT();
         fail("publish(null) should have thrown exception");
      } catch (Exception e) {
      }

      try {
         EventBus.publish((String) null, createEvent());
         waitForEDT();
         fail("publish(null, x) should have thrown exception");
      } catch (Exception e) {
      }

      EventBus.publish(createEvent());
      waitForEDT();
      assertEquals("testPublish(completed)", 0, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);

      EventBus.publish("Foo", "Bar");
      waitForEDT();
      assertEquals("testPublish(completed)", 0, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);

      EventBus.subscribe(getEventClass(), createEventSubscriber(true));
      EventBus.subscribe(getEventClass(), createEventSubscriber(false));
      EventBus.subscribe(getEventClass(), createEventSubscriber(true));
      EventBus.subscribe(getEventClass(), createEventSubscriber(false));

      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      EventBus.publish(createEvent());
      waitForEDT();

      //The test passes if 2 subscribers completed and 2 subscribers threw exception.
      assertEquals("testPublish(completed)", 4, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 2, testCounter.subscribeExceptionCount);

      EventSubscriber eventSubscriber = createEventSubscriber(false);
      EventBus.subscribe(ObjectEvent.class, eventSubscriber);
      testCounter.eventsHandledCount = 0;
      testCounter.subscribeExceptionCount = 0;
      ObjectEvent evt = new ObjectEvent("Foo", "Bar");
      assertEquals(evt.getEventObject(), "Bar");
      EventBus.publish(evt);
      waitForEDT();
      assertEquals("testPublish(completed)", 1, testCounter.eventsHandledCount);
      assertEquals("testPublish(exceptions)", 0, testCounter.subscribeExceptionCount);
   }
}
