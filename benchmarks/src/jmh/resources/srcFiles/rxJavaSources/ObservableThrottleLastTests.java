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

package io.reactivex.rxjava3.observable;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.exceptions.TestException;
import io.reactivex.rxjava3.functions.Action;
import org.junit.Test;
import org.mockito.InOrder;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.testsupport.TestHelper;

import static org.mockito.Mockito.*;

public class ObservableThrottleLastTests extends RxJavaTest {

    @Test
    public void throttleLastWithDropCallbackException() throws Throwable {
        Observer<Integer> observer = TestHelper.mockObserver();

        Action whenDisposed = mock(Action.class);

        TestScheduler s = new TestScheduler();
        PublishSubject<Integer> o = PublishSubject.create();
        o.doOnDispose(whenDisposed)
                .throttleLast(500, TimeUnit.MILLISECONDS, s, e -> {
                    if (e == 1) {
                        throw new TestException("Forced");
                    }
                })
                .subscribe(observer);

        s.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        o.onNext(1); 
        o.onNext(2); 
        s.advanceTimeTo(501, TimeUnit.MILLISECONDS);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onError(any(TestException.class));
        inOrder.verifyNoMoreInteractions();
        verify(whenDisposed).run();
    }

    @Test
    public void throttleLastWithDropCallback() {
        Observer<Integer> observer = TestHelper.mockObserver();

        Observer<Object> dropCallbackObserver = TestHelper.mockObserver();

        TestScheduler s = new TestScheduler();
        PublishSubject<Integer> o = PublishSubject.create();
        o.throttleLast(500, TimeUnit.MILLISECONDS, s, dropCallbackObserver::onNext).subscribe(observer);

        s.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        o.onNext(1); 
        o.onNext(2); 
        s.advanceTimeTo(501, TimeUnit.MILLISECONDS);
        o.onNext(3); 
        s.advanceTimeTo(600, TimeUnit.MILLISECONDS);
        o.onNext(4); 
        s.advanceTimeTo(700, TimeUnit.MILLISECONDS);
        o.onNext(5); 
        o.onNext(6); 
        s.advanceTimeTo(1001, TimeUnit.MILLISECONDS);
        o.onNext(7); 
        s.advanceTimeTo(1501, TimeUnit.MILLISECONDS);
        o.onComplete();

        InOrder inOrder = inOrder(observer);
        InOrder dropCallbackOrder = inOrder(dropCallbackObserver);
        dropCallbackOrder.verify(dropCallbackObserver).onNext(1);
        inOrder.verify(observer).onNext(2);
        dropCallbackOrder.verify(dropCallbackObserver).onNext(3);
        dropCallbackOrder.verify(dropCallbackObserver).onNext(4);
        dropCallbackOrder.verify(dropCallbackObserver).onNext(5);
        inOrder.verify(observer).onNext(6);
        inOrder.verify(observer).onNext(7);
        inOrder.verify(observer).onComplete();
        inOrder.verifyNoMoreInteractions();
        dropCallbackOrder.verifyNoMoreInteractions();
    }

    @Test
    public void throttle() {
        Observer<Integer> observer = TestHelper.mockObserver();

        TestScheduler s = new TestScheduler();
        PublishSubject<Integer> o = PublishSubject.create();
        o.throttleLast(500, TimeUnit.MILLISECONDS, s).subscribe(observer);

        s.advanceTimeTo(0, TimeUnit.MILLISECONDS);
        o.onNext(1); 
        o.onNext(2); 
        s.advanceTimeTo(501, TimeUnit.MILLISECONDS);
        o.onNext(3); 
        s.advanceTimeTo(600, TimeUnit.MILLISECONDS);
        o.onNext(4); 
        s.advanceTimeTo(700, TimeUnit.MILLISECONDS);
        o.onNext(5); 
        o.onNext(6); 
        s.advanceTimeTo(1001, TimeUnit.MILLISECONDS);
        o.onNext(7); 
        s.advanceTimeTo(1501, TimeUnit.MILLISECONDS);
        o.onComplete();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(2);
        inOrder.verify(observer).onNext(6);
        inOrder.verify(observer).onNext(7);
        inOrder.verify(observer).onComplete();
        inOrder.verifyNoMoreInteractions();
    }
}
