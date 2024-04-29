/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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
 */

/*
 * @test
 * @bug 4742177 8019834
 * @summary Re-test IPv6 (and specifically MulticastSocket) with latest Linux & USAGI code
 */
import java.net.*;
import java.util.*;


public class BadIPv6Addresses {
    public static void main(String[] args) throws Exception {
        String[] badAddresses = new String[] {
            "0:1:2:3:4:5:6:7:8",        
            "0:1:2:3:4:5:6",            
            "0:1:2:3:4:5:6:x",          
            "0:1:2:3:4:5:6::7",         
            "0:1:2:3:4:5:6:789abcdef",  
            "0:1:2:3::x",               
            "0:1:2:::3",                
            "0:1:2:3::abcde",           
            "0:1",                      
            "0:0:0:0:0:x:10.0.0.1",     
            "0:0:0:0:0:0:10.0.0.x",     
            "0:0:0:0:0::0:10.0.0.1",    
            "0:0:0:0:0:fffff:10.0.0.1", 
            "0:0:0:0:0:0:0:10.0.0.1",   
            "0:0:0:0:0:10.0.0.1",       
            "0:0:0:0:0:0:10.0.0.0.1",   
            "0:0:0:0:0:0:10.0.1",       
            "0:0:0:0:0:0:10..0.0.1",    
            "::fffx:192.168.0.1",       
            "::ffff:192.168.0.x",       
            ":::ffff:192.168.0.1",      
            "::fffff:192.168.0.1",      
            "::ffff:1923.168.0.1",      
            ":ffff:192.168.0.1",        
            "::ffff:192.168.0.1.2",     
            "::ffff:192.168.0",         
            "::ffff:192.168..0.1"       
        };

        List<String> failedAddrs = new ArrayList<String>();
        for (String addrStr : badAddresses) {
            try {
                InetAddress addr = InetAddress.getByName(addrStr);

                failedAddrs.add(addrStr);
            } catch (UnknownHostException e) {
            }
        }

        if (failedAddrs.size() > 0) {
            System.out.println("We should reject following ipv6 addresses, but we didn't:");
            for (String addr : failedAddrs) {
                System.out.println("\t" + addr);
            }
            throw new RuntimeException("Test failed.");
        }
    }
}
