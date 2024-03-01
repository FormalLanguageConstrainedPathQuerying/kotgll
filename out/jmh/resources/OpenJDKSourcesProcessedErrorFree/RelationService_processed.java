/*
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates. All rights reserved.
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

package javax.management.relation;

import static com.sun.jmx.defaults.JmxProperties.RELATION_LOGGER;
import static com.sun.jmx.mbeanserver.Util.cast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.lang.System.Logger.Level;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.MBeanServerDelegate;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * The Relation Service is in charge of creating and deleting relation types
 * and relations, of handling the consistency and of providing query
 * mechanisms.
 * <P>It implements the NotificationBroadcaster by extending
 * NotificationBroadcasterSupport to send notifications when a relation is
 * removed from it.
 * <P>It implements the NotificationListener interface to be able to receive
 * notifications concerning unregistration of MBeans referenced in relation
 * roles and of relation MBeans.
 * <P>It implements the MBeanRegistration interface to be able to retrieve
 * its ObjectName and MBean Server.
 *
 * @since 1.5
 */
public class RelationService extends NotificationBroadcasterSupport
    implements RelationServiceMBean, MBeanRegistration, NotificationListener {


    private Map<String,Object> myRelId2ObjMap = new HashMap<>();

    private Map<String,String> myRelId2RelTypeMap = new HashMap<>();

    private Map<ObjectName,String> myRelMBeanObjName2RelIdMap =
        new HashMap<>();

    private Map<String,RelationType> myRelType2ObjMap =
        new HashMap<>();

    private Map<String,List<String>> myRelType2RelIdsMap =
        new HashMap<>();

    private final Map<ObjectName,Map<String,List<String>>>
        myRefedMBeanObjName2RelIdsMap =
            new HashMap<>();

    private boolean myPurgeFlag = true;

    private final AtomicLong atomicSeqNo = new AtomicLong();

    private ObjectName myObjName = null;

    private MBeanServer myMBeanServer = null;

    private MBeanServerNotificationFilter myUnregNtfFilter = null;

    private List<MBeanServerNotification> myUnregNtfList =
        new ArrayList<>();


    /**
     * Constructor.
     *
     * @param immediatePurgeFlag  flag to indicate when a notification is
     * received for the unregistration of an MBean referenced in a relation, if
     * an immediate "purge" of the relations (look for the relations no
     * longer valid) has to be performed , or if that will be performed only
     * when the purgeRelations method will be explicitly called.
     * <P>true is immediate purge.
     */
    public RelationService(boolean immediatePurgeFlag) {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        setPurgeFlag(immediatePurgeFlag);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Checks if the Relation Service is active.
     * Current condition is that the Relation Service must be registered in the
     * MBean Server
     *
     * @exception RelationServiceNotRegisteredException  if it is not
     * registered
     */
    public void isActive()
        throws RelationServiceNotRegisteredException {
        if (myMBeanServer == null) {
            String excMsg =
                "Relation Service not registered in the MBean Server.";
            throw new RelationServiceNotRegisteredException(excMsg);
        }
        return;
    }


    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name)
        throws Exception {

        myMBeanServer = server;
        myObjName = name;
        return name;
    }

    public void postRegister(Boolean registrationDone) {
        return;
    }

    public void preDeregister()
        throws Exception {
        return;
    }

    public void postDeregister() {
        return;
    }


    /**
     * Returns the flag to indicate if when a notification is received for the
     * unregistration of an MBean referenced in a relation, if an immediate
     * "purge" of the relations (look for the relations no longer valid)
     * has to be performed , or if that will be performed only when the
     * purgeRelations method will be explicitly called.
     * <P>true is immediate purge.
     *
     * @return true if purges are automatic.
     *
     * @see #setPurgeFlag
     */
    public boolean getPurgeFlag() {
        return myPurgeFlag;
    }

    /**
     * Sets the flag to indicate if when a notification is received for the
     * unregistration of an MBean referenced in a relation, if an immediate
     * "purge" of the relations (look for the relations no longer valid)
     * has to be performed , or if that will be performed only when the
     * purgeRelations method will be explicitly called.
     * <P>true is immediate purge.
     *
     * @param purgeFlag  flag
     *
     * @see #getPurgeFlag
     */
    public void setPurgeFlag(boolean purgeFlag) {

        myPurgeFlag = purgeFlag;
        return;
    }


    /**
     * Creates a relation type (a RelationTypeSupport object) with given
     * role infos (provided by the RoleInfo objects), and adds it in the
     * Relation Service.
     *
     * @param relationTypeName  name of the relation type
     * @param roleInfoArray  array of role infos
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception InvalidRelationTypeException  If:
     * <P>- there is already a relation type with that name
     * <P>- the same name has been used for two different role infos
     * <P>- no role info provided
     * <P>- one null role info provided
     */
    public void createRelationType(String relationTypeName,
                                   RoleInfo[] roleInfoArray)
        throws IllegalArgumentException,
               InvalidRelationTypeException {

        if (relationTypeName == null || roleInfoArray == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationTypeName);

        RelationType relType =
            new RelationTypeSupport(relationTypeName, roleInfoArray);

        addRelationTypeInt(relType);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Adds given object as a relation type. The object is expected to
     * implement the RelationType interface.
     *
     * @param relationTypeObj  relation type object (implementing the
     * RelationType interface)
     *
     * @exception IllegalArgumentException  if null parameter or if
     * {@link RelationType#getRelationTypeName
     * relationTypeObj.getRelationTypeName()} returns null.
     * @exception InvalidRelationTypeException  if:
     * <P>- the same name has been used for two different roles
     * <P>- no role info provided
     * <P>- one null role info provided
     * <P>- there is already a relation type with that name
     */
    public void addRelationType(RelationType relationTypeObj)
        throws IllegalArgumentException,
               InvalidRelationTypeException {

        if (relationTypeObj == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        List<RoleInfo> roleInfoList = relationTypeObj.getRoleInfos();
        if (roleInfoList == null) {
            String excMsg = "No role info provided.";
            throw new InvalidRelationTypeException(excMsg);
        }

        RoleInfo[] roleInfoArray = new RoleInfo[roleInfoList.size()];
        int i = 0;
        for (RoleInfo currRoleInfo : roleInfoList) {
            roleInfoArray[i] = currRoleInfo;
            i++;
        }
        RelationTypeSupport.checkRoleInfos(roleInfoArray);

        addRelationTypeInt(relationTypeObj);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
     }

    /**
     * Retrieves names of all known relation types.
     *
     * @return ArrayList of relation type names (Strings)
     */
    public List<String> getAllRelationTypeNames() {
        ArrayList<String> result;
        synchronized(myRelType2ObjMap) {
            result = new ArrayList<>(myRelType2ObjMap.keySet());
        }
        return result;
    }

    /**
     * Retrieves list of role infos (RoleInfo objects) of a given relation
     * type.
     *
     * @param relationTypeName  name of relation type
     *
     * @return ArrayList of RoleInfo.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if there is no relation type
     * with that name.
     */
    public List<RoleInfo> getRoleInfos(String relationTypeName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException {

        if (relationTypeName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationTypeName);

        RelationType relType = getRelationType(relationTypeName);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return relType.getRoleInfos();
    }

    /**
     * Retrieves role info for given role name of a given relation type.
     *
     * @param relationTypeName  name of relation type
     * @param roleInfoName  name of role
     *
     * @return RoleInfo object.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if the relation type is not
     * known in the Relation Service
     * @exception RoleInfoNotFoundException  if the role is not part of the
     * relation type.
     */
    public RoleInfo getRoleInfo(String relationTypeName,
                                String roleInfoName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException,
               RoleInfoNotFoundException {

        if (relationTypeName == null || roleInfoName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}",
                            relationTypeName, roleInfoName);

        RelationType relType = getRelationType(relationTypeName);

        RoleInfo roleInfo = relType.getRoleInfo(roleInfoName);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return roleInfo;
    }

    /**
     * Removes given relation type from Relation Service.
     * <P>The relation objects of that type will be removed from the
     * Relation Service.
     *
     * @param relationTypeName  name of the relation type to be removed
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  If there is no relation type
     * with that name
     */
    public void removeRelationType(String relationTypeName)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationTypeNotFoundException {

        isActive();

        if (relationTypeName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationTypeName);

        RelationType relType = getRelationType(relationTypeName);

        List<String> relIdList = null;
        synchronized(myRelType2RelIdsMap) {
            List<String> relIdList1 =
                myRelType2RelIdsMap.get(relationTypeName);
            if (relIdList1 != null) {
                relIdList = new ArrayList<>(relIdList1);
            }
        }

        synchronized(myRelType2ObjMap) {
            myRelType2ObjMap.remove(relationTypeName);
        }
        synchronized(myRelType2RelIdsMap) {
            myRelType2RelIdsMap.remove(relationTypeName);
        }

        if (relIdList != null) {
            for (String currRelId : relIdList) {
                try {
                    removeRelation(currRelId);
                } catch (RelationNotFoundException exc1) {
                    throw new RuntimeException(exc1.getMessage());
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }


    /**
     * Creates a simple relation (represented by a RelationSupport object) of
     * given relation type, and adds it in the Relation Service.
     * <P>Roles are initialized according to the role list provided in
     * parameter. The ones not initialized in this way are set to an empty
     * ArrayList of ObjectNames.
     * <P>A RelationNotification, with type RELATION_BASIC_CREATION, is sent.
     *
     * @param relationId  relation identifier, to identify uniquely the relation
     * inside the Relation Service
     * @param relationTypeName  name of the relation type (has to be created
     * in the Relation Service)
     * @param roleList  role list to initialize roles of the relation (can
     * be null).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter, except the role
     * list which can be null if no role initialization
     * @exception RoleNotFoundException  if a value is provided for a role
     * that does not exist in the relation type
     * @exception InvalidRelationIdException  if relation id already used
     * @exception RelationTypeNotFoundException  if relation type not known in
     * Relation Service
     * @exception InvalidRoleValueException if:
     * <P>- the same role name is used for two different roles
     * <P>- the number of referenced MBeans in given value is less than
     * expected minimum degree
     * <P>- the number of referenced MBeans in provided value exceeds expected
     * maximum degree
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>- an MBean provided for that role does not exist
     */
    public void createRelation(String relationId,
                               String relationTypeName,
                               RoleList roleList)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RoleNotFoundException,
               InvalidRelationIdException,
               RelationTypeNotFoundException,
               InvalidRoleValueException {

        isActive();

        if (relationId == null ||
            relationTypeName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            relationId, relationTypeName, roleList);

        RelationSupport relObj = new RelationSupport(relationId,
                                               myObjName,
                                               relationTypeName,
                                               roleList);

        addRelationInt(true,
                       relObj,
                       null,
                       relationId,
                       relationTypeName,
                       roleList);
        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Adds an MBean created by the user (and registered by him in the MBean
     * Server) as a relation in the Relation Service.
     * <P>To be added as a relation, the MBean must conform to the
     * following:
     * <P>- implement the Relation interface
     * <P>- have for RelationService ObjectName the ObjectName of current
     * Relation Service
     * <P>- have a relation id unique and unused in current Relation Service
     * <P>- have for relation type a relation type created in the Relation
     * Service
     * <P>- have roles conforming to the role info provided in the relation
     * type.
     *
     * @param relationObjectName  ObjectName of the relation MBean to be added.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception NoSuchMethodException  If the MBean does not implement the
     * Relation interface
     * @exception InvalidRelationIdException  if:
     * <P>- no relation identifier in MBean
     * <P>- the relation identifier is already used in the Relation Service
     * @exception InstanceNotFoundException  if the MBean for given ObjectName
     * has not been registered
     * @exception InvalidRelationServiceException  if:
     * <P>- no Relation Service name in MBean
     * <P>- the Relation Service name in the MBean is not the one of the
     * current Relation Service
     * @exception RelationTypeNotFoundException  if:
     * <P>- no relation type name in MBean
     * <P>- the relation type name in MBean does not correspond to a relation
     * type created in the Relation Service
     * @exception InvalidRoleValueException  if:
     * <P>- the number of referenced MBeans in a role is less than
     * expected minimum degree
     * <P>- the number of referenced MBeans in a role exceeds expected
     * maximum degree
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>- an MBean provided for a role does not exist
     * @exception RoleNotFoundException  if a value is provided for a role
     * that does not exist in the relation type
     */
    public void addRelation(ObjectName relationObjectName)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               NoSuchMethodException,
               InvalidRelationIdException,
               InstanceNotFoundException,
               InvalidRelationServiceException,
               RelationTypeNotFoundException,
               RoleNotFoundException,
               InvalidRoleValueException {

        if (relationObjectName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationObjectName);

        isActive();

        if ((!(myMBeanServer.isInstanceOf(relationObjectName, "javax.management.relation.Relation")))) {
            String excMsg = "This MBean does not implement the Relation interface.";
            throw new NoSuchMethodException(excMsg);
        }
        String relId;
        try {
            relId = (String)(myMBeanServer.getAttribute(relationObjectName,
                                                        "RelationId"));

        } catch (MBeanException exc1) {
            throw new RuntimeException(
                                     (exc1.getTargetException()).getMessage());
        } catch (ReflectionException | AttributeNotFoundException exc2) {
            throw new RuntimeException(exc2.getMessage());
        }

        if (relId == null) {
            String excMsg = "This MBean does not provide a relation id.";
            throw new InvalidRelationIdException(excMsg);
        }
        ObjectName relServObjName;
        try {
            relServObjName = (ObjectName)
                (myMBeanServer.getAttribute(relationObjectName,
                                            "RelationServiceName"));

        } catch (MBeanException exc1) {
            throw new RuntimeException(
                                     (exc1.getTargetException()).getMessage());
        } catch (ReflectionException | AttributeNotFoundException exc2) {
            throw new RuntimeException(exc2.getMessage());
        }

        boolean badRelServFlag = false;
        if (relServObjName == null) {
            badRelServFlag = true;

        } else if (!(relServObjName.equals(myObjName))) {
            badRelServFlag = true;
        }
        if (badRelServFlag) {
            String excMsg = "The Relation Service referenced in the MBean is not the current one.";
            throw new InvalidRelationServiceException(excMsg);
        }
        String relTypeName;
        try {
            relTypeName = (String)(myMBeanServer.getAttribute(relationObjectName,
                                                              "RelationTypeName"));

        } catch (MBeanException exc1) {
            throw new RuntimeException(
                                     (exc1.getTargetException()).getMessage());
        }catch (ReflectionException | AttributeNotFoundException exc2) {
            throw new RuntimeException(exc2.getMessage());
        }
        if (relTypeName == null) {
            String excMsg = "No relation type provided.";
            throw new RelationTypeNotFoundException(excMsg);
        }
        RoleList roleList;
        try {
            roleList = (RoleList)(myMBeanServer.invoke(relationObjectName,
                                                       "retrieveAllRoles",
                                                       null,
                                                       null));
        } catch (MBeanException exc1) {
            throw new RuntimeException(
                                     (exc1.getTargetException()).getMessage());
        } catch (ReflectionException exc2) {
            throw new RuntimeException(exc2.getMessage());
        }

        addRelationInt(false,
                       null,
                       relationObjectName,
                       relId,
                       relTypeName,
                       roleList);
        synchronized(myRelMBeanObjName2RelIdMap) {
            myRelMBeanObjName2RelIdMap.put(relationObjectName, relId);
        }

        try {
            myMBeanServer.setAttribute(relationObjectName,
                                       new Attribute(
                                         "RelationServiceManagementFlag",
                                         Boolean.TRUE));
        } catch (Exception exc) {
        }

        List<ObjectName> newRefList = new ArrayList<>();
        newRefList.add(relationObjectName);
        updateUnregistrationListener(newRefList, null);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * If the relation is represented by an MBean (created by the user and
     * added as a relation in the Relation Service), returns the ObjectName of
     * the MBean.
     *
     * @param relationId  relation id identifying the relation
     *
     * @return ObjectName of the corresponding relation MBean, or null if
     * the relation is not an MBean.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException there is no relation associated
     * to that id
     */
    public ObjectName isRelationMBean(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException{

        if (relationId == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationId);

        Object result = getRelation(relationId);
        if (result instanceof ObjectName) {
            return ((ObjectName)result);
        } else {
            return null;
        }
    }

    /**
     * Returns the relation id associated to the given ObjectName if the
     * MBean has been added as a relation in the Relation Service.
     *
     * @param objectName  ObjectName of supposed relation
     *
     * @return relation id (String) or null (if the ObjectName is not a
     * relation handled by the Relation Service)
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public String isRelation(ObjectName objectName)
        throws IllegalArgumentException {

        if (objectName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", objectName);

        String result = null;
        synchronized(myRelMBeanObjName2RelIdMap) {
            String relId = myRelMBeanObjName2RelIdMap.get(objectName);
            if (relId != null) {
                result = relId;
            }
        }
        return result;
    }

    /**
     * Checks if there is a relation identified in Relation Service with given
     * relation id.
     *
     * @param relationId  relation id identifying the relation
     *
     * @return boolean: true if there is a relation, false else
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public Boolean hasRelation(String relationId)
        throws IllegalArgumentException {

        if (relationId == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationId);

        try {
            Object result = getRelation(relationId);
            return true;
        } catch (RelationNotFoundException exc) {
            return false;
        }
    }

    /**
     * Returns all the relation ids for all the relations handled by the
     * Relation Service.
     *
     * @return ArrayList of String
     */
    public List<String> getAllRelationIds() {
        List<String> result;
        synchronized(myRelId2ObjMap) {
            result = new ArrayList<>(myRelId2ObjMap.keySet());
        }
        return result;
    }

    /**
     * Checks if given Role can be read in a relation of the given type.
     *
     * @param roleName  name of role to be checked
     * @param relationTypeName  name of the relation type
     *
     * @return an Integer wrapping an integer corresponding to possible
     * problems represented as constants in RoleUnresolved:
     * <P>- 0 if role can be read
     * <P>- integer corresponding to RoleStatus.NO_ROLE_WITH_NAME
     * <P>- integer corresponding to RoleStatus.ROLE_NOT_READABLE
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if the relation type is not
     * known in the Relation Service
     */
    public Integer checkRoleReading(String roleName,
                                    String relationTypeName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException {

        if (roleName == null || relationTypeName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}",
                            roleName, relationTypeName);

        Integer result;

        RelationType relType = getRelationType(relationTypeName);

        try {
            RoleInfo roleInfo = relType.getRoleInfo(roleName);

            result =  checkRoleInt(1,
                                   roleName,
                                   null,
                                   roleInfo,
                                   false);

        } catch (RoleInfoNotFoundException exc) {
            result = Integer.valueOf(RoleStatus.NO_ROLE_WITH_NAME);
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Checks if given Role can be set in a relation of given type.
     *
     * @param role  role to be checked
     * @param relationTypeName  name of relation type
     * @param initFlag  flag to specify that the checking is done for the
     * initialization of a role, write access shall not be verified.
     *
     * @return an Integer wrapping an integer corresponding to possible
     * problems represented as constants in RoleUnresolved:
     * <P>- 0 if role can be set
     * <P>- integer corresponding to RoleStatus.NO_ROLE_WITH_NAME
     * <P>- integer for RoleStatus.ROLE_NOT_WRITABLE
     * <P>- integer for RoleStatus.LESS_THAN_MIN_ROLE_DEGREE
     * <P>- integer for RoleStatus.MORE_THAN_MAX_ROLE_DEGREE
     * <P>- integer for RoleStatus.REF_MBEAN_OF_INCORRECT_CLASS
     * <P>- integer for RoleStatus.REF_MBEAN_NOT_REGISTERED
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if unknown relation type
     */
    public Integer checkRoleWriting(Role role,
                                    String relationTypeName,
                                    Boolean initFlag)
        throws IllegalArgumentException,
               RelationTypeNotFoundException {

        if (role == null ||
            relationTypeName == null ||
            initFlag == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            role, relationTypeName, initFlag);

        RelationType relType = getRelationType(relationTypeName);

        String roleName = role.getRoleName();
        List<ObjectName> roleValue = role.getRoleValue();
        boolean writeChkFlag = true;
        if (initFlag.booleanValue()) {
            writeChkFlag = false;
        }

        RoleInfo roleInfo;
        try {
            roleInfo = relType.getRoleInfo(roleName);
        } catch (RoleInfoNotFoundException exc) {
            RELATION_LOGGER.log(Level.TRACE, "RETURN");
            return Integer.valueOf(RoleStatus.NO_ROLE_WITH_NAME);
        }

        Integer result = checkRoleInt(2,
                                      roleName,
                                      roleValue,
                                      roleInfo,
                                      writeChkFlag);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Sends a notification (RelationNotification) for a relation creation.
     * The notification type is:
     * <P>- RelationNotification.RELATION_BASIC_CREATION if the relation is an
     * object internal to the Relation Service
     * <P>- RelationNotification.RELATION_MBEAN_CREATION if the relation is a
     * MBean added as a relation.
     * <P>The source object is the Relation Service itself.
     * <P>It is called in Relation Service createRelation() and
     * addRelation() methods.
     *
     * @param relationId  relation identifier of the updated relation
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if there is no relation for given
     * relation id
     */
    public void sendRelationCreationNotification(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException {

        if (relationId == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationId);

        StringBuilder ntfMsg = new StringBuilder("Creation of relation ");
        ntfMsg.append(relationId);

        sendNotificationInt(1,
                            ntfMsg.toString(),
                            relationId,
                            null,
                            null,
                            null,
                            null);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Sends a notification (RelationNotification) for a role update in the
     * given relation. The notification type is:
     * <P>- RelationNotification.RELATION_BASIC_UPDATE if the relation is an
     * object internal to the Relation Service
     * <P>- RelationNotification.RELATION_MBEAN_UPDATE if the relation is a
     * MBean added as a relation.
     * <P>The source object is the Relation Service itself.
     * <P>It is called in relation MBean setRole() (for given role) and
     * setRoles() (for each role) methods (implementation provided in
     * RelationSupport class).
     * <P>It is also called in Relation Service setRole() (for given role) and
     * setRoles() (for each role) methods.
     *
     * @param relationId  relation identifier of the updated relation
     * @param newRole  new role (name and new value)
     * @param oldValue  old role value (List of ObjectName objects)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if there is no relation for given
     * relation id
     */
    public void sendRoleUpdateNotification(String relationId,
                                           Role newRole,
                                           List<ObjectName> oldValue)
        throws IllegalArgumentException,
               RelationNotFoundException {

        if (relationId == null ||
            newRole == null ||
            oldValue == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        if (!(oldValue instanceof ArrayList<?>))
            oldValue = new ArrayList<>(oldValue);

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            relationId, newRole, oldValue);

        String roleName = newRole.getRoleName();
        List<ObjectName> newRoleVal = newRole.getRoleValue();

        String newRoleValString = Role.roleValueToString(newRoleVal);
        String oldRoleValString = Role.roleValueToString(oldValue);
        StringBuilder ntfMsg = new StringBuilder("Value of role ");
        ntfMsg.append(roleName);
        ntfMsg.append(" has changed\nOld value:\n");
        ntfMsg.append(oldRoleValString);
        ntfMsg.append("\nNew value:\n");
        ntfMsg.append(newRoleValString);

        sendNotificationInt(2,
                            ntfMsg.toString(),
                            relationId,
                            null,
                            roleName,
                            newRoleVal,
                            oldValue);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
    }

    /**
     * Sends a notification (RelationNotification) for a relation removal.
     * The notification type is:
     * <P>- RelationNotification.RELATION_BASIC_REMOVAL if the relation is an
     * object internal to the Relation Service
     * <P>- RelationNotification.RELATION_MBEAN_REMOVAL if the relation is a
     * MBean added as a relation.
     * <P>The source object is the Relation Service itself.
     * <P>It is called in Relation Service removeRelation() method.
     *
     * @param relationId  relation identifier of the updated relation
     * @param unregMBeanList  List of ObjectNames of MBeans expected
     * to be unregistered due to relation removal (can be null)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if there is no relation for given
     * relation id
     */
    public void sendRelationRemovalNotification(String relationId,
                                                List<ObjectName> unregMBeanList)
        throws IllegalArgumentException,
               RelationNotFoundException {

        if (relationId == null) {
            String excMsg = "Invalid parameter";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}",
                            relationId, unregMBeanList);

        sendNotificationInt(3,
                            "Removal of relation " + relationId,
                            relationId,
                            unregMBeanList,
                            null,
                            null,
                            null);


        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Handles update of the Relation Service role map for the update of given
     * role in given relation.
     * <P>It is called in relation MBean setRole() (for given role) and
     * setRoles() (for each role) methods (implementation provided in
     * RelationSupport class).
     * <P>It is also called in Relation Service setRole() (for given role) and
     * setRoles() (for each role) methods.
     * <P>To allow the Relation Service to maintain the consistency (in case
     * of MBean unregistration) and to be able to perform queries, this method
     * must be called when a role is updated.
     *
     * @param relationId  relation identifier of the updated relation
     * @param newRole  new role (name and new value)
     * @param oldValue  old role value (List of ObjectName objects)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationNotFoundException  if no relation for given id.
     */
    public void updateRoleMap(String relationId,
                              Role newRole,
                              List<ObjectName> oldValue)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException {

        if (relationId == null ||
            newRole == null ||
            oldValue == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            relationId, newRole, oldValue);

        isActive();

        Object result = getRelation(relationId);

        String roleName = newRole.getRoleName();
        List<ObjectName> newRoleValue = newRole.getRoleValue();
        List<ObjectName> oldRoleValue = new ArrayList<>(oldValue);

        List<ObjectName> newRefList = new ArrayList<>();

        for (ObjectName currObjName : newRoleValue) {

            int currObjNamePos = oldRoleValue.indexOf(currObjName);

            if (currObjNamePos == -1) {

                boolean isNewFlag = addNewMBeanReference(currObjName,
                                                        relationId,
                                                        roleName);

                if (isNewFlag) {
                    newRefList.add(currObjName);
                }

            } else {

                oldRoleValue.remove(currObjNamePos);
            }
        }

        List<ObjectName> obsRefList = new ArrayList<>();

        for (ObjectName currObjName : oldRoleValue) {
            boolean noLongerRefFlag = removeMBeanReference(currObjName,
                                                          relationId,
                                                          roleName,
                                                          false);

            if (noLongerRefFlag) {
                obsRefList.add(currObjName);
            }
        }

        updateUnregistrationListener(newRefList, obsRefList);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Removes given relation from the Relation Service.
     * <P>A RelationNotification notification is sent, its type being:
     * <P>- RelationNotification.RELATION_BASIC_REMOVAL if the relation was
     * only internal to the Relation Service
     * <P>- RelationNotification.RELATION_MBEAN_REMOVAL if the relation is
     * registered as an MBean.
     * <P>For MBeans referenced in such relation, nothing will be done,
     *
     * @param relationId  relation id of the relation to be removed
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation corresponding to
     * given relation id
     */
    public void removeRelation(String relationId)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException {

        isActive();

        if (relationId == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationId);

        Object result = getRelation(relationId);

        if (result instanceof ObjectName) {
            List<ObjectName> obsRefList = new ArrayList<>();
            obsRefList.add((ObjectName)result);
            updateUnregistrationListener(null, obsRefList);
        }



        sendRelationRemovalNotification(relationId, null);


        List<ObjectName> refMBeanList = new ArrayList<>();
        List<ObjectName> nonRefObjNameList = new ArrayList<>();

        synchronized(myRefedMBeanObjName2RelIdsMap) {

            for (ObjectName currRefObjName :
                     myRefedMBeanObjName2RelIdsMap.keySet()) {

                Map<String,List<String>> relIdMap =
                    myRefedMBeanObjName2RelIdsMap.get(currRefObjName);

                if (relIdMap.containsKey(relationId)) {
                    relIdMap.remove(relationId);
                    refMBeanList.add(currRefObjName);
                }

                if (relIdMap.isEmpty()) {
                    nonRefObjNameList.add(currRefObjName);
                }
            }

            for (ObjectName currRefObjName : nonRefObjNameList) {
                myRefedMBeanObjName2RelIdsMap.remove(currRefObjName);
            }
        }

        synchronized(myRelId2ObjMap) {
            myRelId2ObjMap.remove(relationId);
        }

        if (result instanceof ObjectName) {
            synchronized(myRelMBeanObjName2RelIdMap) {
                myRelMBeanObjName2RelIdMap.remove((ObjectName)result);
            }
        }

        String relTypeName;
        synchronized(myRelId2RelTypeMap) {
            relTypeName = myRelId2RelTypeMap.get(relationId);
            myRelId2RelTypeMap.remove(relationId);
        }
        synchronized(myRelType2RelIdsMap) {
            List<String> relIdList = myRelType2RelIdsMap.get(relTypeName);
            if (relIdList != null) {
                relIdList.remove(relationId);
                if (relIdList.isEmpty()) {
                    myRelType2RelIdsMap.remove(relTypeName);
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Purges the relations.
     *
     * <P>Depending on the purgeFlag value, this method is either called
     * automatically when a notification is received for the unregistration of
     * an MBean referenced in a relation (if the flag is set to true), or not
     * (if the flag is set to false).
     * <P>In that case it is up to the user to call it to maintain the
     * consistency of the relations. To be kept in mind that if an MBean is
     * unregistered and the purge not done immediately, if the ObjectName is
     * reused and assigned to another MBean referenced in a relation, calling
     * manually this purgeRelations() method will cause trouble, as will
     * consider the ObjectName as corresponding to the unregistered MBean, not
     * seeing the new one.
     *
     * <P>The behavior depends on the cardinality of the role where the
     * unregistered MBean is referenced:
     * <P>- if removing one MBean reference in the role makes its number of
     * references less than the minimum degree, the relation has to be removed.
     * <P>- if the remaining number of references after removing the MBean
     * reference is still in the cardinality range, keep the relation and
     * update it calling its handleMBeanUnregistration() callback.
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server.
     */
    public void purgeRelations()
        throws RelationServiceNotRegisteredException {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        isActive();



        List<MBeanServerNotification> localUnregNtfList;
        synchronized(myRefedMBeanObjName2RelIdsMap) {
            localUnregNtfList = new ArrayList<>(myUnregNtfList);
            myUnregNtfList = new ArrayList<>();
        }


        List<ObjectName> obsRefList = new ArrayList<>();
        Map<ObjectName,Map<String,List<String>>> localMBean2RelIdMap =
            new HashMap<>();

        synchronized(myRefedMBeanObjName2RelIdsMap) {
            for (MBeanServerNotification currNtf : localUnregNtfList) {

                ObjectName unregMBeanName = currNtf.getMBeanName();

                obsRefList.add(unregMBeanName);

                Map<String,List<String>> relIdMap =
                    myRefedMBeanObjName2RelIdsMap.get(unregMBeanName);
                localMBean2RelIdMap.put(unregMBeanName, relIdMap);

                myRefedMBeanObjName2RelIdsMap.remove(unregMBeanName);
            }
        }

        updateUnregistrationListener(null, obsRefList);

        for (MBeanServerNotification currNtf : localUnregNtfList) {

            ObjectName unregMBeanName = currNtf.getMBeanName();

            Map<String,List<String>> localRelIdMap =
                    localMBean2RelIdMap.get(unregMBeanName);

            for (Map.Entry<String,List<String>> currRel :
                        localRelIdMap.entrySet()) {
                final String currRelId = currRel.getKey();
                List<String> localRoleNameList = currRel.getValue();

                try {
                    handleReferenceUnregistration(currRelId,
                                                  unregMBeanName,
                                                  localRoleNameList);
                } catch (RelationNotFoundException | RoleNotFoundException exc) {
                    throw new RuntimeException(exc.getMessage());
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Retrieves the relations where a given MBean is referenced.
     * <P>This corresponds to the CIM "References" and "ReferenceNames"
     * operations.
     *
     * @param mbeanName  ObjectName of MBean
     * @param relationTypeName  can be null; if specified, only the relations
     * of that type will be considered in the search. Else all relation types
     * are considered.
     * @param roleName  can be null; if specified, only the relations
     * where the MBean is referenced in that role will be returned. Else all
     * roles are considered.
     *
     * @return an HashMap, where the keys are the relation ids of the relations
     * where the MBean is referenced, and the value is, for each key,
     * an ArrayList of role names (as an MBean can be referenced in several
     * roles in the same relation).
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public Map<String,List<String>>
        findReferencingRelations(ObjectName mbeanName,
                                 String relationTypeName,
                                 String roleName)
            throws IllegalArgumentException {

        if (mbeanName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            mbeanName, relationTypeName, roleName);

        Map<String,List<String>> result = new HashMap<>();

        synchronized(myRefedMBeanObjName2RelIdsMap) {

            Map<String,List<String>> relId2RoleNamesMap =
                myRefedMBeanObjName2RelIdsMap.get(mbeanName);

            if (relId2RoleNamesMap != null) {

                Set<String> allRelIdSet = relId2RoleNamesMap.keySet();

                List<String> relIdList;
                if (relationTypeName == null) {
                    relIdList = new ArrayList<>(allRelIdSet);

                } else {

                    relIdList = new ArrayList<>();

                    for (String currRelId : allRelIdSet) {

                        String currRelTypeName;
                        synchronized(myRelId2RelTypeMap) {
                            currRelTypeName =
                                myRelId2RelTypeMap.get(currRelId);
                        }

                        if (currRelTypeName.equals(relationTypeName)) {

                            relIdList.add(currRelId);

                        }
                    }
                }


                for (String currRelId : relIdList) {
                    List<String> currRoleNameList =
                        relId2RoleNamesMap.get(currRelId);

                    if (roleName == null) {
                        result.put(currRelId,
                                new ArrayList<>(currRoleNameList));

                    }  else if (currRoleNameList.contains(roleName)) {
                        List<String> dummyList = new ArrayList<>();
                        dummyList.add(roleName);
                        result.put(currRelId, dummyList);
                    }
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Retrieves the MBeans associated to given one in a relation.
     * <P>This corresponds to CIM Associators and AssociatorNames operations.
     *
     * @param mbeanName  ObjectName of MBean
     * @param relationTypeName  can be null; if specified, only the relations
     * of that type will be considered in the search. Else all
     * relation types are considered.
     * @param roleName  can be null; if specified, only the relations
     * where the MBean is referenced in that role will be considered. Else all
     * roles are considered.
     *
     * @return an HashMap, where the keys are the ObjectNames of the MBeans
     * associated to given MBean, and the value is, for each key, an ArrayList
     * of the relation ids of the relations where the key MBean is
     * associated to given one (as they can be associated in several different
     * relations).
     *
     * @exception IllegalArgumentException  if null parameter
     */
    public Map<ObjectName,List<String>>
        findAssociatedMBeans(ObjectName mbeanName,
                             String relationTypeName,
                             String roleName)
            throws IllegalArgumentException {

        if (mbeanName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            mbeanName, relationTypeName, roleName);

        Map<String,List<String>> relId2RoleNamesMap =
            findReferencingRelations(mbeanName,
                                     relationTypeName,
                                     roleName);

        Map<ObjectName,List<String>> result = new HashMap<>();

        for (String currRelId : relId2RoleNamesMap.keySet()) {

            Map<ObjectName,List<String>> objName2RoleNamesMap;
            try {
                objName2RoleNamesMap = getReferencedMBeans(currRelId);
            } catch (RelationNotFoundException exc) {
                throw new RuntimeException(exc.getMessage());
            }

            for (ObjectName currObjName : objName2RoleNamesMap.keySet()) {

                if (!(currObjName.equals(mbeanName))) {

                    List<String> currRelIdList = result.get(currObjName);
                    if (currRelIdList == null) {

                        currRelIdList = new ArrayList<>();
                        currRelIdList.add(currRelId);
                        result.put(currObjName, currRelIdList);

                    } else {
                        currRelIdList.add(currRelId);
                    }
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns the relation ids for relations of the given type.
     *
     * @param relationTypeName  relation type name
     *
     * @return an ArrayList of relation ids.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationTypeNotFoundException  if there is no relation type
     * with that name.
     */
    public List<String> findRelationsOfType(String relationTypeName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException {

        if (relationTypeName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        RelationType relType = getRelationType(relationTypeName);

        List<String> result;
        synchronized(myRelType2RelIdsMap) {
            List<String> result1 = myRelType2RelIdsMap.get(relationTypeName);
            if (result1 == null)
                result = new ArrayList<>();
            else
                result = new ArrayList<>(result1);
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Retrieves role value for given role name in given relation.
     *
     * @param relationId  relation id
     * @param roleName  name of role
     *
     * @return the ArrayList of ObjectName objects being the role value
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     * @exception RoleNotFoundException  if:
     * <P>- there is no role with given name
     * <P>or
     * <P>- the role is not readable.
     *
     * @see #setRole
     */
    public List<ObjectName> getRole(String relationId,
                                    String roleName)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException,
               RoleNotFoundException {

        if (relationId == null || roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}",
                            relationId, roleName);

        isActive();

        Object relObj = getRelation(relationId);

        List<ObjectName> result;

        if (relObj instanceof RelationSupport) {
            result = cast(
                ((RelationSupport)relObj).getRoleInt(roleName,
                                                     true,
                                                     this,
                                                     false));

        } else {
            Object[] params = new Object[1];
            params[0] = roleName;
            String[] signature = new String[1];
            signature[0] = "java.lang.String";
            try {
                List<ObjectName> invokeResult = cast(
                    myMBeanServer.invoke(((ObjectName)relObj),
                                         "getRole",
                                         params,
                                         signature));
                if (invokeResult == null || invokeResult instanceof ArrayList<?>)
                    result = invokeResult;
                else
                    result = new ArrayList<>(invokeResult);
            } catch (InstanceNotFoundException | ReflectionException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (MBeanException exc2) {
                Exception wrappedExc = exc2.getTargetException();
                if (wrappedExc instanceof RoleNotFoundException) {
                    throw ((RoleNotFoundException)wrappedExc);
                } else {
                    throw new RuntimeException(wrappedExc.getMessage());
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Retrieves values of roles with given names in given relation.
     *
     * @param relationId  relation id
     * @param roleNameArray  array of names of roles to be retrieved
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully retrieved) and a RoleUnresolvedList (for roles not
     * retrieved).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     *
     * @see #setRoles
     */
    public RoleResult getRoles(String relationId,
                               String[] roleNameArray)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException {

        if (relationId == null || roleNameArray == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationId);

        isActive();

        Object relObj = getRelation(relationId);

        RoleResult result;

        if (relObj instanceof RelationSupport) {
            result = ((RelationSupport)relObj).getRolesInt(roleNameArray,
                                                        true,
                                                        this);
        } else {
            Object[] params = new Object[1];
            params[0] = roleNameArray;
            String[] signature = new String[1];
            try {
                signature[0] = (roleNameArray.getClass()).getName();
            } catch (Exception exc) {
            }
            try {
                result = (RoleResult)
                    (myMBeanServer.invoke(((ObjectName)relObj),
                                          "getRoles",
                                          params,
                                          signature));
            } catch (InstanceNotFoundException | ReflectionException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (MBeanException exc2) {
                throw new
                    RuntimeException((exc2.getTargetException()).getMessage());
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns all roles present in the relation.
     *
     * @param relationId  relation id
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully retrieved) and a RoleUnresolvedList (for roles not
     * readable).
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation for given id
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     */
    public RoleResult getAllRoles(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException,
               RelationServiceNotRegisteredException {

        if (relationId == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationId);

        Object relObj = getRelation(relationId);

        RoleResult result;

        if (relObj instanceof RelationSupport) {
            result = ((RelationSupport)relObj).getAllRolesInt(true, this);

        } else {
            try {
                result = (RoleResult)
                    (myMBeanServer.getAttribute(((ObjectName)relObj),
                                                "AllRoles"));
            } catch (Exception exc) {
                throw new RuntimeException(exc.getMessage());
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Retrieves the number of MBeans currently referenced in the given role.
     *
     * @param relationId  relation id
     * @param roleName  name of role
     *
     * @return the number of currently referenced MBeans in that role
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     * @exception RoleNotFoundException  if there is no role with given name
     */
    public Integer getRoleCardinality(String relationId,
                                      String roleName)
        throws IllegalArgumentException,
               RelationNotFoundException,
               RoleNotFoundException {

        if (relationId == null || roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}",
                            relationId, roleName);

        Object relObj = getRelation(relationId);

        Integer result;

        if (relObj instanceof RelationSupport) {
            result = ((RelationSupport)relObj).getRoleCardinality(roleName);

        } else {
            Object[] params = new Object[1];
            params[0] = roleName;
            String[] signature = new String[1];
            signature[0] = "java.lang.String";
            try {
                result = (Integer)
                    (myMBeanServer.invoke(((ObjectName)relObj),
                                          "getRoleCardinality",
                                          params,
                                          signature));
            } catch (InstanceNotFoundException | ReflectionException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (MBeanException exc2) {
                Exception wrappedExc = exc2.getTargetException();
                if (wrappedExc instanceof RoleNotFoundException) {
                    throw ((RoleNotFoundException)wrappedExc);
                } else {
                    throw new RuntimeException(wrappedExc.getMessage());
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Sets the given role in given relation.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>The Relation Service will keep track of the change to keep the
     * consistency of relations by handling referenced MBean deregistrations.
     *
     * @param relationId  relation id
     * @param role  role to be set (name and new value)
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     * @exception RoleNotFoundException  if the role does not exist or is not
     * writable
     * @exception InvalidRoleValueException  if value provided for role is not
     * valid:
     * <P>- the number of referenced MBeans in given value is less than
     * expected minimum degree
     * <P>or
     * <P>- the number of referenced MBeans in provided value exceeds expected
     * maximum degree
     * <P>or
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>or
     * <P>- an MBean provided for that role does not exist
     *
     * @see #getRole
     */
    public void setRole(String relationId,
                        Role role)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException,
               RoleNotFoundException,
               InvalidRoleValueException {

        if (relationId == null || role == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}",
                            relationId, role);

        isActive();

        Object relObj = getRelation(relationId);

        if (relObj instanceof RelationSupport) {
            try {
                ((RelationSupport)relObj).setRoleInt(role,
                                                  true,
                                                  this,
                                                  false);

            } catch (RelationTypeNotFoundException exc) {
                throw new RuntimeException(exc.getMessage());
            }

        } else {
            Object[] params = new Object[1];
            params[0] = role;
            String[] signature = new String[1];
            signature[0] = "javax.management.relation.Role";
            try {
                myMBeanServer.setAttribute(((ObjectName)relObj),
                                           new Attribute("Role", role));

            } catch (InstanceNotFoundException | ReflectionException |
                    AttributeNotFoundException | InvalidAttributeValueException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (MBeanException exc2) {
                Exception wrappedExc = exc2.getTargetException();
                if (wrappedExc instanceof RoleNotFoundException) {
                    throw ((RoleNotFoundException)wrappedExc);
                } else if (wrappedExc instanceof InvalidRoleValueException) {
                    throw ((InvalidRoleValueException)wrappedExc);
                } else {
                    throw new RuntimeException(wrappedExc.getMessage());

                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Sets the given roles in given relation.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>The Relation Service keeps track of the changes to keep the
     * consistency of relations by handling referenced MBean deregistrations.
     *
     * @param relationId  relation id
     * @param roleList  list of roles to be set
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully set) and a RoleUnresolvedList (for roles not
     * set).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation with given id
     *
     * @see #getRoles
     */
    public RoleResult setRoles(String relationId,
                               RoleList roleList)
        throws RelationServiceNotRegisteredException,
               IllegalArgumentException,
               RelationNotFoundException {

        if (relationId == null || roleList == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}",
                            relationId, roleList);

        isActive();

        Object relObj = getRelation(relationId);

        RoleResult result;

        if (relObj instanceof RelationSupport) {
            try {
                result = ((RelationSupport)relObj).setRolesInt(roleList,
                                                            true,
                                                            this);
            } catch (RelationTypeNotFoundException exc) {
                throw new RuntimeException(exc.getMessage());
            }

        } else {
            Object[] params = new Object[1];
            params[0] = roleList;
            String[] signature = new String[1];
            signature[0] = "javax.management.relation.RoleList";
            try {
                result = (RoleResult)
                    (myMBeanServer.invoke(((ObjectName)relObj),
                                          "setRoles",
                                          params,
                                          signature));
            } catch (InstanceNotFoundException | ReflectionException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (MBeanException exc2) {
                throw new
                    RuntimeException((exc2.getTargetException()).getMessage());
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Retrieves MBeans referenced in the various roles of the relation.
     *
     * @param relationId  relation id
     *
     * @return a HashMap mapping:
     * <P> ObjectName {@literal ->} ArrayList of String (role names)
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation for given
     * relation id
     */
    public Map<ObjectName,List<String>>
        getReferencedMBeans(String relationId)
            throws IllegalArgumentException,
        RelationNotFoundException {

        if (relationId == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}",
                            relationId);

        Object relObj = getRelation(relationId);

        Map<ObjectName,List<String>> result;

        if (relObj instanceof RelationSupport) {
            result = ((RelationSupport)relObj).getReferencedMBeans();

        } else {
            try {
                result = cast(
                    myMBeanServer.getAttribute(((ObjectName)relObj),
                                               "ReferencedMBeans"));
            } catch (Exception exc) {
                throw new RuntimeException(exc.getMessage());
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns name of associated relation type for given relation.
     *
     * @param relationId  relation id
     *
     * @return the name of the associated relation type.
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RelationNotFoundException  if no relation for given
     * relation id
     */
    public String getRelationTypeName(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException {

        if (relationId == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationId);

        Object relObj = getRelation(relationId);

        String result;

        if (relObj instanceof RelationSupport) {
            result = ((RelationSupport)relObj).getRelationTypeName();

        } else {
            try {
                result = (String)
                    (myMBeanServer.getAttribute(((ObjectName)relObj),
                                                "RelationTypeName"));
            } catch (Exception exc) {
                throw new RuntimeException(exc.getMessage());
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }


    /**
     * Invoked when a JMX notification occurs.
     * Currently handles notifications for unregistration of MBeans, either
     * referenced in a relation role or being a relation itself.
     *
     * @param notif  The notification.
     * @param handback  An opaque object which helps the listener to
     * associate information regarding the MBean emitter (can be null).
     */
    public void handleNotification(Notification notif,
                                   Object handback) {

        if (notif == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", notif);

        if (notif instanceof MBeanServerNotification) {

            MBeanServerNotification mbsNtf = (MBeanServerNotification) notif;
            String ntfType = notif.getType();

            if (ntfType.equals(
                       MBeanServerNotification.UNREGISTRATION_NOTIFICATION )) {
                ObjectName mbeanName =
                    ((MBeanServerNotification)notif).getMBeanName();

                boolean isRefedMBeanFlag = false;
                synchronized(myRefedMBeanObjName2RelIdsMap) {

                    if (myRefedMBeanObjName2RelIdsMap.containsKey(mbeanName)) {
                        synchronized(myUnregNtfList) {
                            myUnregNtfList.add(mbsNtf);
                        }
                        isRefedMBeanFlag = true;
                    }
                    if (isRefedMBeanFlag && myPurgeFlag) {
                        try {
                            purgeRelations();
                        } catch (Exception exc) {
                            throw new RuntimeException(exc.getMessage());
                        }
                    }
                }

                String relId;
                synchronized(myRelMBeanObjName2RelIdMap){
                    relId = myRelMBeanObjName2RelIdMap.get(mbeanName);
                }
                if (relId != null) {
                    try {
                        removeRelation(relId);
                    } catch (Exception exc) {
                        throw new RuntimeException(exc.getMessage());
                    }
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }


    /**
     * Returns a NotificationInfo object containing the name of the Java class
     * of the notification and the notification types sent.
     */
    public MBeanNotificationInfo[] getNotificationInfo() {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        String ntfClass = "javax.management.relation.RelationNotification";

        String[] ntfTypes = new String[] {
            RelationNotification.RELATION_BASIC_CREATION,
            RelationNotification.RELATION_MBEAN_CREATION,
            RelationNotification.RELATION_BASIC_UPDATE,
            RelationNotification.RELATION_MBEAN_UPDATE,
            RelationNotification.RELATION_BASIC_REMOVAL,
            RelationNotification.RELATION_MBEAN_REMOVAL,
        };

        String ntfDesc = "Sent when a relation is created, updated or deleted.";

        MBeanNotificationInfo ntfInfo =
            new MBeanNotificationInfo(ntfTypes, ntfClass, ntfDesc);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return new MBeanNotificationInfo[] {ntfInfo};
    }


    private void addRelationTypeInt(RelationType relationTypeObj)
        throws IllegalArgumentException,
               InvalidRelationTypeException {

        if (relationTypeObj == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        String relTypeName = relationTypeObj.getRelationTypeName();

        try {
            RelationType relType = getRelationType(relTypeName);

            if (relType != null) {
                String excMsg = "There is already a relation type in the Relation Service with name ";
                StringBuilder excMsgStrB = new StringBuilder(excMsg);
                excMsgStrB.append(relTypeName);
                throw new InvalidRelationTypeException(excMsgStrB.toString());
            }

        } catch (RelationTypeNotFoundException exc) {
        }

        synchronized(myRelType2ObjMap) {
            myRelType2ObjMap.put(relTypeName, relationTypeObj);
        }

        if (relationTypeObj instanceof RelationTypeSupport) {
            ((RelationTypeSupport)relationTypeObj).setRelationServiceFlag(true);
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
     }

    RelationType getRelationType(String relationTypeName)
        throws IllegalArgumentException,
               RelationTypeNotFoundException {

        if (relationTypeName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationTypeName);

        RelationType relType;
        synchronized(myRelType2ObjMap) {
            relType = (myRelType2ObjMap.get(relationTypeName));
        }

        if (relType == null) {
            String excMsg = "No relation type created in the Relation Service with the name ";
            StringBuilder excMsgStrB = new StringBuilder(excMsg);
            excMsgStrB.append(relationTypeName);
            throw new RelationTypeNotFoundException(excMsgStrB.toString());
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return relType;
    }

    Object getRelation(String relationId)
        throws IllegalArgumentException,
               RelationNotFoundException {

        if (relationId == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", relationId);

        Object rel;
        synchronized(myRelId2ObjMap) {
            rel = myRelId2ObjMap.get(relationId);
        }

        if (rel == null) {
            String excMsg = "No relation associated to relation id " + relationId;
            throw new RelationNotFoundException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return rel;
    }

    private boolean addNewMBeanReference(ObjectName objectName,
                                         String relationId,
                                         String roleName)
        throws IllegalArgumentException {

        if (objectName == null ||
            relationId == null ||
            roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            objectName, relationId, roleName);

        boolean isNewFlag = false;

        synchronized(myRefedMBeanObjName2RelIdsMap) {

            Map<String,List<String>> mbeanRefMap =
                myRefedMBeanObjName2RelIdsMap.get(objectName);

            if (mbeanRefMap == null) {

                isNewFlag = true;

                List<String> roleNames = new ArrayList<>();
                roleNames.add(roleName);

                mbeanRefMap = new HashMap<>();
                mbeanRefMap.put(relationId, roleNames);

                myRefedMBeanObjName2RelIdsMap.put(objectName, mbeanRefMap);

            } else {
                List<String> roleNames = mbeanRefMap.get(relationId);

                if (roleNames == null) {

                    roleNames = new ArrayList<>();
                    roleNames.add(roleName);

                    mbeanRefMap.put(relationId, roleNames);

                } else {
                    roleNames.add(roleName);
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return isNewFlag;
    }

    private boolean removeMBeanReference(ObjectName objectName,
                                         String relationId,
                                         String roleName,
                                         boolean allRolesFlag)
        throws IllegalArgumentException {

        if (objectName == null ||
            relationId == null ||
            roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            objectName, relationId, roleName, allRolesFlag);

        boolean noLongerRefFlag = false;

        synchronized(myRefedMBeanObjName2RelIdsMap) {

            Map<String,List<String>> mbeanRefMap =
                (myRefedMBeanObjName2RelIdsMap.get(objectName));

            if (mbeanRefMap == null) {
                RELATION_LOGGER.log(Level.TRACE, "RETURN");
                return true;
            }

            List<String> roleNames = null;
            if (!allRolesFlag) {
                roleNames = mbeanRefMap.get(relationId);

                int obsRefIdx = roleNames.indexOf(roleName);
                if (obsRefIdx != -1) {
                    roleNames.remove(obsRefIdx);
                }
            }

            if (roleNames.isEmpty() || allRolesFlag) {
                mbeanRefMap.remove(relationId);
            }

            if (mbeanRefMap.isEmpty()) {
                myRefedMBeanObjName2RelIdsMap.remove(objectName);
                noLongerRefFlag = true;
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return noLongerRefFlag;
    }

    private void updateUnregistrationListener(List<ObjectName> newRefList,
                                              List<ObjectName> obsoleteRefList)
        throws RelationServiceNotRegisteredException {

        if (newRefList != null && obsoleteRefList != null) {
            if (newRefList.isEmpty() && obsoleteRefList.isEmpty()) {
                return;
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}",
                            newRefList, obsoleteRefList);

        isActive();

        if (newRefList != null || obsoleteRefList != null) {

            boolean newListenerFlag = false;
            if (myUnregNtfFilter == null) {
                myUnregNtfFilter = new MBeanServerNotificationFilter();
                newListenerFlag = true;
            }

            synchronized(myUnregNtfFilter) {

                if (newRefList != null) {
                    for (ObjectName newObjName : newRefList)
                        myUnregNtfFilter.enableObjectName(newObjName);
                }

                if (obsoleteRefList != null) {
                    for (ObjectName obsObjName : obsoleteRefList)
                        myUnregNtfFilter.disableObjectName(obsObjName);
                }

                if (newListenerFlag) {
                    try {
                        myMBeanServer.addNotificationListener(
                                MBeanServerDelegate.DELEGATE_NAME,
                                this,
                                myUnregNtfFilter,
                                null);
                    } catch (InstanceNotFoundException exc) {
                        throw new
                       RelationServiceNotRegisteredException(exc.getMessage());
                    }
                }



            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    private void addRelationInt(boolean relationBaseFlag,
                                RelationSupport relationObj,
                                ObjectName relationObjName,
                                String relationId,
                                String relationTypeName,
                                RoleList roleList)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RoleNotFoundException,
               InvalidRelationIdException,
               RelationTypeNotFoundException,
               InvalidRoleValueException {

        if (relationId == null ||
            relationTypeName == null ||
            (relationBaseFlag &&
             (relationObj == null ||
              relationObjName != null)) ||
            (!relationBaseFlag &&
             (relationObjName == null ||
              relationObj != null))) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE,
                            "ENTRY {0} {1} {2} {3} {4} {5}",
                            relationBaseFlag, relationObj, relationObjName,
                            relationId, relationTypeName, roleList);

        isActive();

        try {
            Object rel = getRelation(relationId);

            if (rel != null) {
                String excMsg = "There is already a relation with id ";
                StringBuilder excMsgStrB = new StringBuilder(excMsg);
                excMsgStrB.append(relationId);
                throw new InvalidRelationIdException(excMsgStrB.toString());
            }
        } catch (RelationNotFoundException exc) {
        }

        RelationType relType = getRelationType(relationTypeName);

        List<RoleInfo> roleInfoList = new ArrayList<>(relType.getRoleInfos());

        if (roleList != null) {

            for (Role currRole : roleList.asList()) {
                String currRoleName = currRole.getRoleName();
                List<ObjectName> currRoleValue = currRole.getRoleValue();
                RoleInfo roleInfo;
                try {
                    roleInfo = relType.getRoleInfo(currRoleName);
                } catch (RoleInfoNotFoundException exc) {
                    throw new RoleNotFoundException(exc.getMessage());
                }

                Integer status = checkRoleInt(2,
                                              currRoleName,
                                              currRoleValue,
                                              roleInfo,
                                              false);
                int pbType = status.intValue();
                if (pbType != 0) {
                    throwRoleProblemException(pbType, currRoleName);
                }

                int roleInfoIdx = roleInfoList.indexOf(roleInfo);
                roleInfoList.remove(roleInfoIdx);
            }
        }

        initializeMissingRoles(relationBaseFlag,
                               relationObj,
                               relationObjName,
                               relationId,
                               relationTypeName,
                               roleInfoList);


        synchronized(myRelId2ObjMap) {
            if (relationBaseFlag) {
                myRelId2ObjMap.put(relationId, relationObj);
            } else {
                myRelId2ObjMap.put(relationId, relationObjName);
            }
        }

        synchronized(myRelId2RelTypeMap) {
            myRelId2RelTypeMap.put(relationId,
                                   relationTypeName);
        }

        synchronized(myRelType2RelIdsMap) {
            List<String> relIdList =
                myRelType2RelIdsMap.get(relationTypeName);
            boolean firstRelFlag = false;
            if (relIdList == null) {
                firstRelFlag = true;
                relIdList = new ArrayList<>();
            }
            relIdList.add(relationId);
            if (firstRelFlag) {
                myRelType2RelIdsMap.put(relationTypeName, relIdList);
            }
        }

        for (Role currRole : roleList.asList()) {
            List<ObjectName> dummyList = new ArrayList<>();
            try {
                updateRoleMap(relationId, currRole, dummyList);

            } catch (RelationNotFoundException exc) {
            }
        }

        try {
            sendRelationCreationNotification(relationId);

        } catch (RelationNotFoundException exc) {
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    private Integer checkRoleInt(int chkType,
                                 String roleName,
                                 List<ObjectName> roleValue,
                                 RoleInfo roleInfo,
                                 boolean writeChkFlag)
        throws IllegalArgumentException {

        if (roleName == null ||
            roleInfo == null ||
            (chkType == 2 && roleValue == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3} {4}",
                            chkType, roleName, roleValue, roleInfo, writeChkFlag);

        String expName = roleInfo.getName();
        if (!(roleName.equals(expName))) {
            RELATION_LOGGER.log(Level.TRACE, "RETURN");
            return Integer.valueOf(RoleStatus.NO_ROLE_WITH_NAME);
        }

        if (chkType == 1) {
            boolean isReadable = roleInfo.isReadable();
            if (!isReadable) {
                RELATION_LOGGER.log(Level.TRACE, "RETURN");
                return Integer.valueOf(RoleStatus.ROLE_NOT_READABLE);
            } else {
                RELATION_LOGGER.log(Level.TRACE, "RETURN");
                return 0;
            }
        }

        if (writeChkFlag) {
            boolean isWritable = roleInfo.isWritable();
            if (!isWritable) {
                RELATION_LOGGER.log(Level.TRACE, "RETURN");
                return RoleStatus.ROLE_NOT_WRITABLE;
            }
        }

        int refNbr = roleValue.size();

        boolean chkMinFlag = roleInfo.checkMinDegree(refNbr);
        if (!chkMinFlag) {
            RELATION_LOGGER.log(Level.TRACE, "RETURN");
            return RoleStatus.LESS_THAN_MIN_ROLE_DEGREE;
        }

        boolean chkMaxFlag = roleInfo.checkMaxDegree(refNbr);
        if (!chkMaxFlag) {
            RELATION_LOGGER.log(Level.TRACE, "RETURN");
            return RoleStatus.MORE_THAN_MAX_ROLE_DEGREE;
        }

        String expClassName = roleInfo.getRefMBeanClassName();

        for (ObjectName currObjName : roleValue) {

            if (currObjName == null) {
                RELATION_LOGGER.log(Level.TRACE, "RETURN");
                return RoleStatus.REF_MBEAN_NOT_REGISTERED;
            }

            try {
                boolean classSts = myMBeanServer.isInstanceOf(currObjName,
                                                              expClassName);
                if (!classSts) {
                    RELATION_LOGGER.log(Level.TRACE, "RETURN");
                    return RoleStatus.REF_MBEAN_OF_INCORRECT_CLASS;
                }

            } catch (InstanceNotFoundException exc) {
                RELATION_LOGGER.log(Level.TRACE, "RETURN");
                return RoleStatus.REF_MBEAN_NOT_REGISTERED;
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return 0;
    }



    private void initializeMissingRoles(boolean relationBaseFlag,
                                        RelationSupport relationObj,
                                        ObjectName relationObjName,
                                        String relationId,
                                        String relationTypeName,
                                        List<RoleInfo> roleInfoList)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               InvalidRoleValueException {

        if ((relationBaseFlag &&
             (relationObj == null ||
              relationObjName != null)) ||
            (!relationBaseFlag &&
             (relationObjName == null ||
              relationObj != null)) ||
            relationId == null ||
            relationTypeName == null ||
            roleInfoList == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3} {4} {5}",
                            relationBaseFlag, relationObj, relationObjName,
                            relationId, relationTypeName, roleInfoList);

        isActive();

        for (RoleInfo currRoleInfo : roleInfoList) {

            String roleName = currRoleInfo.getName();

            List<ObjectName> emptyValue = new ArrayList<>();
            Role role = new Role(roleName, emptyValue);

            if (relationBaseFlag) {

                try {
                    relationObj.setRoleInt(role, true, this, false);

                } catch (RoleNotFoundException | RelationTypeNotFoundException |
                         RelationNotFoundException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else {

                Object[] params = new Object[1];
                params[0] = role;
                String[] signature = new String[1];
                signature[0] = "javax.management.relation.Role";
                try {
                    myMBeanServer.setAttribute(relationObjName,
                                               new Attribute("Role", role));

                } catch (InstanceNotFoundException | InvalidAttributeValueException |
                         AttributeNotFoundException | ReflectionException exc1) {
                    throw new RuntimeException(exc1.getMessage());
                } catch (MBeanException exc2) {
                    Exception wrappedExc = exc2.getTargetException();
                    if (wrappedExc instanceof InvalidRoleValueException) {
                        throw ((InvalidRoleValueException)wrappedExc);
                    } else {
                        throw new RuntimeException(wrappedExc.getMessage());
                    }
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    static void throwRoleProblemException(int pbType,
                                          String roleName)
        throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException {

        if (roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        int excType = 0;

        String excMsgPart = null;

        switch (pbType) {
        case RoleStatus.NO_ROLE_WITH_NAME:
            excMsgPart = " does not exist in relation.";
            excType = 1;
            break;
        case RoleStatus.ROLE_NOT_READABLE:
            excMsgPart = " is not readable.";
            excType = 1;
            break;
        case RoleStatus.ROLE_NOT_WRITABLE:
            excMsgPart = " is not writable.";
            excType = 1;
            break;
        case RoleStatus.LESS_THAN_MIN_ROLE_DEGREE:
            excMsgPart = " has a number of MBean references less than the expected minimum degree.";
            excType = 2;
            break;
        case RoleStatus.MORE_THAN_MAX_ROLE_DEGREE:
            excMsgPart = " has a number of MBean references greater than the expected maximum degree.";
            excType = 2;
            break;
        case RoleStatus.REF_MBEAN_OF_INCORRECT_CLASS:
            excMsgPart = " has an MBean reference to an MBean not of the expected class of references for that role.";
            excType = 2;
            break;
        case RoleStatus.REF_MBEAN_NOT_REGISTERED:
            excMsgPart = " has a reference to null or to an MBean not registered.";
            excType = 2;
            break;
        }

        StringBuilder excMsgStrB = new StringBuilder(roleName);
        excMsgStrB.append(excMsgPart);
        String excMsg = excMsgStrB.toString();
        if (excType == 1) {
            throw new RoleNotFoundException(excMsg);

        } else if (excType == 2) {
            throw new InvalidRoleValueException(excMsg);
        }
    }

    private void sendNotificationInt(int intNtfType,
                                     String message,
                                     String relationId,
                                     List<ObjectName> unregMBeanList,
                                     String roleName,
                                     List<ObjectName> roleNewValue,
                                     List<ObjectName> oldValue)
        throws IllegalArgumentException,
               RelationNotFoundException {

        if (message == null ||
            relationId == null ||
            (intNtfType != 3 && unregMBeanList != null) ||
            (intNtfType == 2 &&
             (roleName == null ||
              roleNewValue == null ||
              oldValue == null))) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3} {4} {5} {6}",
                            intNtfType, message, relationId, unregMBeanList,
                            roleName, roleNewValue, oldValue);

        String relTypeName;
        synchronized(myRelId2RelTypeMap) {
            relTypeName = (myRelId2RelTypeMap.get(relationId));
        }

        ObjectName relObjName = isRelationMBean(relationId);

        String ntfType = null;
        if (relObjName != null) {
            switch (intNtfType) {
            case 1:
                ntfType = RelationNotification.RELATION_MBEAN_CREATION;
                break;
            case 2:
                ntfType = RelationNotification.RELATION_MBEAN_UPDATE;
                break;
            case 3:
                ntfType = RelationNotification.RELATION_MBEAN_REMOVAL;
                break;
            }
        } else {
            switch (intNtfType) {
            case 1:
                ntfType = RelationNotification.RELATION_BASIC_CREATION;
                break;
            case 2:
                ntfType = RelationNotification.RELATION_BASIC_UPDATE;
                break;
            case 3:
                ntfType = RelationNotification.RELATION_BASIC_REMOVAL;
                break;
            }
        }

        Long seqNo = atomicSeqNo.incrementAndGet();

        Date currDate = new Date();
        long timeStamp = currDate.getTime();

        RelationNotification ntf = null;

        if (ntfType.equals(RelationNotification.RELATION_BASIC_CREATION) ||
            ntfType.equals(RelationNotification.RELATION_MBEAN_CREATION) ||
            ntfType.equals(RelationNotification.RELATION_BASIC_REMOVAL) ||
            ntfType.equals(RelationNotification.RELATION_MBEAN_REMOVAL))

            ntf = new RelationNotification(ntfType,
                                           this,
                                           seqNo.longValue(),
                                           timeStamp,
                                           message,
                                           relationId,
                                           relTypeName,
                                           relObjName,
                                           unregMBeanList);

        else if (ntfType.equals(RelationNotification.RELATION_BASIC_UPDATE)
                 ||
                 ntfType.equals(RelationNotification.RELATION_MBEAN_UPDATE))
            {
                ntf = new RelationNotification(ntfType,
                                               this,
                                               seqNo.longValue(),
                                               timeStamp,
                                               message,
                                               relationId,
                                               relTypeName,
                                               relObjName,
                                               roleName,
                                               roleNewValue,
                                               oldValue);
            }

        sendNotification(ntf);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    private void handleReferenceUnregistration(String relationId,
                                               ObjectName objectName,
                                               List<String> roleNameList)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException,
               RoleNotFoundException {

        if (relationId == null ||
            roleNameList == null ||
            objectName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            relationId, objectName, roleNameList);

        isActive();

        String currRelTypeName = getRelationTypeName(relationId);

        Object relObj = getRelation(relationId);

        boolean deleteRelFlag = false;

        for (String currRoleName : roleNameList) {

            if (deleteRelFlag) {
                break;
            }

            int currRoleRefNbr =
                (getRoleCardinality(relationId, currRoleName)).intValue();

            int currRoleNewRefNbr = currRoleRefNbr - 1;

            RoleInfo currRoleInfo;
            try {
                currRoleInfo = getRoleInfo(currRelTypeName,
                                           currRoleName);
            } catch (RelationTypeNotFoundException | RoleInfoNotFoundException exc) {
                throw new RuntimeException(exc.getMessage());
            }

            boolean chkMinFlag = currRoleInfo.checkMinDegree(currRoleNewRefNbr);

            if (!chkMinFlag) {
                deleteRelFlag = true;
            }
        }

        if (deleteRelFlag) {
            removeRelation(relationId);

        } else {

            for (String currRoleName : roleNameList) {

                if (relObj instanceof RelationSupport) {
                    try {
                        ((RelationSupport)relObj).handleMBeanUnregistrationInt(
                                                  objectName,
                                                  currRoleName,
                                                  true,
                                                  this);
                    } catch (RelationTypeNotFoundException | InvalidRoleValueException exc) {
                        throw new RuntimeException(exc.getMessage());
                    }

                } else {
                    Object[] params = new Object[2];
                    params[0] = objectName;
                    params[1] = currRoleName;
                    String[] signature = new String[2];
                    signature[0] = "javax.management.ObjectName";
                    signature[1] = "java.lang.String";
                    try {
                        myMBeanServer.invoke(((ObjectName)relObj),
                                             "handleMBeanUnregistration",
                                             params,
                                             signature);
                    } catch (InstanceNotFoundException | ReflectionException exc1) {
                        throw new RuntimeException(exc1.getMessage());
                    } catch (MBeanException exc2) {
                        Exception wrappedExc = exc2.getTargetException();
                        throw new RuntimeException(wrappedExc.getMessage());
                    }

                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }
}
