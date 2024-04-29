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



import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import java.util.concurrent.atomic.AtomicBoolean;
import static com.sun.jmx.defaults.JmxProperties.RELATION_LOGGER;
import static com.sun.jmx.mbeanserver.Util.cast;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * A RelationSupport object is used internally by the Relation Service to
 * represent simple relations (only roles, no properties or methods), with an
 * unlimited number of roles, of any relation type. As internal representation,
 * it is not exposed to the user.
 * <P>RelationSupport class conforms to the design patterns of standard MBean. So
 * the user can decide to instantiate a RelationSupport object himself as
 * a MBean (as it follows the MBean design patterns), to register it in the
 * MBean Server, and then to add it in the Relation Service.
 * <P>The user can also, when creating his own MBean relation class, have it
 * extending RelationSupport, to retrieve the implementations of required
 * interfaces (see below).
 * <P>It is also possible to have in a user relation MBean class a member
 * being a RelationSupport object, and to implement the required interfaces by
 * delegating all to this member.
 * <P> RelationSupport implements the Relation interface (to be handled by the
 * Relation Service).
 * <P>It implements also the MBeanRegistration interface to be able to retrieve
 * the MBean Server where it is registered (if registered as a MBean) to access
 * to its Relation Service.
 *
 * @since 1.5
 */
