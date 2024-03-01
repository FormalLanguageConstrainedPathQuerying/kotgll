/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.org.apache.xerces.internal.impl;
import com.sun.org.apache.xerces.internal.util.DefaultErrorHandler;
import com.sun.org.apache.xerces.internal.util.ErrorHandlerProxy;
import com.sun.org.apache.xerces.internal.util.MessageFormatter;
import com.sun.org.apache.xerces.internal.xni.XMLLocator;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponent;
import com.sun.org.apache.xerces.internal.xni.parser.XMLComponentManager;
import com.sun.org.apache.xerces.internal.xni.parser.XMLConfigurationException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.xml.sax.ErrorHandler;

/**
 * This class is a common element of all parser configurations and is
 * used to report errors that occur. This component can be queried by
 * parser components from the component manager using the following
 * property ID:
 * <pre>
 *   http:
 * </pre>
 * <p>
 * Errors are separated into domains that categorize a class of errors.
 * In a parser configuration, the parser would register a
 * <code>MessageFormatter</code> for each domain that is capable of
 * localizing error messages and formatting them based on information
 * about the error. Any parser component can invent new error domains
 * and register additional message formatters to localize messages in
 * those domains.
 * <p>
 * This component requires the following features and properties from the
 * component manager that uses it:
 * <ul>
 *  <li>http:
 * </ul>
 * <p>
 * This component can use the following features and properties but they
 * are not required:
 * <ul>
 *  <li>http:
 * </ul>
 *
 * @xerces.internal
 *
 * @see MessageFormatter
 *
 * @author Eric Ye, IBM
 * @author Andy Clark, IBM
 *
 * @LastModified: Nov 2017
 */
