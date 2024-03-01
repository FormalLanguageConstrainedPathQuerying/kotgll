/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.cli;

/**
 * POSIX exit codes.
 */
public class ExitCodes {
    public static final int OK = 0;
    public static final int USAGE = 64;          
    public static final int DATA_ERROR = 65;     
    public static final int NO_INPUT = 66;       
    public static final int NO_USER = 67;        
    public static final int NO_HOST = 68;        
    public static final int UNAVAILABLE = 69;    
    public static final int CODE_ERROR = 70;     
    public static final int CANT_CREATE = 73;    
    public static final int IO_ERROR = 74;       
    public static final int TEMP_FAILURE = 75;   
    public static final int PROTOCOL = 76;       
    public static final int NOPERM = 77;         
    public static final int CONFIG = 78;         
    public static final int NOOP = 80;           

    private ExitCodes() { /* no instance, just constants */ }
}
