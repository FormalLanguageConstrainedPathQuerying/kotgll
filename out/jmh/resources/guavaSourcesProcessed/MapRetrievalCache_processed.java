/*
 * Copyright (C) 2016 The Guava Authors
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

package com.google.common.graph;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import javax.annotation.CheckForNull;

/**
 * A {@link MapIteratorCache} that adds additional caching. In addition to the caching provided by
 * {@link MapIteratorCache}, this structure caches values for the two most recently retrieved keys.
 *
 * @author James Sexton
 */
@ElementTypesAreNonnullByDefault
final class MapRetrievalCache<K, V> extends MapIteratorCache<K, V> {
  @CheckForNull private transient volatile CacheEntry<K, V> cacheEntry1;
  @CheckForNull private transient volatile CacheEntry<K, V> cacheEntry2;

  MapRetrievalCache(Map<K, V> backingMap) {
    super(backingMap);
  }

  @SuppressWarnings("unchecked") 
  @Override
  @CheckForNull
  V get(Object key) {
    checkNotNull(key);
    V value = getIfCached(key);
    if (value != null) {
      return value;
    }

    value = getWithoutCaching(key);
    if (value != null) {
      addToCache((K) key, value);
    }
    return value;
  }


  @Override
  @CheckForNull
  V getIfCached(@CheckForNull Object key) {
    V value = super.getIfCached(key);
    if (value != null) {
      return value;
    }

    CacheEntry<K, V> entry;

    entry = cacheEntry1;
    if (entry != null && entry.key == key) {
      return entry.value;
    }
    entry = cacheEntry2;
    if (entry != null && entry.key == key) {
      addToCache(entry);
      return entry.value;
    }
    return null;
  }

  @Override
  void clearCache() {
    super.clearCache();
    cacheEntry1 = null;
    cacheEntry2 = null;
  }

  private void addToCache(K key, V value) {
    addToCache(new CacheEntry<K, V>(key, value));
  }

  private void addToCache(CacheEntry<K, V> entry) {
    cacheEntry2 = cacheEntry1;
    cacheEntry1 = entry;
  }

  private static final class CacheEntry<K, V> {
    final K key;
    final V value;

    CacheEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }
  }
}
