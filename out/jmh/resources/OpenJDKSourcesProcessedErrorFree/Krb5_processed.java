/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */

/*
 *
 *  (C) Copyright IBM Corp. 1999 All Rights Reserved.
 *  Copyright 1997 The Open Group Research Institute.  All rights reserved.
 */

package sun.security.krb5.internal;

import sun.security.action.GetBooleanAction;

import java.util.Hashtable;


public class Krb5 {

    public static final int DEFAULT_ALLOWABLE_CLOCKSKEW = 5 * 60; 
    public static final int DEFAULT_MINIMUM_LIFETIME = 5 * 60; 
    public static final int DEFAULT_MAXIMUM_RENEWABLE_LIFETIME = 7 * 24 * 60 * 60; 
    public static final int DEFAULT_MAXIMUM_TICKET_LIFETIME = 24 * 60 * 60; 
    public static final boolean DEFAULT_FORWARDABLE_ALLOWED = true;
    public static final boolean DEFAULT_PROXIABLE_ALLOWED = true;
    public static final boolean DEFAULT_POSTDATE_ALLOWED = true;
    public static final boolean DEFAULT_RENEWABLE_ALLOWED = true;
    public static final boolean AP_EMPTY_ADDRESSES_ALLOWED = true;


    public static final int AP_OPTS_RESERVED        = 0;
    public static final int AP_OPTS_USE_SESSION_KEY = 1;
    public static final int AP_OPTS_MUTUAL_REQUIRED = 2;
    public static final int AP_OPTS_MAX             = 31;


    public static final int TKT_OPTS_RESERVED     = 0;
    public static final int TKT_OPTS_FORWARDABLE  = 1;
    public static final int TKT_OPTS_FORWARDED    = 2;
    public static final int TKT_OPTS_PROXIABLE    = 3;
    public static final int TKT_OPTS_PROXY        = 4;
    public static final int TKT_OPTS_MAY_POSTDATE = 5;
    public static final int TKT_OPTS_POSTDATED    = 6;
    public static final int TKT_OPTS_INVALID      = 7;
    public static final int TKT_OPTS_RENEWABLE    = 8;
    public static final int TKT_OPTS_INITIAL      = 9;
    public static final int TKT_OPTS_PRE_AUTHENT  = 10;
    public static final int TKT_OPTS_HW_AUTHENT   = 11;
    public static final int TKT_OPTS_DELEGATE     = 13;
    public static final int TKT_OPTS_ENC_PA_REP   = 15;
    public static final int TKT_OPTS_MAX          = 31;

    public static final int KDC_OPTS_MAX          = 31;

    public static final int KRB_FLAGS_MAX         = 31;


    public static final int LRTYPE_NONE                 = 0;
    public static final int LRTYPE_TIME_OF_INITIAL_TGT  = 1;
    public static final int LRTYPE_TIME_OF_INITIAL_REQ  = 2;
    public static final int LRTYPE_TIME_OF_NEWEST_TGT   = 3;
    public static final int LRTYPE_TIME_OF_LAST_RENEWAL = 4;
    public static final int LRTYPE_TIME_OF_LAST_REQ     = 5;


    public static final int ADDR_LEN_INET      = 4;
    public static final int ADDR_LEN_CHAOS     = 2;
    public static final int ADDR_LEN_OSI       = 0; 
    public static final int ADDR_LEN_XNS       = 6;
    public static final int ADDR_LEN_APPLETALK = 3;
    public static final int ADDR_LEN_DECNET    = 2;


    public static final int ADDRTYPE_UNIX      = 1;  
    public static final int ADDRTYPE_INET      = 2;  
    public static final int ADDRTYPE_IMPLINK   = 3;  
    public static final int ADDRTYPE_PUP       = 4;  
    public static final int ADDRTYPE_CHAOS     = 5;  
    public static final int ADDRTYPE_XNS       = 6;  
    public static final int ADDRTYPE_IPX       = 6;  
    public static final int ADDRTYPE_ISO       = 7;  
    public static final int ADDRTYPE_ECMA      = 8;  
    public static final int ADDRTYPE_DATAKIT   = 9;  
    public static final int ADDRTYPE_CCITT     = 10; 
    public static final int ADDRTYPE_SNA       = 11; 
    public static final int ADDRTYPE_DECNET    = 12; 
    public static final int ADDRTYPE_DLI       = 13; 
    public static final int ADDRTYPE_LAT       = 14; 
    public static final int ADDRTYPE_HYLINK    = 15; 
    public static final int ADDRTYPE_APPLETALK = 16; 
    public static final int ADDRTYPE_NETBIOS   = 17; 
    public static final int ADDRTYPE_VOICEVIEW = 18; 
    public static final int ADDRTYPE_FIREFOX   = 19; 
    public static final int ADDRTYPE_BAN       = 21; 
    public static final int ADDRTYPE_ATM       = 22; 
    public static final int ADDRTYPE_INET6     = 24; 


