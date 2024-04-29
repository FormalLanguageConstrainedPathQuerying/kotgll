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

package io.reactivex.rxjava3.internal.operators.observable;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.RxJavaTest;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

public class ObservableDoOnUnsubscribeTest extends RxJavaTest {

    @Test
    public void doOnUnsubscribe() throws Exception {
        int subCount = 3;
        final CountDownLatch upperLatch = new CountDownLatch(subCount);
        final CountDownLatch lowerLatch = new CountDownLatch(subCount);
        final CountDownLatch onNextLatch = new CountDownLatch(subCount);

        final AtomicInteger upperCount = new AtomicInteger();
        final AtomicInteger lowerCount = new AtomicInteger();
        Observable<Long> longs = Observable
                .interval(50, TimeUnit.MILLISECONDS)
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                            upperLatch.countDown();
                            upperCount.incrementAndGet();
                    }
                })
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                            onNextLatch.countDown();
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                            lowerLatch.countDown();
                            lowerCount.incrementAndGet();
                    }
                });

        List<Disposable> subscriptions = new ArrayList<>();
        List<TestObserver<Long>> subscribers = new ArrayList<>();

        for (int i = 0; i < subCount; ++i) {
            TestObserver<Long> observer = new TestObserver<>();
            subscriptions.add(observer);
            longs.subscribe(observer);
            subscribers.add(observer);
        }

        onNextLatch.await();
        for (int i = 0; i < subCount; ++i) {
            subscriptions.get(i).dispose();
        }

        upperLatch.await();
        lowerLatch.await();
        assertEquals(String.format("There should exactly %d un-subscription events for upper stream", subCount), subCount, upperCount.get());
        assertEquals(String.format("There should exactly %d un-subscription events for lower stream", subCount), subCount, lowerCount.get());
    }

    @Test
    public void doOnUnSubscribeWorksWithRefCount() throws Exception {
        int subCount = 3;
        final CountDownLatch upperLatch = new CountDownLatch(1);
        final CountDownLatch lowerLatch = new CountDownLatch(1);
        final CountDownLatch onNextLatch = new CountDownLatch(subCount);

        final AtomicInteger upperCount = new AtomicInteger();
        final AtomicInteger lowerCount = new AtomicInteger();
        Observable<Long> longs = Observable
                .interval(50, TimeUnit.MILLISECONDS)
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                            upperLatch.countDown();
                            upperCount.incrementAndGet();
                    }
                })
                .doOnNext(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                            onNextLatch.countDown();
                    }
                })
                .doOnDispose(new Action() {
                    @Override
                    public void run() {
                            lowerLatch.countDown();
                            lowerCount.incrementAndGet();
                    }
                })
                .publish()
                .refCount();

        List<Disposable> subscriptions = new ArrayList<>();
        List<TestObserver<Long>> subscribers = new ArrayList<>();

        for (int i = 0; i < subCount; ++i) {
            TestObserver<Long> observer = new TestObserver<>();
            longs.subscribe(observer);
            subscriptions.add(observer);
            subscribers.add(observer);
        }

        onNextLatch.await();
        for (int i = 0; i < subCount; ++i) {
            subscriptions.get(i).dispose();
        }

        upperLatch.await();
        lowerLatch.await();
        assertEquals("There should exactly 1 un-subscription events for upper stream", 1, upperCount.get());
        assertEquals("There should exactly 1 un-subscription events for lower stream", 1, lowerCount.get());
    }

    @Test
    public void noReentrantDispose() {

        final AtomicInteger disposeCalled = new AtomicInteger();

        final BehaviorSubject<Integer> s = BehaviorSubject.create();
        s.doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                disposeCalled.incrementAndGet();
                s.onNext(2);
            }
        })
        .firstOrError()
        .subscribe()
        .dispose();

        assertEquals(1, disposeCalled.get());
    }
}