public class RelationSupport
    implements RelationSupportMBean, MBeanRegistration {


    private String myRelId = null;

    private ObjectName myRelServiceName = null;

    private MBeanServer myRelServiceMBeanServer = null;

    private String myRelTypeName = null;

    private final Map<String,Role> myRoleName2ValueMap = new HashMap<>();

    private final AtomicBoolean myInRelServFlg = new AtomicBoolean();


    /**
     * Creates a {@code RelationSupport} object.
     * <P>This constructor has to be used when the RelationSupport object will
     * be registered as a MBean by the user, or when creating a user relation
     * MBean whose class extends RelationSupport.
     * <P>Nothing is done at the Relation Service level, i.e.
     * the {@code RelationSupport} object is not added to the
     * {@code RelationService} and no checks are performed to
     * see if the provided values are correct.
     * The object is always created, EXCEPT if:
     * <P>- any of the required parameters is {@code null}.
     * <P>- the same name is used for two roles.
     * <P>To be handled as a relation, the {@code RelationSupport} object has
     * to be added to the Relation Service using the Relation Service method
     * addRelation().
     *
     * @param relationId  relation identifier, to identify the relation in the
     * Relation Service.
     * <P>Expected to be unique in the given Relation Service.
     * @param relationServiceName  ObjectName of the Relation Service where
     * the relation will be registered.
     * <P>This parameter is required as it is the Relation Service that is
     * aware of the definition of the relation type of the given relation,
     * so that will be able to check update operations (set).
     * @param relationTypeName  Name of relation type.
     * <P>Expected to have been created in the given Relation Service.
     * @param list  list of roles (Role objects) to initialize the
     * relation. Can be {@code null}.
     * <P>Expected to conform to relation info in associated relation type.
     *
     * @exception InvalidRoleValueException  if the same name is used for two
     * roles.
     * @exception IllegalArgumentException  if any of the required parameters
     * (relation id, relation service ObjectName, or relation type name) is
     * {@code null}.
     */
    public RelationSupport(String relationId,
                        ObjectName relationServiceName,
                        String relationTypeName,
                        RoleList list)
        throws InvalidRoleValueException,
               IllegalArgumentException {

        super();

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        initMembers(relationId,
                    relationServiceName,
                    null,
                    relationTypeName,
                    list);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
    }

    /**
     * Creates a {@code RelationSupport} object.
     * <P>This constructor has to be used when the user relation MBean
     * implements the interfaces expected to be supported by a relation by
     * delegating to a RelationSupport object.
     * <P>This object needs to know the Relation Service expected to handle the
     * relation. So it has to know the MBean Server where the Relation Service
     * is registered.
     * <P>According to a limitation, a relation MBean must be registered in the
     * same MBean Server as the Relation Service expected to handle it. So the
     * user relation MBean has to be created and registered, and then the
     * wrapped RelationSupport object can be created within the identified MBean
     * Server.
     * <P>Nothing is done at the Relation Service level, i.e.
     * the {@code RelationSupport} object is not added to the
     * {@code RelationService} and no checks are performed to
     * see if the provided values are correct.
     * The object is always created, EXCEPT if:
     * <P>- any of the required parameters is {@code null}.
     * <P>- the same name is used for two roles.
     * <P>To be handled as a relation, the {@code RelationSupport} object has
     * to be added to the Relation Service using the Relation Service method
     * addRelation().
     *
     * @param relationId  relation identifier, to identify the relation in the
     * Relation Service.
     * <P>Expected to be unique in the given Relation Service.
     * @param relationServiceName  ObjectName of the Relation Service where
     * the relation will be registered.
     * <P>This parameter is required as it is the Relation Service that is
     * aware of the definition of the relation type of the given relation,
     * so that will be able to check update operations (set).
     * @param relationServiceMBeanServer  MBean Server where the wrapping MBean
     * is or will be registered.
     * <P>Expected to be the MBean Server where the Relation Service is or will
     * be registered.
     * @param relationTypeName  Name of relation type.
     * <P>Expected to have been created in the given Relation Service.
     * @param list  list of roles (Role objects) to initialize the
     * relation. Can be {@code null}.
     * <P>Expected to conform to relation info in associated relation type.
     *
     * @exception InvalidRoleValueException  if the same name is used for two
     * roles.
     * @exception IllegalArgumentException  if any of the required parameters
     * (relation id, relation service ObjectName, relation service MBeanServer,
     * or relation type name) is {@code null}.
     */
    public RelationSupport(String relationId,
                        ObjectName relationServiceName,
                        MBeanServer relationServiceMBeanServer,
                        String relationTypeName,
                        RoleList list)
        throws InvalidRoleValueException,
               IllegalArgumentException {

        super();

        if (relationServiceMBeanServer == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        initMembers(relationId,
                    relationServiceName,
                    relationServiceMBeanServer,
                    relationTypeName,
                    list);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
    }


    /**
     * Retrieves role value for given role name.
     * <P>Checks if the role exists and is readable according to the relation
     * type.
     *
     * @param roleName  name of role
     *
     * @return the ArrayList of ObjectName objects being the role value
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RoleNotFoundException  if:
     * <P>- there is no role with given name
     * <P>- the role is not readable.
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     *
     * @see #setRole
     */
    public List<ObjectName> getRole(String roleName)
        throws IllegalArgumentException,
               RoleNotFoundException,
               RelationServiceNotRegisteredException {

        if (roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", roleName);

        List<ObjectName> result = cast(
            getRoleInt(roleName, false, null, false));

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Retrieves values of roles with given names.
     * <P>Checks for each role if it exists and is readable according to the
     * relation type.
     *
     * @param roleNameArray  array of names of roles to be retrieved
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully retrieved) and a RoleUnresolvedList (for roles not
     * retrieved).
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     *
     * @see #setRoles
     */
    public RoleResult getRoles(String[] roleNameArray)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException {

        if (roleNameArray == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        RoleResult result = getRolesInt(roleNameArray, false, null);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns all roles present in the relation.
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully retrieved) and a RoleUnresolvedList (for roles not
     * readable).
     *
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     */
    public RoleResult getAllRoles()
        throws RelationServiceNotRegisteredException {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        RoleResult result = null;
        try {
            result = getAllRolesInt(false, null);
        } catch (IllegalArgumentException exc) {
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns all roles in the relation without checking read mode.
     *
     * @return a RoleList
     */
    public RoleList retrieveAllRoles() {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        RoleList result;
        synchronized(myRoleName2ValueMap) {
            result =
                new RoleList(new ArrayList<>(myRoleName2ValueMap.values()));
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Returns the number of MBeans currently referenced in the given role.
     *
     * @param roleName  name of role
     *
     * @return the number of currently referenced MBeans in that role
     *
     * @exception IllegalArgumentException  if null role name
     * @exception RoleNotFoundException  if there is no role with given name
     */
    public Integer getRoleCardinality(String roleName)
        throws IllegalArgumentException,
               RoleNotFoundException {

        if (roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", roleName);

        Role role;
        synchronized(myRoleName2ValueMap) {
            role = (myRoleName2ValueMap.get(roleName));
        }
        if (role == null) {
            int pbType = RoleStatus.NO_ROLE_WITH_NAME;
            try {
                RelationService.throwRoleProblemException(pbType,
                                                          roleName);
            } catch (InvalidRoleValueException exc) {
            }
        }

        List<ObjectName> roleValue = role.getRoleValue();

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return roleValue.size();
    }

    /**
     * Sets the given role.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>Will send a notification (RelationNotification with type
     * RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
     * relation is a MBean or not).
     *
     * @param role  role to be set (name and new value)
     *
     * @exception IllegalArgumentException  if null role
     * @exception RoleNotFoundException  if there is no role with the supplied
     * role's name or if the role is not writable (no test on the write access
     * mode performed when initializing the role)
     * @exception InvalidRoleValueException  if value provided for
     * role is not valid, i.e.:
     * <P>- the number of referenced MBeans in given value is less than
     * expected minimum degree
     * <P>- the number of referenced MBeans in provided value exceeds expected
     * maximum degree
     * <P>- one referenced MBean in the value is not an Object of the MBean
     * class expected for that role
     * <P>- a MBean provided for that role does not exist
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service
     * @exception RelationNotFoundException  if the relation has not been
     * added in the Relation Service.
     *
     * @see #getRole
     */
    public void setRole(Role role)
        throws IllegalArgumentException,
               RoleNotFoundException,
               RelationTypeNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException {

        if (role == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", role);

        Object result = setRoleInt(role, false, null, false);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Sets the given roles.
     * <P>Will check the role according to its corresponding role definition
     * provided in relation's relation type
     * <P>Will send one notification (RelationNotification with type
     * RELATION_BASIC_UPDATE or RELATION_MBEAN_UPDATE, depending if the
     * relation is a MBean or not) per updated role.
     *
     * @param list  list of roles to be set
     *
     * @return a RoleResult object, including a RoleList (for roles
     * successfully set) and a RoleUnresolvedList (for roles not
     * set).
     *
     * @exception IllegalArgumentException  if null role list
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service.
     * @exception RelationNotFoundException  if the relation MBean has not been
     * added in the Relation Service.
     *
     * @see #getRoles
     */
    public RoleResult setRoles(RoleList list)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (list == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", list);

        RoleResult result = setRolesInt(list, false, null);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    /**
     * Callback used by the Relation Service when a MBean referenced in a role
     * is unregistered.
     * <P>The Relation Service will call this method to let the relation
     * take action to reflect the impact of such unregistration.
     * <P>BEWARE. the user is not expected to call this method.
     * <P>Current implementation is to set the role with its current value
     * (list of ObjectNames of referenced MBeans) without the unregistered
     * one.
     *
     * @param objectName  ObjectName of unregistered MBean
     * @param roleName  name of role where the MBean is referenced
     *
     * @exception IllegalArgumentException  if null parameter
     * @exception RoleNotFoundException  if role does not exist in the
     * relation or is not writable
     * @exception InvalidRoleValueException  if role value does not conform to
     * the associated role info (this will never happen when called from the
     * Relation Service)
     * @exception RelationServiceNotRegisteredException  if the Relation
     * Service is not registered in the MBean Server
     * @exception RelationTypeNotFoundException  if the relation type has not
     * been declared in the Relation Service.
     * @exception RelationNotFoundException  if this method is called for a
     * relation MBean not added in the Relation Service.
     */
    public void handleMBeanUnregistration(ObjectName objectName,
                                          String roleName)
        throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (objectName == null || roleName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1}", objectName, roleName);

        handleMBeanUnregistrationInt(objectName,
                                     roleName,
                                     false,
                                     null);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    /**
     * Retrieves MBeans referenced in the various roles of the relation.
     *
     * @return a HashMap mapping:
     * <P> ObjectName {@literal ->} ArrayList of String (role names)
     */
    public Map<ObjectName,List<String>> getReferencedMBeans() {

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        Map<ObjectName,List<String>> refMBeanMap = new HashMap<>();

        synchronized(myRoleName2ValueMap) {

            for (Role currRole : myRoleName2ValueMap.values()) {

                String currRoleName = currRole.getRoleName();
                List<ObjectName> currRefMBeanList = currRole.getRoleValue();

                for (ObjectName currRoleObjName : currRefMBeanList) {

                    List<String> mbeanRoleNameList =
                        refMBeanMap.get(currRoleObjName);

                    boolean newRefFlg = false;
                    if (mbeanRoleNameList == null) {
                        newRefFlg = true;
                        mbeanRoleNameList = new ArrayList<>();
                    }
                    mbeanRoleNameList.add(currRoleName);
                    if (newRefFlg) {
                        refMBeanMap.put(currRoleObjName, mbeanRoleNameList);
                    }
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return refMBeanMap;
    }

    /**
     * Returns name of associated relation type.
     */
    public String getRelationTypeName() {
        return myRelTypeName;
    }

    /**
     * Returns ObjectName of the Relation Service handling the relation.
     *
     * @return the ObjectName of the Relation Service.
     */
    public ObjectName getRelationServiceName() {
        return myRelServiceName;
    }

    /**
     * Returns relation identifier (used to uniquely identify the relation
     * inside the Relation Service).
     *
     * @return the relation id.
     */
    public String getRelationId() {
        return myRelId;
    }


    public ObjectName preRegister(MBeanServer server,
                                  ObjectName name)
        throws Exception {

        myRelServiceMBeanServer = server;
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
     * Returns an internal flag specifying if the object is still handled by
     * the Relation Service.
     */
    public Boolean isInRelationService() {
        return myInRelServFlg.get();
    }

    public void setRelationServiceManagementFlag(Boolean flag)
        throws IllegalArgumentException {

        if (flag == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }
        myInRelServFlg.set(flag);
    }


    Object getRoleInt(String roleName,
                      boolean relationServCallFlg,
                      RelationService relationServ,
                      boolean multiRoleFlg)
        throws IllegalArgumentException,
               RoleNotFoundException,
               RelationServiceNotRegisteredException {

        if (roleName == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", roleName);

        int pbType = 0;

        Role role;
        synchronized(myRoleName2ValueMap) {
            role = (myRoleName2ValueMap.get(roleName));
        }

        if (role == null) {
                pbType = RoleStatus.NO_ROLE_WITH_NAME;

        } else {
            Integer status;

            if (relationServCallFlg) {

                try {
                    status = relationServ.checkRoleReading(roleName,
                                                         myRelTypeName);
                } catch (RelationTypeNotFoundException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else {

                Object[] params = new Object[2];
                params[0] = roleName;
                params[1] = myRelTypeName;
                String[] signature = new String[2];
                signature[0] = "java.lang.String";
                signature[1] = "java.lang.String";
                try {
                    status = (Integer)
                        (myRelServiceMBeanServer.invoke(myRelServiceName,
                                                        "checkRoleReading",
                                                        params,
                                                        signature));
                } catch (MBeanException exc1) {
                    throw new RuntimeException("incorrect relation type");
                } catch (ReflectionException exc2) {
                    throw new RuntimeException(exc2.getMessage());
                } catch (InstanceNotFoundException exc3) {
                    throw new RelationServiceNotRegisteredException(
                                                            exc3.getMessage());
                }
            }

            pbType = status.intValue();
        }

        Object result;

        if (pbType == 0) {

            if (!(multiRoleFlg)) {
                result = new ArrayList<>(role.getRoleValue());

            } else {
                result = (Role)(role.clone());
            }

        } else {

            if (!(multiRoleFlg)) {
                try {
                    RelationService.throwRoleProblemException(pbType,
                                                              roleName);
                    return null;
                } catch (InvalidRoleValueException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else {
                result = new RoleUnresolved(roleName, null, pbType);
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    RoleResult getRolesInt(String[] roleNameArray,
                           boolean relationServCallFlg,
                           RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException {

        if (roleNameArray == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        RoleList roleList = new RoleList();
        RoleUnresolvedList roleUnresList = new RoleUnresolvedList();

        for (int i = 0; i < roleNameArray.length; i++) {
            String currRoleName = roleNameArray[i];

            Object currResult;

            try {
                currResult = getRoleInt(currRoleName,
                                        relationServCallFlg,
                                        relationServ,
                                        true);

            } catch (RoleNotFoundException exc) {
                return null; 
            }

            if (currResult instanceof Role) {
                try {
                    roleList.add((Role)currResult);
                } catch (IllegalArgumentException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else if (currResult instanceof RoleUnresolved) {
                try {
                    roleUnresList.add((RoleUnresolved)currResult);
                } catch (IllegalArgumentException exc) {
                    throw new RuntimeException(exc.getMessage());
                }
            }
        }

        RoleResult result = new RoleResult(roleList, roleUnresList);
        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    RoleResult getAllRolesInt(boolean relationServCallFlg,
                              RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException {

        if (relationServCallFlg && relationServ == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY");

        List<String> roleNameList;
        synchronized(myRoleName2ValueMap) {
            roleNameList = new ArrayList<>(myRoleName2ValueMap.keySet());
        }
        String[] roleNames = new String[roleNameList.size()];
        roleNameList.toArray(roleNames);

        RoleResult result = getRolesInt(roleNames,
                                        relationServCallFlg,
                                        relationServ);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    Object setRoleInt(Role aRole,
                      boolean relationServCallFlg,
                      RelationService relationServ,
                      boolean multiRoleFlg)
        throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (aRole == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            aRole, relationServCallFlg, relationServ,
                            multiRoleFlg);

        String roleName = aRole.getRoleName();
        int pbType = 0;

        Role role;
        synchronized(myRoleName2ValueMap) {
            role = (myRoleName2ValueMap.get(roleName));
        }

        List<ObjectName> oldRoleValue;
        Boolean initFlg;

        if (role == null) {
            initFlg = true;
            oldRoleValue = new ArrayList<>();

        } else {
            initFlg = false;
            oldRoleValue = role.getRoleValue();
        }

        try {
            Integer status;

            if (relationServCallFlg) {

                status = relationServ.checkRoleWriting(aRole,
                                                     myRelTypeName,
                                                     initFlg);

            } else {

                Object[] params = new Object[3];
                params[0] = aRole;
                params[1] = myRelTypeName;
                params[2] = initFlg;
                String[] signature = new String[3];
                signature[0] = "javax.management.relation.Role";
                signature[1] = "java.lang.String";
                signature[2] = "java.lang.Boolean";
                status = (Integer)
                    (myRelServiceMBeanServer.invoke(myRelServiceName,
                                                    "checkRoleWriting",
                                                    params,
                                                    signature));
            }

            pbType = status.intValue();

        } catch (MBeanException exc2) {

            Exception wrappedExc = exc2.getTargetException();
            if (wrappedExc instanceof RelationTypeNotFoundException) {
                throw ((RelationTypeNotFoundException)wrappedExc);

            } else {
                throw new RuntimeException(wrappedExc.getMessage());
            }

        } catch (ReflectionException | RelationTypeNotFoundException exc3) {
            throw new RuntimeException(exc3.getMessage());

        } catch (InstanceNotFoundException exc4) {
            throw new RelationServiceNotRegisteredException(exc4.getMessage());
        }

        Object result = null;

        if (pbType == 0) {
            if (!(initFlg.booleanValue())) {


                sendRoleUpdateNotification(aRole,
                                           oldRoleValue,
                                           relationServCallFlg,
                                           relationServ);

                updateRelationServiceMap(aRole,
                                         oldRoleValue,
                                         relationServCallFlg,
                                         relationServ);

            }

            synchronized(myRoleName2ValueMap) {
                myRoleName2ValueMap.put(roleName,
                                        (Role)(aRole.clone()));
            }


            if (multiRoleFlg) {
                result = aRole;
            }

        } else {


            if (!(multiRoleFlg)) {
                RelationService.throwRoleProblemException(pbType,
                                                          roleName);
                return null;

            } else {
                result = new RoleUnresolved(roleName,
                                            aRole.getRoleValue(),
                                            pbType);
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    private void sendRoleUpdateNotification(Role newRole,
                                            List<ObjectName> oldRoleValue,
                                            boolean relationServCallFlg,
                                            RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException {

        if (newRole == null ||
            oldRoleValue == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            newRole, oldRoleValue, relationServCallFlg,
                            relationServ);

        if (relationServCallFlg) {
            try {
                relationServ.sendRoleUpdateNotification(myRelId,
                                                      newRole,
                                                      oldRoleValue);
            } catch (RelationNotFoundException exc) {
                throw new RuntimeException(exc.getMessage());
            }

        } else {

            Object[] params = new Object[3];
            params[0] = myRelId;
            params[1] = newRole;
            params[2] = oldRoleValue;
            String[] signature = new String[3];
            signature[0] = "java.lang.String";
            signature[1] = "javax.management.relation.Role";
            signature[2] = "java.util.List";

            try {
                myRelServiceMBeanServer.invoke(myRelServiceName,
                                               "sendRoleUpdateNotification",
                                               params,
                                               signature);
            } catch (ReflectionException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (InstanceNotFoundException exc2) {
                throw new RelationServiceNotRegisteredException(
                                                            exc2.getMessage());
            } catch (MBeanException exc3) {
                Exception wrappedExc = exc3.getTargetException();
                if (wrappedExc instanceof RelationNotFoundException) {
                    throw ((RelationNotFoundException)wrappedExc);
                } else {
                    throw new RuntimeException(wrappedExc.getMessage());
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    private void updateRelationServiceMap(Role newRole,
                                          List<ObjectName> oldRoleValue,
                                          boolean relationServCallFlg,
                                          RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationNotFoundException {

        if (newRole == null ||
            oldRoleValue == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            newRole, oldRoleValue, relationServCallFlg,
                            relationServ);

        if (relationServCallFlg) {
            try {
                relationServ.updateRoleMap(myRelId,
                                         newRole,
                                         oldRoleValue);
            } catch (RelationNotFoundException exc) {
                throw new RuntimeException(exc.getMessage());
            }

        } else {
            Object[] params = new Object[3];
            params[0] = myRelId;
            params[1] = newRole;
            params[2] = oldRoleValue;
            String[] signature = new String[3];
            signature[0] = "java.lang.String";
            signature[1] = "javax.management.relation.Role";
            signature[2] = "java.util.List";
            try {
                myRelServiceMBeanServer.invoke(myRelServiceName,
                                               "updateRoleMap",
                                               params,
                                               signature);
            } catch (ReflectionException exc1) {
                throw new RuntimeException(exc1.getMessage());
            } catch (InstanceNotFoundException exc2) {
                throw new
                     RelationServiceNotRegisteredException(exc2.getMessage());
            } catch (MBeanException exc3) {
                Exception wrappedExc = exc3.getTargetException();
                if (wrappedExc instanceof RelationNotFoundException) {
                    throw ((RelationNotFoundException)wrappedExc);
                } else {
                    throw new RuntimeException(wrappedExc.getMessage());
                }
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    RoleResult setRolesInt(RoleList list,
                           boolean relationServCallFlg,
                           RelationService relationServ)
        throws IllegalArgumentException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (list == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2}",
                            list, relationServCallFlg, relationServ);

        RoleList roleList = new RoleList();
        RoleUnresolvedList roleUnresList = new RoleUnresolvedList();

        for (Role currRole : list.asList()) {

            Object currResult = null;
            try {
                currResult = setRoleInt(currRole,
                                        relationServCallFlg,
                                        relationServ,
                                        true);
            } catch (RoleNotFoundException exc1) {
            } catch (InvalidRoleValueException exc2) {
            }

            if (currResult instanceof Role) {
                try {
                    roleList.add((Role)currResult);
                } catch (IllegalArgumentException exc) {
                    throw new RuntimeException(exc.getMessage());
                }

            } else if (currResult instanceof RoleUnresolved) {
                try {
                    roleUnresList.add((RoleUnresolved)currResult);
                } catch (IllegalArgumentException exc) {
                    throw new RuntimeException(exc.getMessage());
                }
            }
        }

        RoleResult result = new RoleResult(roleList, roleUnresList);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return result;
    }

    private void initMembers(String relationId,
                             ObjectName relationServiceName,
                             MBeanServer relationServiceMBeanServer,
                             String relationTypeName,
                             RoleList list)
        throws InvalidRoleValueException,
               IllegalArgumentException {

        if (relationId == null ||
            relationServiceName == null ||
            relationTypeName == null) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3} {4}",
                            relationId, relationServiceName,
                            relationServiceMBeanServer, relationTypeName, list);

        myRelId = relationId;
        myRelServiceName = relationServiceName;
        myRelServiceMBeanServer = relationServiceMBeanServer;
        myRelTypeName = relationTypeName;
        initRoleMap(list);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    private void initRoleMap(RoleList list)
        throws InvalidRoleValueException {

        if (list == null) {
            return;
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0}", list);

        synchronized(myRoleName2ValueMap) {

            for (Role currRole : list.asList()) {

                String currRoleName = currRole.getRoleName();

                if (myRoleName2ValueMap.containsKey(currRoleName)) {
                    StringBuilder excMsgStrB = new StringBuilder("Role name ");
                    excMsgStrB.append(currRoleName);
                    excMsgStrB.append(" used for two roles.");
                    throw new InvalidRoleValueException(excMsgStrB.toString());
                }

                myRoleName2ValueMap.put(currRoleName,
                                        (Role)(currRole.clone()));
            }
        }

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

    void handleMBeanUnregistrationInt(ObjectName objectName,
                                      String roleName,
                                      boolean relationServCallFlg,
                                      RelationService relationServ)
        throws IllegalArgumentException,
               RoleNotFoundException,
               InvalidRoleValueException,
               RelationServiceNotRegisteredException,
               RelationTypeNotFoundException,
               RelationNotFoundException {

        if (objectName == null ||
            roleName == null ||
            (relationServCallFlg && relationServ == null)) {
            String excMsg = "Invalid parameter.";
            throw new IllegalArgumentException(excMsg);
        }

        RELATION_LOGGER.log(Level.TRACE, "ENTRY {0} {1} {2} {3}",
                            objectName, roleName, relationServCallFlg,
                            relationServ);

        Role role;
        synchronized(myRoleName2ValueMap) {
            role = (myRoleName2ValueMap.get(roleName));
        }

        if (role == null) {
            StringBuilder excMsgStrB = new StringBuilder();
            String excMsg = "No role with name ";
            excMsgStrB.append(excMsg);
            excMsgStrB.append(roleName);
            throw new RoleNotFoundException(excMsgStrB.toString());
        }
        List<ObjectName> currRoleValue = role.getRoleValue();

        List<ObjectName> newRoleValue = new ArrayList<>(currRoleValue);
        newRoleValue.remove(objectName);
        Role newRole = new Role(roleName, newRoleValue);

        Object result =
            setRoleInt(newRole, relationServCallFlg, relationServ, false);

        RELATION_LOGGER.log(Level.TRACE, "RETURN");
        return;
    }

}