    public static final int KDC_INET_DEFAULT_PORT = 88;


    public static final int KDC_RETRY_LIMIT = 3;
    public static final int KDC_DEFAULT_UDP_PREF_LIMIT = 1465;
    public static final int KDC_HARD_UDP_LIMIT = 32700;




    public static final int KEYTYPE_NULL = 0;
    public static final int KEYTYPE_DES  = 1;

    public static final int KEYTYPE_DES3 = 2;
    public static final int KEYTYPE_AES  = 3;
    public static final int KEYTYPE_ARCFOUR_HMAC = 4;


    public static final int PA_TGS_REQ       = 1;
    public static final int PA_ENC_TIMESTAMP = 2;
    public static final int PA_PW_SALT       = 3;

    public static final int PA_ETYPE_INFO    = 11;
    public static final int PA_ETYPE_INFO2   = 19;

    public static final int PA_FOR_USER      = 129;
    public static final int PA_PAC_OPTIONS   = 167;

    public static final int PA_REQ_ENC_PA_REP = 149;

    public static final int OSF_DCE = 64;
    public static final int SESAME  = 65;

    public static final int ATT_CHALLENGE_RESPONSE = 64;

    public static final int DOMAIN_X500_COMPRESS = 1;

    public static final int PVNO = 5;   
    public static final int AUTHNETICATOR_VNO = 5;   
    public static final int TICKET_VNO = 5;   


    public static final int KRB_AS_REQ =  10;     
    public static final int KRB_AS_REP =  11;     
    public static final int KRB_TGS_REQ = 12;     
    public static final int KRB_TGS_REP = 13;     
    public static final int KRB_AP_REQ =  14;     
    public static final int KRB_AP_REP =  15;     
    public static final int KRB_SAFE =    20;     
    public static final int KRB_PRIV =    21;     
    public static final int KRB_CRED =    22;     
    public static final int KRB_ERROR =   30;     


    public static final int KRB_TKT               = 1;  
    public static final int KRB_AUTHENTICATOR     = 2;  
    public static final int KRB_ENC_TKT_PART      = 3;  
    public static final int KRB_ENC_AS_REP_PART   = 25; 
    public static final int KRB_ENC_TGS_REP_PART  = 26; 
    public static final int KRB_ENC_AP_REP_PART   = 27; 
    public static final int KRB_ENC_KRB_PRIV_PART = 28; 
    public static final int KRB_ENC_KRB_CRED_PART = 29; 



