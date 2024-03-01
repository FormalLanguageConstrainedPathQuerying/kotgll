/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xpath.internal.objects;

import com.sun.org.apache.xpath.internal.ExpressionOwner;
import com.sun.org.apache.xpath.internal.XPathContext;
import com.sun.org.apache.xpath.internal.XPathVisitor;

/**
 * This class represents an XPath number, and is capable of
 * converting the number to other types, such as a string.
 * @xsl.usage general
 */
public class XNumber extends XObject
{
    static final long serialVersionUID = -2720400709619020193L;

  /** Value of the XNumber object.
   *  @serial         */
  double m_val;

  /**
   * Construct a XNodeSet object.
   *
   * @param d Value of the object
   */
  public XNumber(double d)
  {
    super();

    m_val = d;
  }

  /**
   * Construct a XNodeSet object.
   *
   * @param num Value of the object
   */
  public XNumber(Number num)
  {

    super();

    m_val = num.doubleValue();
    setObject(num);
  }

  /**
   * Tell that this is a CLASS_NUMBER.
   *
   * @return node type CLASS_NUMBER
   */
  public int getType()
  {
    return CLASS_NUMBER;
  }

  /**
   * Given a request type, return the equivalent string.
   * For diagnostic purposes.
   *
   * @return type string "#NUMBER"
   */
  public String getTypeString()
  {
    return "#NUMBER";
  }

  /**
   * Cast result object to a number.
   *
   * @return the value of the XNumber object
   */
  public double num()
  {
    return m_val;
  }

  /**
   * Evaluate expression to a number.
   *
   * @return 0.0
   *
   * @throws javax.xml.transform.TransformerException
   */
  public double num(XPathContext xctxt)
    throws javax.xml.transform.TransformerException
  {

    return m_val;
  }

  /**
   * Cast result object to a boolean.
   *
   * @return false if the value is NaN or equal to 0.0
   */
  public boolean bool()
  {
    return (Double.isNaN(m_val) || (m_val == 0.0)) ? false : true;
  }


  /**
   * Cast result object to a string.
   *
   * @return "NaN" if the number is NaN, Infinity or -Infinity if
   * the number is infinite or the string value of the number.
   */
  public String str()
  {

    if (Double.isNaN(m_val))
    {
      return "NaN";
    }
    else if (Double.isInfinite(m_val))
    {
      if (m_val > 0)
        return "Infinity";
      else
        return "-Infinity";
    }

    double num = m_val;
    String s = Double.toString(num);
    int len = s.length();

    if (s.charAt(len - 2) == '.' && s.charAt(len - 1) == '0')
    {
      s = s.substring(0, len - 2);

      if (s.equals("-0"))
        return "0";

      return s;
    }

    int e = s.indexOf('E');

    if (e < 0)
    {
      if (s.charAt(len - 1) == '0')
        return s.substring(0, len - 1);
      else
        return s;
    }

    int exp = Integer.parseInt(s.substring(e + 1));
    String sign;

    if (s.charAt(0) == '-')
    {
      sign = "-";
      s = s.substring(1);

      --e;
    }
    else
      sign = "";

    int nDigits = e - 2;

    if (exp >= nDigits)
      return sign + s.substring(0, 1) + s.substring(2, e)
             + zeros(exp - nDigits);

    while (s.charAt(e-1) == '0')
      e--;

    if (exp > 0)
      return sign + s.substring(0, 1) + s.substring(2, 2 + exp) + "."
             + s.substring(2 + exp, e);

    return sign + "0." + zeros(-1 - exp) + s.substring(0, 1)
           + s.substring(2, e);
  }


  /**
   * Return a string of '0' of the given length
   *
   *
   * @param n Length of the string to be returned
   *
   * @return a string of '0' with the given length
   */
  static private String zeros(int n)
  {
    if (n < 1)
      return "";

    char[] buf = new char[n];

    for (int i = 0; i < n; i++)
    {
      buf[i] = '0';
    }

    return new String(buf);
  }

  /**
   * Return a java object that's closest to the representation
   * that should be handed to an extension.
   *
   * @return The value of this XNumber as a Double object
   */
  public Object object()
  {
    if(null == m_obj)
      setObject(m_val);
    return m_obj;
  }

  /**
   * Tell if two objects are functionally equal.
   *
   * @param obj2 Object to compare this to
   *
   * @return true if the two objects are equal
   *
   * @throws javax.xml.transform.TransformerException
   */
  public boolean equals(XObject obj2)
  {

    int t = obj2.getType();
    try
    {
            if (t == XObject.CLASS_NODESET)
              return obj2.equals(this);
            else if(t == XObject.CLASS_BOOLEAN)
              return obj2.bool() == bool();
                else
               return m_val == obj2.num();
    }
    catch(javax.xml.transform.TransformerException te)
    {
      throw new com.sun.org.apache.xml.internal.utils.WrappedRuntimeException(te);
    }
  }

  /**
   * Tell if this expression returns a stable number that will not change during
   * iterations within the expression.  This is used to determine if a proximity
   * position predicate can indicate that no more searching has to occur.
   *
   *
   * @return true if the expression represents a stable number.
   */
  public boolean isStableNumber()
  {
    return true;
  }

  /**
   * @see com.sun.org.apache.xpath.internal.XPathVisitable#callVisitors(ExpressionOwner, XPathVisitor)
   */
  public void callVisitors(ExpressionOwner owner, XPathVisitor visitor)
  {
        visitor.visitNumberLiteral(owner, this);
  }


}
