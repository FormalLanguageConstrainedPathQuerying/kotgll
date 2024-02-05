/*
 * reserved comment block
 * DO NOT REMOVE OR ALTER!
 */
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sun.org.apache.xml.internal.utils;

/**
 * A digital search trie for 7-bit ASCII text
 * The API is a subset of java.util.Hashtable
 * The key must be a 7-bit ASCII string
 * The value may be any Java Object
 * @xsl.usage internal
 */
public class Trie
{

  /** Size of the m_nextChar array.  */
  public static final int ALPHA_SIZE = 128;

  /** The root node of the tree.    */
  Node m_Root;

  /** helper buffer to convert Strings to char arrays */
  private char[] m_charBuffer = new char[0];

  /**
   * Construct the trie.
   */
  public Trie()
  {
    m_Root = new Node();
  }

  /**
   * Put an object into the trie for lookup.
   *
   * @param key must be a 7-bit ASCII string
   * @param value any java object.
   *
   * @return The old object that matched key, or null.
   */
  public Object put(String key, Object value)
  {

    final int len = key.length();
    if (len > m_charBuffer.length)
    {
        m_charBuffer = new char[len];
    }

    Node node = m_Root;

    for (int i = 0; i < len; i++)
    {
      Node nextNode = node.m_nextChar[Character.toUpperCase(key.charAt(i))];

      if (nextNode != null)
      {
        node = nextNode;
      }
      else
      {
        for (; i < len; i++)
        {
          Node newNode = new Node();
          node.m_nextChar[Character.toUpperCase(key.charAt(i))] = newNode;
          node.m_nextChar[Character.toLowerCase(key.charAt(i))] = newNode;
          node = newNode;
        }
        break;
      }
    }

    Object ret = node.m_Value;

    node.m_Value = value;

    return ret;
  }

  /**
   * Get an object that matches the key.
   *
   * @param key must be a 7-bit ASCII string
   *
   * @return The object that matches the key, or null.
   */
public Object get(final String key)
{

    final int len = key.length();

    /* If the name is too long, we won't find it, this also keeps us
     * from overflowing m_charBuffer
     */
    if (m_charBuffer.length < len)
        return null;

    Node node = m_Root;
    switch (len) 
    {
        case 0 :
            {
                return null;
            }

        case 1 :
            {
                final char ch = key.charAt(0);
                if (ch < ALPHA_SIZE)
                {
                    node = node.m_nextChar[ch];
                    if (node != null)
                        return node.m_Value;
                }
                return null;
            }
        default :
            {
                key.getChars(0, len, m_charBuffer, 0);
                for (int i = 0; i < len; i++)
                {
                    final char ch = m_charBuffer[i];
                    if (ALPHA_SIZE <= ch)
                    {
                        return null;
                    }

                    node = node.m_nextChar[ch];
                    if (node == null)
                        return null;
                }

                return node.m_Value;
            }
    }
}

  /**
   * The node representation for the trie.
   * @xsl.usage internal
   */
  class Node
  {

    /**
     * Constructor, creates a Node[ALPHA_SIZE].
     */
    Node()
    {
      m_nextChar = new Node[ALPHA_SIZE];
      m_Value = null;
    }

    /** The next nodes.   */
    Node m_nextChar[];

    /** The value.   */
    Object m_Value;
  }
}
