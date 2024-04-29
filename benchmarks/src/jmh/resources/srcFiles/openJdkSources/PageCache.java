/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

package sun.jvm.hotspot.debugger;

/** This class implements an LRU page-level cache of configurable page
    size and number of pages. It is configured with a PageFetcher
    which enables it to transparently satisfy requests which span
    multiple pages when one or more of those pages is not in the
    cache. It is generic enough to be shareable among debugger
    implementations. */

import sun.jvm.hotspot.utilities.*;

public class PageCache {
  /** The pageSize must be a power of two and implicitly specifies the
      alignment of pages. numPages specifies how many pages should be
      cached. */
  public PageCache(long pageSize,
                   long maxNumPages,
                   PageFetcher fetcher) {
    checkPageInfo(pageSize, maxNumPages);
    this.pageSize    = pageSize;
    this.maxNumPages = maxNumPages;
    this.fetcher     = fetcher;
    addressToPageMap = new LongHashMap();
    enabled = true;
  }

  /** This handles fetches which span multiple pages by virtue of the
      presence of the PageFetcher. Throws UnmappedAddressException if
      a page on which data was requested was unmapped. This can not
      really handle numBytes > 32 bits. */
  public synchronized byte[] getData(long startAddress, long numBytes)
    throws UnmappedAddressException {
    byte[] data = new byte[(int) numBytes];
    long numRead = 0;

    while (numBytes > 0) {
      long pageBaseAddress = startAddress & pageMask;
      Page page = checkPage(getPage(pageBaseAddress), startAddress);
      long pageOffset = startAddress - pageBaseAddress;
      long numBytesFromPage = Math.min(pageSize - pageOffset, numBytes);
      page.getDataAsBytes(startAddress, numBytesFromPage, data, numRead);
      numRead      += numBytesFromPage;
      numBytes     -= numBytesFromPage;
      startAddress += numBytesFromPage;
    }

    return data;
  }

  public synchronized boolean getBoolean(long address) {
    return (getByte(address) != 0);
  }

  public synchronized byte getByte(long address) {
    return checkPage(getPage(address & pageMask), address).getByte(address);
  }

  public synchronized short getShort(long address, boolean bigEndian) {
    return checkPage(getPage(address & pageMask), address).getShort(address, bigEndian);
  }

  public synchronized char getChar(long address, boolean bigEndian) {
    return checkPage(getPage(address & pageMask), address).getChar(address, bigEndian);
  }

  public synchronized int getInt(long address, boolean bigEndian) {
    return checkPage(getPage(address & pageMask), address).getInt(address, bigEndian);
  }

  public synchronized long getLong(long address, boolean bigEndian) {
    return checkPage(getPage(address & pageMask), address).getLong(address, bigEndian);
  }

  public synchronized float getFloat(long address, boolean bigEndian) {
    return checkPage(getPage(address & pageMask), address).getFloat(address, bigEndian);
  }

  public synchronized double getDouble(long address, boolean bigEndian) {
    return checkPage(getPage(address & pageMask), address).getDouble(address, bigEndian);
  }

  /** A mechanism for clearing cached data covering the given region */
  public synchronized void clear(long startAddress, long numBytes) {
    long pageBaseAddress = startAddress & pageMask;
    long endAddress      = startAddress + numBytes;
    while (pageBaseAddress < endAddress) {
      flushPage(pageBaseAddress);
      pageBaseAddress += pageSize;
    }
  }

  /** A mechanism for clearing out the cache is necessary to handle
      detaching and reattaching */
  public synchronized void clear() {
    addressToPageMap.clear();
    lruList = null;
    numPages = 0;
  }

  /** Disables the page cache; no further pages will be added to the
      cache and all existing pages will be flushed. Call this when the
      target process has been resumed. */
  public synchronized void disable() {
    enabled = false;
    clear();
  }

  /** Enables the page cache; fetched pages will be added to the
      cache. Call this when the target process has been suspended. */
  public synchronized void enable() {
    enabled = true;
  }




  private boolean     enabled;
  private long        pageSize;
  private long        maxNumPages;
  private long        pageMask;
  private long        numPages;
  private PageFetcher fetcher;
  private LongHashMap addressToPageMap; 
  private Page        lruList; 

  /** Page fetcher plus LRU functionality */
  private Page getPage(long pageBaseAddress) {
    if (lruList != null) {
      if (lruList.getBaseAddress() == pageBaseAddress) {
        return lruList;
      }
    }
    long key = pageBaseAddress;
    Page page = (Page) addressToPageMap.get(key);
    if (page == null) {
      page = fetcher.fetchPage(pageBaseAddress, pageSize);
      if (enabled) {
        addressToPageMap.put(key, page);
        if (Assert.ASSERTS_ENABLED) {
          Assert.that(page == (Page) addressToPageMap.get(pageBaseAddress),
                      "must have found page in cache!");
        }
        addPageToList(page);
        if (numPages == maxNumPages) {
          Page evictedPage = lruList.getPrev();
          removePageFromList(evictedPage);
          addressToPageMap.remove(evictedPage.getBaseAddress());
        } else {
          ++numPages;
        }
      }
    } else {
      removePageFromList(page);
      addPageToList(page);
    }
    return page;
  }

  private Page checkPage(Page page, long startAddress) {
    if (!page.isMapped()) {
      throw new UnmappedAddressException(startAddress);
    }
    return page;
  }

  private int countPages() {
    Page page = lruList;
    int num = 0;
    if (page == null) {
      return num;
    }
    do {
      ++num;
      page = page.getNext();
    } while (page != lruList);
    return num;
  }

  private void flushPage(long pageBaseAddress) {
    long key = pageBaseAddress;
    Page page = (Page) addressToPageMap.remove(key);
    if (page != null) {
      removePageFromList(page);
    }
  }

  private void addPageToList(Page page) {
    if (lruList == null) {
      lruList = page;
      page.setNext(page);
      page.setPrev(page);
    } else {
      page.setNext(lruList);
      page.setPrev(lruList.getPrev());
      lruList.getPrev().setNext(page);
      lruList.setPrev(page);
      lruList = page;
    }
  }

  private void removePageFromList(Page page) {
    if (page.getNext() == page) {
      lruList = null;
    } else {
      if (lruList == page) {
        lruList = page.getNext();
      }
      page.getPrev().setNext(page.getNext());
      page.getNext().setPrev(page.getPrev());
    }
    page.setPrev(null);
    page.setNext(null);
  }

  /** Ensure that page size fits within 32 bits and is a power of two, and that maxNumPages > 0 */
  private void checkPageInfo(long pageSize, long maxNumPages) {
    if ((pageSize <= 0) || maxNumPages <= 0) {
      throw new IllegalArgumentException("pageSize and maxNumPages must both be greater than zero");
    }
    long tmpPageSize = pageSize >>> 32;
    if (tmpPageSize != 0) {
      throw new IllegalArgumentException("pageSize " + pageSize + " too big (must fit within 32 bits)");
    }
    int numNonZeroBits = 0;
    for (int i = 0; i < 32; ++i) {
      if ((pageSize & 1L) != 0) {
        ++numNonZeroBits;
        if ((numNonZeroBits > 1) || (i == 0)) {
          throw new IllegalArgumentException("pageSize " + pageSize + " must be a power of two");
        }
      }
      pageSize >>>= 1;
      if (numNonZeroBits == 0) {
        pageMask = (pageMask << 1) | 1L;
      }
    }
    pageMask = ~pageMask;
  }
}
