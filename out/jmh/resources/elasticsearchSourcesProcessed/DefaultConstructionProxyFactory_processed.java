/*
 * Copyright (C) 2006 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.elasticsearch.common.inject;

import org.elasticsearch.common.inject.spi.InjectionPoint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Produces construction proxies that invoke the class constructor.
 *
 * @author crazybob@google.com (Bob Lee)
 */
class DefaultConstructionProxyFactory<T> implements ConstructionProxyFactory<T> {

    private final InjectionPoint injectionPoint;

    /**
     * @param injectionPoint an injection point whose member is a constructor of {@code T}.
     */
    DefaultConstructionProxyFactory(InjectionPoint injectionPoint) {
        this.injectionPoint = injectionPoint;
    }

    @Override
    public ConstructionProxy<T> create() {
        @SuppressWarnings("unchecked") 
        final Constructor<T> constructor = (Constructor<T>) injectionPoint.getMember();

        return new ConstructionProxy<>() {
            @Override
            public T newInstance(Object... arguments) throws InvocationTargetException {
                try {
                    return constructor.newInstance(arguments);
                } catch (InstantiationException e) {
                    throw new AssertionError(e); 
                } catch (IllegalAccessException e) {
                    throw new AssertionError("Wrong access modifiers on " + constructor, e);
                }
            }

            @Override
            public InjectionPoint getInjectionPoint() {
                return injectionPoint;
            }

        };
    }
}
