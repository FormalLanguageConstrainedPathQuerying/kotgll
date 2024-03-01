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

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.junit.Test;

import io.reactivex.rxjava3.core.*;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.*;
import io.reactivex.rxjava3.observables.GroupedObservable;
import io.reactivex.rxjava3.testsupport.TestObserverEx;

/**
 * Test super/extends of generics.
 *
 * See https:
 */
public class ObservableCovarianceTest extends RxJavaTest {

    /**
     * This won't compile if super/extends isn't done correctly on generics.
     */
    @Test
    public void covarianceOfFrom() {
        Observable.<Movie> just(new HorrorMovie());
        Observable.<Movie> fromIterable(new ArrayList<HorrorMovie>());
    }

    @Test
    public void sortedList() {
        Comparator<Media> sortFunction = new Comparator<Media>() {
            @Override
            public int compare(Media t1, Media t2) {
                return 1;
            }
        };

        Observable<Media> o = Observable.just(new Movie(), new TVSeason(), new Album());
        o.toSortedList(sortFunction);

        Observable<Movie> o2 = Observable.just(new Movie(), new ActionMovie(), new HorrorMovie());
        o2.toSortedList(sortFunction);
    }

    @Test
    public void groupByCompose() {
        Observable<Movie> movies = Observable.just(new HorrorMovie(), new ActionMovie(), new Movie());
        TestObserverEx<String> to = new TestObserverEx<>();
        movies
        .groupBy(new Function<Movie, Object>() {
            @Override
            public Object apply(Movie v) {
                return v.getClass();
            }
        })
        .doOnNext(new Consumer<GroupedObservable<Object, Movie>>() {
            @Override
            public void accept(GroupedObservable<Object, Movie> g) {
                System.out.println(g.getKey());
            }
        })
        .flatMap(new Function<GroupedObservable<Object, Movie>, Observable<String>>() {
            @Override
            public Observable<String> apply(GroupedObservable<Object, Movie> g) {
                return g
                .doOnNext(new Consumer<Movie>() {
                    @Override
                    public void accept(Movie pv) {
                        System.out.println(pv);
                    }
                })
                .compose(new ObservableTransformer<Movie, Movie>() {
                    @Override
                    public Observable<Movie> apply(Observable<Movie> m) {
                        return m.concatWith(Observable.just(new ActionMovie()));
                    }
                }
                )
                .map(new Function<Movie, String>() {
                    @Override
                    public String apply(Movie v) {
                        return v.toString();
                    }
                });
            }
        })
        .subscribe(to);
        to.assertTerminated();
        to.assertNoErrors();
        assertEquals(6, to.values().size());
    }

    @SuppressWarnings("unused")
    @Test
    public void covarianceOfCompose() {
        Observable<HorrorMovie> movie = Observable.just(new HorrorMovie());
        Observable<Movie> movie2 = movie.compose(new ObservableTransformer<HorrorMovie, Movie>() {
            @Override
            public Observable<Movie> apply(Observable<HorrorMovie> t) {
                return Observable.just(new Movie());
            }
        });
    }

    @SuppressWarnings("unused")
    @Test
    public void covarianceOfCompose2() {
        Observable<Movie> movie = Observable.<Movie> just(new HorrorMovie());
        Observable<HorrorMovie> movie2 = movie.compose(new ObservableTransformer<Movie, HorrorMovie>() {
            @Override
            public Observable<HorrorMovie> apply(Observable<Movie> t) {
                return Observable.just(new HorrorMovie());
            }
        });
    }

    @SuppressWarnings("unused")
    @Test
    public void covarianceOfCompose3() {
        Observable<Movie> movie = Observable.<Movie>just(new HorrorMovie());
        Observable<HorrorMovie> movie2 = movie.compose(new ObservableTransformer<Movie, HorrorMovie>() {
            @Override
            public Observable<HorrorMovie> apply(Observable<Movie> t) {
                return Observable.just(new HorrorMovie()).map(new Function<HorrorMovie, HorrorMovie>() {
                    @Override
                    public HorrorMovie apply(HorrorMovie v) {
                        return v;
                    }
                });
            }
        }
        );
    }

    @SuppressWarnings("unused")
    @Test
    public void covarianceOfCompose4() {
        Observable<HorrorMovie> movie = Observable.just(new HorrorMovie());
        Observable<HorrorMovie> movie2 = movie.compose(new ObservableTransformer<HorrorMovie, HorrorMovie>() {
            @Override
            public Observable<HorrorMovie> apply(Observable<HorrorMovie> t1) {
                return t1.map(new Function<HorrorMovie, HorrorMovie>() {
                    @Override
                    public HorrorMovie apply(HorrorMovie v) {
                        return v;
                    }
                });
            }
        });
    }

    @Test
    public void composeWithDeltaLogic() {
        List<Movie> list1 = Arrays.asList(new Movie(), new HorrorMovie(), new ActionMovie());
        List<Movie> list2 = Arrays.asList(new ActionMovie(), new Movie(), new HorrorMovie(), new ActionMovie());
        Observable<List<Movie>> movies = Observable.just(list1, list2);
        movies.compose(deltaTransformer);
    }

    static Function<List<List<Movie>>, Observable<Movie>> calculateDelta = new Function<List<List<Movie>>, Observable<Movie>>() {
        @Override
        public Observable<Movie> apply(List<List<Movie>> listOfLists) {
            if (listOfLists.size() == 1) {
                return Observable.fromIterable(listOfLists.get(0));
            } else {
                List<Movie> newList = listOfLists.get(1);
                List<Movie> oldList = new ArrayList<>(listOfLists.get(0));

                Set<Movie> delta = new LinkedHashSet<>();
                delta.addAll(newList);
                delta.removeAll(oldList);

                oldList.removeAll(newList);

                for (@SuppressWarnings("unused") Movie old : oldList) {
                    delta.add(new Movie());
                }

                return Observable.fromIterable(delta);
            }
        }
    };

    static ObservableTransformer<List<Movie>, Movie> deltaTransformer = new ObservableTransformer<List<Movie>, Movie>() {
        @Override
        public Observable<Movie> apply(Observable<List<Movie>> movieList) {
            return movieList
                .startWithItem(new ArrayList<>())
                .buffer(2, 1)
                .skip(1)
                .flatMap(calculateDelta);
        }
    };

    /*
     * Most tests are moved into their applicable classes such as [Operator]Tests.java
     */

    static class Media {
    }

    static class Movie extends Media {
    }

    static class HorrorMovie extends Movie {
    }

    static class ActionMovie extends Movie {
    }

    static class Album extends Media {
    }

    static class TVSeason extends Media {
    }

    static class Rating {
    }

    static class CoolRating extends Rating {
    }

    static class Result {
    }

    static class ExtendedResult extends Result {
    }
}