public class XMLErrorReporter
    implements XMLComponent {



    /**
     * Severity: warning. Warnings represent informational messages only
     * that should not be considered serious enough to stop parsing or
     * indicate an error in the document's validity.
     */
    public static final short SEVERITY_WARNING = 0;

    /**
     * Severity: error. Common causes of errors are document structure and/or
     * content that that does not conform to the grammar rules specified for
     * the document. These are typically validation errors.
     */
    public static final short SEVERITY_ERROR = 1;

    /**
     * Severity: fatal error. Fatal errors are errors in the syntax of the
     * XML document or invalid byte sequences for a given encoding. The
     * XML 1.0 Specification mandates that errors of this type are not
     * recoverable.
     * <p>
     * <strong>Note:</strong> The parser does have a "continue after fatal
     * error" feature but it should be used with extreme caution and care.
     */
    public static final short SEVERITY_FATAL_ERROR = 2;


    /** Feature identifier: continue after fatal error. */
    protected static final String CONTINUE_AFTER_FATAL_ERROR =
        Constants.XERCES_FEATURE_PREFIX + Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE;


    /** Property identifier: error handler. */
    protected static final String ERROR_HANDLER =
        Constants.XERCES_PROPERTY_PREFIX + Constants.ERROR_HANDLER_PROPERTY;


    /** Recognized features. */
    private static final String[] RECOGNIZED_FEATURES = {
        CONTINUE_AFTER_FATAL_ERROR,
    };

    /** Feature defaults. */
    private static final Boolean[] FEATURE_DEFAULTS = {
        null,
    };

    /** Recognized properties. */
    private static final String[] RECOGNIZED_PROPERTIES = {
        ERROR_HANDLER,
    };

    /** Property defaults. */
    private static final Object[] PROPERTY_DEFAULTS = {
        null,
    };


    /** The locale to be used to format error messages. */
    protected Locale fLocale;

    /** Mapping of Message formatters for domains. */
    protected Map<String, MessageFormatter> fMessageFormatters;

    /** Error handler. */
    protected XMLErrorHandler fErrorHandler;

    /** Document locator. */
    protected XMLLocator fLocator;

    /** Continue after fatal error feature. */
    protected boolean fContinueAfterFatalError;

    /**
     * Default error handler. This error handler is only used in the
     * absence of a registered error handler so that errors are not
     * "swallowed" silently. This is one of the most common "problems"
     * reported by users of the parser.
     */
    protected XMLErrorHandler fDefaultErrorHandler;

    /** A SAX proxy to the error handler contained in this error reporter. */
    private ErrorHandler fSaxProxy = null;


    /** Constructs an error reporter with a locator. */
    public XMLErrorReporter() {


        fMessageFormatters = new HashMap<>();

    } 


    /**
     * Sets the current locale.
     *
     * @param locale The new locale.
     */
    public void setLocale(Locale locale) {
        fLocale = locale;
    } 

    /**
     * Gets the current locale.
     *
     * @return the current Locale
     */
    public Locale getLocale() {
        return fLocale ;
    } 

    /**
     * Sets the document locator.
     *
     * @param locator The locator.
     */
    public void setDocumentLocator(XMLLocator locator) {
        fLocator = locator;
    } 

    /**
     * Registers a message formatter for the specified domain.
     * <p>
     * <strong>Note:</strong> Registering a message formatter for a domain
     * when there is already a formatter registered will cause the previous
     * formatter to be lost. This method replaces any previously registered
     * message formatter for the specified domain.
     *
     * @param domain
     * @param messageFormatter
     */
    public void putMessageFormatter(String domain,
                                    MessageFormatter messageFormatter) {
        fMessageFormatters.put(domain, messageFormatter);
    } 

    /**
     * Returns the message formatter associated with the specified domain,
     * or null if no message formatter is registered for that domain.
     *
     * @param domain The domain of the message formatter.
     */
    public MessageFormatter getMessageFormatter(String domain) {
        return fMessageFormatters.get(domain);
    } 

    /**
     * Removes the message formatter for the specified domain and
     * returns the removed message formatter.
     *
     * @param domain The domain of the message formatter.
     */
    public MessageFormatter removeMessageFormatter(String domain) {
        return fMessageFormatters.remove(domain);
    } 

    /**
     * Reports an error. The error message passed to the error handler
     * is formatted for the locale by the message formatter installed
     * for the specified error domain.
     *
     * @param domain    The error domain.
     * @param key       The key of the error message.
     * @param arguments The replacement arguments for the error message,
     *                  if needed.
     * @param severity  The severity of the error.
     * @return          The formatted error message.
     *
     * @see #SEVERITY_WARNING
     * @see #SEVERITY_ERROR
     * @see #SEVERITY_FATAL_ERROR
     */
    public String reportError(String domain, String key, Object[] arguments,
                            short severity) throws XNIException {
        return reportError(fLocator, domain, key, arguments, severity);
    } 

    /**
     * Reports an error. The error message passed to the error handler
     * is formatted for the locale by the message formatter installed
     * for the specified error domain.
     *
     * @param domain    The error domain.
     * @param key       The key of the error message.
     * @param arguments The replacement arguments for the error message,
     *                  if needed.
     * @param severity  The severity of the error.
     * @param exception The exception to wrap.
     * @return          The formatted error message.
     *
     * @see #SEVERITY_WARNING
     * @see #SEVERITY_ERROR
     * @see #SEVERITY_FATAL_ERROR
     */
    public String reportError(String domain, String key, Object[] arguments,
            short severity, Exception exception) throws XNIException {
        return reportError(fLocator, domain, key, arguments, severity, exception);
    } 

    /**
     * Reports an error at a specific location.
     *
     * @param location  The error location.
     * @param domain    The error domain.
     * @param key       The key of the error message.
     * @param arguments The replacement arguments for the error message,
     *                  if needed.
     * @param severity  The severity of the error.
     * @return          The formatted error message.
     *
     * @see #SEVERITY_WARNING
     * @see #SEVERITY_ERROR
     * @see #SEVERITY_FATAL_ERROR
     */
    public String reportError(XMLLocator location,
            String domain, String key, Object[] arguments,
            short severity) throws XNIException {
        return reportError(location, domain, key, arguments, severity, null);
    } 

    /**
     * Reports an error at a specific location.
     *
     * @param location  The error location.
     * @param domain    The error domain.
     * @param key       The key of the error message.
     * @param arguments The replacement arguments for the error message,
     *                  if needed.
     * @param severity  The severity of the error.
     * @param exception The exception to wrap.
     * @return          The formatted error message.
     *
     * @see #SEVERITY_WARNING
     * @see #SEVERITY_ERROR
     * @see #SEVERITY_FATAL_ERROR
     */
    public String reportError(XMLLocator location,
                            String domain, String key, Object[] arguments,
                            short severity, Exception exception) throws XNIException {


        MessageFormatter messageFormatter = getMessageFormatter(domain);
        String message;
        if (messageFormatter != null) {
            message = messageFormatter.formatMessage(fLocale, key, arguments);
        }
        else {
            StringBuffer str = new StringBuffer();
            str.append(domain);
            str.append('#');
            str.append(key);
            int argCount = arguments != null ? arguments.length : 0;
            if (argCount > 0) {
                str.append('?');
                for (int i = 0; i < argCount; i++) {
                    str.append(arguments[i]);
                    if (i < argCount -1) {
                        str.append('&');
                    }
                }
            }
            message = str.toString();
        }
        XMLParseException parseException = (exception != null) ?
            new XMLParseException(location, message, exception) :
            new XMLParseException(location, message);

        XMLErrorHandler errorHandler = fErrorHandler;
        if (errorHandler == null) {
            if (fDefaultErrorHandler == null) {
                fDefaultErrorHandler = new DefaultErrorHandler();
            }
            errorHandler = fDefaultErrorHandler;
        }

        switch (severity) {
            case SEVERITY_WARNING: {
                errorHandler.warning(domain, key, parseException);
                break;
            }
            case SEVERITY_ERROR: {
                errorHandler.error(domain, key, parseException);
                break;
            }
            case SEVERITY_FATAL_ERROR: {
                errorHandler.fatalError(domain, key, parseException);
                if (!fContinueAfterFatalError) {
                    throw parseException;
                }
                break;
            }
        }
        return message;

    } 


    /**
     * Resets the component. The component can query the component manager
     * about any features and properties that affect the operation of the
     * component.
     *
     * @param componentManager The component manager.
     *
     * @throws SAXException Thrown by component on initialization error.
     *                      For example, if a feature or property is
     *                      required for the operation of the component, the
     *                      component manager may throw a
     *                      SAXNotRecognizedException or a
     *                      SAXNotSupportedException.
     */
    public void reset(XMLComponentManager componentManager)
        throws XNIException {

        fContinueAfterFatalError = componentManager.getFeature(CONTINUE_AFTER_FATAL_ERROR, false);

        fErrorHandler = (XMLErrorHandler)componentManager.getProperty(ERROR_HANDLER);

    } 

    /**
     * Returns a list of feature identifiers that are recognized by
     * this component. This method may return null if no features
     * are recognized by this component.
     */
    public String[] getRecognizedFeatures() {
        return RECOGNIZED_FEATURES.clone();
    } 

    /**
     * Sets the state of a feature. This method is called by the component
     * manager any time after reset when a feature changes state.
     * <p>
     * <strong>Note:</strong> Components should silently ignore features
     * that do not affect the operation of the component.
     *
     * @param featureId The feature identifier.
     * @param state     The state of the feature.
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    public void setFeature(String featureId, boolean state)
        throws XMLConfigurationException {


        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            final int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();

            if (suffixLength == Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE.length() &&
                featureId.endsWith(Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE)) {
                fContinueAfterFatalError = state;
            }
        }

    } 

    public boolean getFeature(String featureId)
        throws XMLConfigurationException {


        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
                final int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();

            if (suffixLength == Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE.length() &&
                featureId.endsWith(Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE)) {
                return fContinueAfterFatalError ;
            }
        }
        return false;

    } 

    /**
     * Returns a list of property identifiers that are recognized by
     * this component. This method may return null if no properties
     * are recognized by this component.
     */
    public String[] getRecognizedProperties() {
        return RECOGNIZED_PROPERTIES.clone();
    } 

    /**
     * Sets the value of a property. This method is called by the component
     * manager any time after reset when a property changes value.
     * <p>
     * <strong>Note:</strong> Components should silently ignore properties
     * that do not affect the operation of the component.
     *
     * @param propertyId The property identifier.
     * @param value      The value of the property.
     *
     * @throws SAXNotRecognizedException The component should not throw
     *                                   this exception.
     * @throws SAXNotSupportedException The component should not throw
     *                                  this exception.
     */
    public void setProperty(String propertyId, Object value)
        throws XMLConfigurationException {


        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            final int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();

            if (suffixLength == Constants.ERROR_HANDLER_PROPERTY.length() &&
                propertyId.endsWith(Constants.ERROR_HANDLER_PROPERTY)) {
                fErrorHandler = (XMLErrorHandler)value;
            }
        }

    } 

    /**
     * Returns the default state for a feature, or null if this
     * component does not want to report a default value for this
     * feature.
     *
     * @param featureId The feature identifier.
     *
     * @since Xerces 2.2.0
     */
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return null;
    } 

    /**
     * Returns the default state for a property, or null if this
     * component does not want to report a default value for this
     * property.
     *
     * @param propertyId The property identifier.
     *
     * @since Xerces 2.2.0
     */
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return null;
    } 

    /**
     * Get the internal XMLErrrorHandler.
     */
    public XMLErrorHandler getErrorHandler() {
        return fErrorHandler;
    }

    /**
     * Gets the internal XMLErrorHandler
     * as SAX ErrorHandler.
     */
    public ErrorHandler getSAXErrorHandler() {
        if (fSaxProxy == null) {
            fSaxProxy = new ErrorHandlerProxy() {
                protected XMLErrorHandler getErrorHandler() {
                    return fErrorHandler;
                }
            };
        }
        return fSaxProxy;
    }

} 