    public static final int KDC_ERR_NONE                 =  0;   
    public static final int KDC_ERR_NAME_EXP             =  1;   
    public static final int KDC_ERR_SERVICE_EXP          =  2;   
    public static final int KDC_ERR_BAD_PVNO             =  3;   
    public static final int KDC_ERR_C_OLD_MAST_KVNO      =  4;   
    public static final int KDC_ERR_S_OLD_MAST_KVNO      =  5;   
    public static final int KDC_ERR_C_PRINCIPAL_UNKNOWN  =  6;   
    public static final int KDC_ERR_S_PRINCIPAL_UNKNOWN  =  7;   
    public static final int KDC_ERR_PRINCIPAL_NOT_UNIQUE =  8;   
    public static final int KDC_ERR_NULL_KEY             =  9;   
    public static final int KDC_ERR_CANNOT_POSTDATE      = 10;   
    public static final int KDC_ERR_NEVER_VALID          = 11;   
    public static final int KDC_ERR_POLICY               = 12;   
    public static final int KDC_ERR_BADOPTION            = 13;   
    public static final int KDC_ERR_ETYPE_NOSUPP         = 14;   
    public static final int KDC_ERR_SUMTYPE_NOSUPP       = 15;   
    public static final int KDC_ERR_PADATA_TYPE_NOSUPP   = 16;   
    public static final int KDC_ERR_TRTYPE_NOSUPP        = 17;   
    public static final int KDC_ERR_CLIENT_REVOKED       = 18;   
    public static final int KDC_ERR_SERVICE_REVOKED      = 19;   
    public static final int KDC_ERR_TGT_REVOKED          = 20;   
    public static final int KDC_ERR_CLIENT_NOTYET        = 21;   
    public static final int KDC_ERR_SERVICE_NOTYET       = 22;   
    public static final int KDC_ERR_KEY_EXPIRED          = 23;   
    public static final int KDC_ERR_PREAUTH_FAILED       = 24;   
    public static final int KDC_ERR_PREAUTH_REQUIRED     = 25;   
    public static final int KDC_ERR_SERVER_NOMATCH       = 26;   
    public static final int KDC_ERR_MUST_USE_USER2USER   = 27;   
    public static final int KDC_ERR_PATH_NOT_ACCEPTED    = 28;   
    public static final int KDC_ERR_SVC_UNAVAILABLE      = 29;   
    public static final int KRB_AP_ERR_BAD_INTEGRITY     = 31;   
    public static final int KRB_AP_ERR_TKT_EXPIRED       = 32;   
    public static final int KRB_AP_ERR_TKT_NYV           = 33;   
    public static final int KRB_AP_ERR_REPEAT            = 34;   
    public static final int KRB_AP_ERR_NOT_US            = 35;   
    public static final int KRB_AP_ERR_BADMATCH          = 36;   
    public static final int KRB_AP_ERR_SKEW              = 37;   
    public static final int KRB_AP_ERR_BADADDR           = 38;   
    public static final int KRB_AP_ERR_BADVERSION        = 39;   
    public static final int KRB_AP_ERR_MSG_TYPE          = 40;   
    public static final int KRB_AP_ERR_MODIFIED          = 41;   
    public static final int KRB_AP_ERR_BADORDER          = 42;   
    public static final int KRB_AP_ERR_BADKEYVER         = 44;   
    public static final int KRB_AP_ERR_NOKEY             = 45;   
    public static final int KRB_AP_ERR_MUT_FAIL          = 46;   
    public static final int KRB_AP_ERR_BADDIRECTION      = 47;   
    public static final int KRB_AP_ERR_METHOD            = 48;   
    public static final int KRB_AP_ERR_BADSEQ            = 49;   
    public static final int KRB_AP_ERR_INAPP_CKSUM       = 50;   
    public static final int KRB_AP_PATH_NOT_ACCEPTED     = 51;   
    public static final int KRB_ERR_RESPONSE_TOO_BIG     = 52;   
    public static final int KRB_ERR_GENERIC              = 60;   
    public static final int KRB_ERR_FIELD_TOOLONG        = 61;   
    public static final int KRB_ERR_WRONG_REALM          = 68;   

    public static final int KRB_CRYPTO_NOT_SUPPORT      = 100;   
    public static final int KRB_AP_ERR_REQ_OPTIONS = 101; 
    public static final int API_INVALID_ARG               = 400;  

    public static final int BITSTRING_SIZE_INVALID        = 500;  
    public static final int BITSTRING_INDEX_OUT_OF_BOUNDS = 501;  
    public static final int BITSTRING_BAD_LENGTH          = 502;  

    public static final int REALM_ILLCHAR                 = 600;  
    public static final int REALM_NULL                    = 601;  

    public static final int ASN1_BAD_TIMEFORMAT           = 900;  
    public static final int ASN1_MISSING_FIELD            = 901;  
    public static final int ASN1_MISPLACED_FIELD          = 902;  
    public static final int ASN1_TYPE_MISMATCH            = 903;  
    public static final int ASN1_OVERFLOW                 = 904;  
    public static final int ASN1_OVERRUN                  = 905;  
    public static final int ASN1_BAD_ID                   = 906;  
    public static final int ASN1_BAD_LENGTH               = 907;  
    public static final int ASN1_BAD_FORMAT               = 908;  
    public static final int ASN1_PARSE_ERROR              = 909;  
    public static final int ASN1_BAD_CLASS                = 910;  
    public static final int ASN1_BAD_TYPE                 = 911;  
    public static final int ASN1_BAD_TAG                  = 912;  
    public static final int ASN1_UNSUPPORTED_TYPE         = 913;  
    public static final int ASN1_CANNOT_ENCODE            = 914;  

