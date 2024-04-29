/*
 * Copyright (c) 2016-present, RxJava Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.rxjava3.schedulers;

import static org.junit.Assert.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.junit.Test;
import org.reactivestreams.*;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Scheduler.Worker;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.subscribers.*;

/**
 * Base tests for schedulers that involve threads (concurrency).
 *
 * These can only run on Schedulers that launch threads since they expect async/concurrent behavior.
 *
 * The Current/Immediate schedulers will not work with these tests.
 */
public abstract class AbstractSchedulerConcurrencyTests extends AbstractSchedulerTests {

    /**
     * Make sure canceling through {@code subscribeOn} works.
     * Bug report: https:
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public final void unSubscribeForScheduler() throws InterruptedException {
        final AtomicInteger countReceived = new AtomicInteger();
        final AtomicInteger countGenerated = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        Flowable.interval(50, TimeUnit.MILLISECONDS)
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(Long aLong) {
                        countGenerated.incrementAndGet();
                        return aLong;
                    }
                })
                .subscribeOn(getScheduler())
                .observeOn(getScheduler())
                .subscribe(new DefaultSubscriber<Long>() {
                    @Override
                    public void onComplete() {
                        System.out.println("--- completed");
                    }

                    @Override
                    public void onError(Throwable e) {
                        System.out.println("--- onError");
                    }

                    @Override
                    public void onNext(Long args) {
                        if (countReceived.incrementAndGet() == 2) {
                            cancel();
                            latch.countDown();
                        }
                        System.out.println("==> Received " + args);
                    }
                });

        latch.await(1000, TimeUnit.MILLISECONDS);

        System.out.println("----------- it thinks it is finished ------------------ ");

        int timeout = 10;

        while (timeout-- > 0 && countGenerated.get() != 2) {
            Thread.sleep(100);
        }

        assertEquals(2, countGenerated.get());
    }

    @Test
    public void unsubscribeRecursiveScheduleFromOutside() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        final Worker inner = getScheduler().createWorker();
        try {
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    inner.schedule(new Runnable() {

                        int i;

                        @Override
                        public void run() {
                            System.out.println("Run: " + i++);
                            if (i == 10) {
                                latch.countDown();
                                try {
                                    unsubscribeLatch.await();
                                } catch (InterruptedException e) {
                                }
                            }

                            counter.incrementAndGet();
                            inner.schedule(this);
                        }
                    });
                }

            });

            latch.await();
            inner.dispose();
            unsubscribeLatch.countDown();
            Thread.sleep(200); 
            assertEquals(10, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void unsubscribeRecursiveScheduleFromInside() throws InterruptedException {
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        final Worker inner = getScheduler().createWorker();
        try {
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    inner.schedule(new Runnable() {

                        int i;

                        @Override
                        public void run() {
                            System.out.println("Run: " + i++);
                            if (i == 10) {
                                inner.dispose();
                            }

                            counter.incrementAndGet();
                            inner.schedule(this);
                        }
                    });
                }

            });

            unsubscribeLatch.countDown();
            Thread.sleep(200); 
            assertEquals(10, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void unsubscribeRecursiveScheduleWithDelay() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch unsubscribeLatch = new CountDownLatch(1);
        final AtomicInteger counter = new AtomicInteger();
        final Worker inner = getScheduler().createWorker();

        try {
            inner.schedule(new Runnable() {

                @Override
                public void run() {
                    inner.schedule(new Runnable() {

                        long i = 1L;

                        @Override
                        public void run() {
                            if (i++ == 10) {
                                latch.countDown();
                                try {
                                    unsubscribeLatch.await();
                                } catch (InterruptedException e) {
                                }
                            }

                            counter.incrementAndGet();
                            inner.schedule(this, 10, TimeUnit.MILLISECONDS);
                        }
                    }, 10, TimeUnit.MILLISECONDS);
                }
            });

            latch.await();
            inner.dispose();
            unsubscribeLatch.countDown();
            Thread.sleep(200); 
            assertEquals(10, counter.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void recursionFromOuterActionAndUnsubscribeInside() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Worker inner = getScheduler().createWorker();
        try {
            inner.schedule(new Runnable() {

                int i;

                @Override
                public void run() {
                    i++;
                    if (i % 100000 == 0) {
                        System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                    }
                    if (i < 1000000L) {
                        inner.schedule(this);
                    } else {
                        latch.countDown();
                    }
                }
            });

            latch.await();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void recursion() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final Worker inner = getScheduler().createWorker();
        try {
            inner.schedule(new Runnable() {

                private long i;

                @Override
                public void run() {
                    i++;
                    if (i % 100000 == 0) {
                        System.out.println(i + "  Total Memory: " + Runtime.getRuntime().totalMemory() + "  Free: " + Runtime.getRuntime().freeMemory());
                    }
                    if (i < 1000000L) {
                        inner.schedule(this);
                    } else {
                        latch.countDown();
                    }
                }
            });

            latch.await();
        } finally {
            inner.dispose();
        }
    }

    @Test
    public void recursionAndOuterUnsubscribe() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(10);
        final CountDownLatch completionLatch = new CountDownLatch(1);
        final Worker inner = getScheduler().createWorker();
        try {
            Flowable<Integer> obs = Flowable.unsafeCreate(new Publisher<Integer>() {
                @Override
                public void subscribe(final Subscriber<? super Integer> subscriber) {
                    inner.schedule(new Runnable() {
                        @Override
                        public void run() {
                            subscriber.onNext(42);
                            latch.countDown();

                            inner.schedule(this);
                        }
                    });

                    subscriber.onSubscribe(new Subscription() {

                        @Override
                        public void cancel() {
                            inner.dispose();
                            subscriber.onComplete();
                            completionLatch.countDown();
                        }

                        @Override
                        public void request(long n) {

                        }
                    });

                }
            });

            final AtomicInteger count = new AtomicInteger();
            final AtomicBoolean completed = new AtomicBoolean(false);
            ResourceSubscriber<Integer> s = new ResourceSubscriber<Integer>() {
                @Override
                public void onComplete() {
                    System.out.println("Completed");
                    completed.set(true);
                }

                @Override
                public void onError(Throwable e) {
                    System.out.println("Error");
                }

                @Override
                public void onNext(Integer args) {
                    count.incrementAndGet();
                    System.out.println(args);
                }
            };
            obs.subscribe(s);

            if (!latch.await(5000, TimeUnit.MILLISECONDS)) {
                fail("Timed out waiting on onNext latch");
            }

            s.dispose();
            System.out.println("unsubscribe");

            if (!completionLatch.await(5000, TimeUnit.MILLISECONDS)) {
                fail("Timed out waiting on completion latch");
            }

            assertTrue(count.get() >= 10);
            assertTrue(completed.get());
        } finally {
            inner.dispose();
        }
    }

    @Test
    public final void subscribeWithScheduler() throws InterruptedException {
        final Scheduler scheduler = getScheduler();

        final AtomicInteger count = new AtomicInteger();

        Flowable<Integer> f1 = Flowable.<Integer> just(1, 2, 3, 4, 5);

        f1.subscribe(new Consumer<Integer>() {

            @Override
            public void accept(Integer t) {
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("t: " + t);
                count.incrementAndGet();
            }
        });

        assertEquals(5, count.get());

        count.set(0);


        final String currentThreadName = Thread.currentThread().getName();

        final CountDownLatch latch = new CountDownLatch(5);
        final CountDownLatch first = new CountDownLatch(1);

        f1.subscribeOn(scheduler).subscribe(new Consumer<Integer>() {

            @Override
            public void accept(Integer t) {
                try {
                    first.await(1000, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException("The latch should have released if we are async.", e);
                }

                assertNotEquals(Thread.currentThread().getName(), currentThreadName);
                System.out.println("Thread: " + Thread.currentThread().getName());
                System.out.println("t: " + t);
                count.incrementAndGet();
                latch.countDown();
            }
        });

        assertEquals(0, count.get());
        first.countDown();

        latch.await();
        assertEquals(5, count.get());
    }

}
