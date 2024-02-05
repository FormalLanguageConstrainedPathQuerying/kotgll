/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * A Simple ListeningConnector with default arguments of all types used by
 * nsk/jdi/PlugConnectors/MultiConnectors/plugMultiConnect005 test
 */

package nsk.jdi.PlugConnectors.MultiConnectors.plugMultiConnect005.connectors;

import nsk.share.jdi.*;
import com.sun.jdi.*;
import com.sun.jdi.connect.*;
import java.util.*;
import java.util.ArrayList;

public class PlugListenConnector005_02 extends PlugConnectors implements ListeningConnector {

    static String plugListenConnectorName
        = "PlugListenConnector005_02_Name";
    static String plugListenConnectorDescription
        = "PlugListenConnector005_02_Description";
    static Transport plugListenConnectorTransport
        = new PlugConnectorsTransport("PlugListenConnector005_02_Transport");
    static Map<String, Connector.Argument> plugListenConnectorDefaultArguments
        = new HashMap<String, Connector.Argument>();

    static Map<String, Connector.Argument> prepareConnectorDefaultArguments() {
        String plugListenConnectorStringArgumentKey = "PlugListenConnector005_02_StringArgument_Key";
        Connector.StringArgument testStringArgument = new TestStringArgument(
            "PlugListenConnector005_02_StringArgument_Name",
            "PlugListenConnector005_02_StringArgument_Label",
            "PlugListenConnector005_02_StringArgument_Description",
            "PlugListenConnector005_02_StringArgument_Value",
            true  
            );
        plugListenConnectorDefaultArguments.put(plugListenConnectorStringArgumentKey, testStringArgument);

        String plugListenConnectorIntegerArgumentKey = "PlugListenConnector005_02_IntegerArgument_Key";
        Connector.IntegerArgument testIntegerArgument = new TestIntegerArgument(
            "PlugListenConnector005_02_IntegerArgument_Name",
            "PlugListenConnector005_02_IntegerArgument_Label",
            "PlugListenConnector005_02_IntegerArgument_Description",
            555555, 
            111111, 
            999999, 
            true    
            );
        plugListenConnectorDefaultArguments.put(plugListenConnectorIntegerArgumentKey, testIntegerArgument);

        String plugListenConnectorBooleanArgumentKey = "PlugListenConnector005_02_BooleanArgument_Key";
        Connector.BooleanArgument testBooleanArgument = new TestBooleanArgument(
            "PlugListenConnector005_02_BooleanArgument_Name",
            "PlugListenConnector005_02_BooleanArgument_Label",
            "PlugListenConnector005_02_BooleanArgument_Description",
            true, 
            true    
            );
        plugListenConnectorDefaultArguments.put(plugListenConnectorBooleanArgumentKey, testBooleanArgument);

        String plugListenConnectorSelectedArgumentKey = "PlugListenConnector005_02_SelectedArgument_Key";
        List<String> selectedArgumentChoices = new ArrayList<String>();
        selectedArgumentChoices.add("PlugListenConnector005_02_SelectedArgument_Value_0");
        selectedArgumentChoices.add("PlugListenConnector005_02_SelectedArgument_Value");
        selectedArgumentChoices.add("PlugListenConnector005_02_SelectedArgument_Value_1");

        Connector.SelectedArgument testSelectedArgument = new TestSelectedArgument(
            "PlugListenConnector005_02_SelectedArgument_Name",
            "PlugListenConnector005_02_SelectedArgument_Label",
            "PlugListenConnector005_02_SelectedArgument_Description",
            "PlugListenConnector005_02_SelectedArgument_Value",
            selectedArgumentChoices, 
            true    
            );
        plugListenConnectorDefaultArguments.put(plugListenConnectorSelectedArgumentKey, testSelectedArgument);

        return plugListenConnectorDefaultArguments;
    }  


    public PlugListenConnector005_02() {

        super(plugListenConnectorName,
            plugListenConnectorDescription,
            plugListenConnectorTransport,
            prepareConnectorDefaultArguments());
    }

} 