    private static Hashtable<Integer,String> errMsgList;

    public static String getErrorMessage(int i) {
        return errMsgList.get(i);
    }

    public static final boolean DEBUG = GetBooleanAction
            .privilegedGetProperty("sun.security.krb5.debug");

    public static final sun.security.util.HexDumpEncoder hexDumper =
        new sun.security.util.HexDumpEncoder();

    static {
        errMsgList = new Hashtable<Integer,String> ();
        errMsgList.put(KDC_ERR_NONE, "No error");
        errMsgList.put(KDC_ERR_NAME_EXP, "Client's entry in database expired");
        errMsgList.put(KDC_ERR_SERVICE_EXP, "Server's entry in database has expired");
        errMsgList.put(KDC_ERR_BAD_PVNO, "Requested protocol version number not supported");
        errMsgList.put(KDC_ERR_C_OLD_MAST_KVNO, "Client's key encrypted in old master key");
        errMsgList.put(KDC_ERR_S_OLD_MAST_KVNO, "Server's key encrypted in old master key");
        errMsgList.put(KDC_ERR_C_PRINCIPAL_UNKNOWN, "Client not found in Kerberos database");
        errMsgList.put(KDC_ERR_S_PRINCIPAL_UNKNOWN, "Server not found in Kerberos database");
        errMsgList.put(KDC_ERR_PRINCIPAL_NOT_UNIQUE, "Multiple principal entries in database");
        errMsgList.put(KDC_ERR_NULL_KEY, "The client or server has a null key");
        errMsgList.put(KDC_ERR_CANNOT_POSTDATE, "Ticket not eligible for postdating");
        errMsgList.put(KDC_ERR_NEVER_VALID, "Requested start time is later than end time");
        errMsgList.put(KDC_ERR_POLICY, "KDC policy rejects request");
        errMsgList.put(KDC_ERR_BADOPTION, "KDC cannot accommodate requested option");
        errMsgList.put(KDC_ERR_ETYPE_NOSUPP, "KDC has no support for encryption type");
        errMsgList.put(KDC_ERR_SUMTYPE_NOSUPP, "KDC has no support for checksum type");
        errMsgList.put(KDC_ERR_PADATA_TYPE_NOSUPP, "KDC has no support for padata type");
        errMsgList.put(KDC_ERR_TRTYPE_NOSUPP, "KDC has no support for transited type");
        errMsgList.put(KDC_ERR_CLIENT_REVOKED, "Clients credentials have been revoked");
        errMsgList.put(KDC_ERR_SERVICE_REVOKED, "Credentials for server have been revoked");
        errMsgList.put(KDC_ERR_TGT_REVOKED, "TGT has been revoked");
        errMsgList.put(KDC_ERR_CLIENT_NOTYET, "Client not yet valid - try again later");
        errMsgList.put(KDC_ERR_SERVICE_NOTYET, "Server not yet valid - try again later");
        errMsgList.put(KDC_ERR_KEY_EXPIRED, "Password has expired - change password to reset");
        errMsgList.put(KDC_ERR_PREAUTH_FAILED, "Pre-authentication information was invalid");
        errMsgList.put(KDC_ERR_PREAUTH_REQUIRED, "Additional pre-authentication required");
        errMsgList.put(KDC_ERR_SERVER_NOMATCH, "Requested server and ticket don't match");
        errMsgList.put(KDC_ERR_MUST_USE_USER2USER, "Server principal valid for user2user only");
        errMsgList.put(KDC_ERR_PATH_NOT_ACCEPTED, "KDC Policy rejects transited path");
        errMsgList.put(KDC_ERR_SVC_UNAVAILABLE, "A service is not available");
        errMsgList.put(KRB_AP_ERR_BAD_INTEGRITY, "Integrity check on decrypted field failed");
        errMsgList.put(KRB_AP_ERR_TKT_EXPIRED, "Ticket expired");
        errMsgList.put(KRB_AP_ERR_TKT_NYV, "Ticket not yet valid");
        errMsgList.put(KRB_AP_ERR_REPEAT, "Request is a replay");
        errMsgList.put(KRB_AP_ERR_NOT_US, "The ticket isn't for us");
        errMsgList.put(KRB_AP_ERR_BADMATCH, "Ticket and authenticator don't match");
        errMsgList.put(KRB_AP_ERR_SKEW, "Clock skew too great");
        errMsgList.put(KRB_AP_ERR_BADADDR, "Incorrect net address");
        errMsgList.put(KRB_AP_ERR_BADVERSION, "Protocol version mismatch");
        errMsgList.put(KRB_AP_ERR_MSG_TYPE, "Invalid msg type");
        errMsgList.put(KRB_AP_ERR_MODIFIED, "Message stream modified");
        errMsgList.put(KRB_AP_ERR_BADORDER, "Message out of order");
        errMsgList.put(KRB_AP_ERR_BADKEYVER, "Specified version of key is not available");
        errMsgList.put(KRB_AP_ERR_NOKEY, "Service key not available");
        errMsgList.put(KRB_AP_ERR_MUT_FAIL, "Mutual authentication failed");
        errMsgList.put(KRB_AP_ERR_BADDIRECTION, "Incorrect message direction");
        errMsgList.put(KRB_AP_ERR_METHOD, "Alternative authentication method required");
        errMsgList.put(KRB_AP_ERR_BADSEQ, "Incorrect sequence number in message");
        errMsgList.put(KRB_AP_ERR_INAPP_CKSUM, "Inappropriate type of checksum in message");
        errMsgList.put(KRB_AP_PATH_NOT_ACCEPTED, "Policy rejects transited path");
        errMsgList.put(KRB_ERR_RESPONSE_TOO_BIG, "Response too big for UDP, retry with TCP");
        errMsgList.put(KRB_ERR_GENERIC, "Generic error (description in e-text)");
        errMsgList.put(KRB_ERR_FIELD_TOOLONG, "Field is too long for this implementation");
        errMsgList.put(KRB_ERR_WRONG_REALM, "Wrong realm");


        errMsgList.put(API_INVALID_ARG, "Invalid argument");

        errMsgList.put(BITSTRING_SIZE_INVALID, "BitString size does not match input byte array");
        errMsgList.put(BITSTRING_INDEX_OUT_OF_BOUNDS, "BitString bit index does not fall within size");
        errMsgList.put(BITSTRING_BAD_LENGTH, "BitString length is wrong for the expected type");

        errMsgList.put(REALM_ILLCHAR, "Illegal character in realm name; one of: '/', ':', '\0'");
        errMsgList.put(REALM_NULL, "Null realm name");

        errMsgList.put(ASN1_BAD_TIMEFORMAT, "Input not in GeneralizedTime format");
        errMsgList.put(ASN1_MISSING_FIELD, "Structure is missing a required field");
        errMsgList.put(ASN1_MISPLACED_FIELD, "Unexpected field number");
        errMsgList.put(ASN1_TYPE_MISMATCH, "Type numbers are inconsistent");
        errMsgList.put(ASN1_OVERFLOW, "Value too large");
        errMsgList.put(ASN1_OVERRUN, "Encoding ended unexpectedly");
        errMsgList.put(ASN1_BAD_ID, "Identifier doesn't match expected value");
        errMsgList.put(ASN1_BAD_LENGTH, "Length doesn't match expected value");
        errMsgList.put(ASN1_BAD_FORMAT, "Badly-formatted encoding");
        errMsgList.put(ASN1_PARSE_ERROR, "Parse error");
        errMsgList.put(ASN1_BAD_CLASS, "Bad class number");
        errMsgList.put(ASN1_BAD_TYPE, "Bad type number");
        errMsgList.put(ASN1_BAD_TAG, "Bad tag number");
        errMsgList.put(ASN1_UNSUPPORTED_TYPE, "Unsupported ASN.1 type encountered");
        errMsgList.put(ASN1_CANNOT_ENCODE, "Encoding failed due to invalid parameter(s)");
        errMsgList.put(KRB_CRYPTO_NOT_SUPPORT, "Client has no support for crypto type");
        errMsgList.put(KRB_AP_ERR_REQ_OPTIONS, "Invalid option setting in ticket request.");
    }

}
