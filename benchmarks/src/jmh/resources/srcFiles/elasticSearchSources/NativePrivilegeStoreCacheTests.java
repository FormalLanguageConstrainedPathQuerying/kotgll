/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.security.authz.store;

import org.elasticsearch.ElasticsearchSecurityException;
import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequestBuilder;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.SecuritySingleNodeTestCase;
import org.elasticsearch.xpack.core.security.action.privilege.ClearPrivilegesCacheAction;
import org.elasticsearch.xpack.core.security.action.privilege.ClearPrivilegesCacheRequest;
import org.elasticsearch.xpack.core.security.action.privilege.ClearPrivilegesCacheResponse;
import org.elasticsearch.xpack.core.security.action.privilege.DeletePrivilegesRequestBuilder;
import org.elasticsearch.xpack.core.security.action.privilege.GetPrivilegesRequestBuilder;
import org.elasticsearch.xpack.core.security.action.privilege.PutPrivilegesAction;
import org.elasticsearch.xpack.core.security.action.privilege.PutPrivilegesRequest;
import org.elasticsearch.xpack.core.security.action.privilege.PutPrivilegesResponse;
import org.elasticsearch.xpack.core.security.action.role.PutRoleRequestBuilder;
import org.elasticsearch.xpack.core.security.action.role.PutRoleResponse;
import org.elasticsearch.xpack.core.security.action.user.HasPrivilegesAction;
import org.elasticsearch.xpack.core.security.action.user.HasPrivilegesRequest;
import org.elasticsearch.xpack.core.security.action.user.HasPrivilegesResponse;
import org.elasticsearch.xpack.core.security.action.user.PutUserRequestBuilder;
import org.elasticsearch.xpack.core.security.action.user.PutUserResponse;
import org.elasticsearch.xpack.core.security.authc.support.Hasher;
import org.elasticsearch.xpack.core.security.authz.RoleDescriptor;
import org.elasticsearch.xpack.core.security.authz.privilege.ApplicationPrivilegeDescriptor;
import org.junit.Before;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.elasticsearch.action.support.WriteRequest.RefreshPolicy.IMMEDIATE;
import static org.elasticsearch.test.SecuritySettingsSource.TEST_PASSWORD_HASHED;
import static org.elasticsearch.test.SecuritySettingsSource.TEST_ROLE;
import static org.elasticsearch.test.SecuritySettingsSourceField.TEST_PASSWORD;
import static org.elasticsearch.xpack.core.security.authz.privilege.ApplicationPrivilegeDescriptor.DOC_TYPE_VALUE;
import static org.elasticsearch.xpack.security.support.SecuritySystemIndices.SECURITY_MAIN_ALIAS;

public class NativePrivilegeStoreCacheTests extends SecuritySingleNodeTestCase {

    private static final String APP_USER_NAME = "app_user";

    @Override
    protected String configUsers() {
        return super.configUsers() + APP_USER_NAME + ":" + TEST_PASSWORD_HASHED + "\n";
    }

    @Override
    protected String configRoles() {
        return super.configRoles() + """
            app_role:
              cluster: ['monitor']
              indices:
                - names: ['*']
                  privileges: ['read']
              applications:
                - application: 'app-1'
                  privileges: ['read', 'check']
                  resources: ['foo']
                - application: 'app-2'
                  privileges: ['check']
                  resources: ['foo']
            """;
    }

    @Override
    protected String configUsersRoles() {
        return super.configUsersRoles() + "app_role:" + APP_USER_NAME + "\n" + TEST_ROLE + ":" + APP_USER_NAME + "\n";
    }

    @Override
    protected Settings nodeSettings() {
        Settings.Builder builder = Settings.builder().put(super.nodeSettings());
        builder.put("xpack.security.authz.store.privileges.cache.max_size", 5000);
        builder.put("xpack.security.authz.store.privileges.cache.ttl", "12h");
        return builder.build();
    }

    @Before
    public void configureApplicationPrivileges() {
        final List<ApplicationPrivilegeDescriptor> applicationPrivilegeDescriptors = Arrays.asList(
            new ApplicationPrivilegeDescriptor("app-1", "read", Set.of("r:a:b:c", "r:x:y:z"), emptyMap()),
            new ApplicationPrivilegeDescriptor("app-1", "write", Set.of("w:a:b:c", "w:x:y:z"), emptyMap()),
            new ApplicationPrivilegeDescriptor("app-1", "admin", Set.of("a:a:b:c", "a:x:y:z"), emptyMap()),
            new ApplicationPrivilegeDescriptor("app-2", "read", Set.of("r:e:f:g", "r:t:u:v"), emptyMap()),
            new ApplicationPrivilegeDescriptor("app-2", "write", Set.of("w:e:f:g", "w:t:u:v"), emptyMap()),
            new ApplicationPrivilegeDescriptor("app-2", "admin", Set.of("a:e:f:g", "a:t:u:v"), emptyMap())
        );

        final PutPrivilegesRequest putPrivilegesRequest = new PutPrivilegesRequest();
        putPrivilegesRequest.setPrivileges(applicationPrivilegeDescriptors);
        final ActionFuture<PutPrivilegesResponse> future = client().execute(PutPrivilegesAction.INSTANCE, putPrivilegesRequest);

        final PutPrivilegesResponse putPrivilegesResponse = future.actionGet();
        assertEquals(2, putPrivilegesResponse.created().size());
        assertEquals(6, putPrivilegesResponse.created().values().stream().mapToInt(List::size).sum());
    }

    public void testGetPrivilegesUsesCache() {
        final Client client = client();

        ApplicationPrivilegeDescriptor[] privileges = new GetPrivilegesRequestBuilder(client).application("app-2")
            .privileges("write")
            .get()
            .privileges();

        assertEquals(1, privileges.length);
        assertEquals("app-2", privileges[0].getApplication());
        assertEquals("write", privileges[0].getName());

        assertFalse(
            client.prepareBulk()
                .add(new DeleteRequest(SECURITY_MAIN_ALIAS, DOC_TYPE_VALUE + "_app-2:read"))
                .add(new DeleteRequest(SECURITY_MAIN_ALIAS, DOC_TYPE_VALUE + "_app-2:write"))
                .add(new DeleteRequest(SECURITY_MAIN_ALIAS, DOC_TYPE_VALUE + "_app-2:admin"))
                .setRefreshPolicy(IMMEDIATE)
                .get()
                .hasFailures()
        );

        privileges = new GetPrivilegesRequestBuilder(client).application("app-2").privileges("read").get().privileges();

        assertEquals(1, privileges.length);

        privileges = new GetPrivilegesRequestBuilder(client).application("app-2").get().privileges();

        assertEquals(3, privileges.length);

        final ClearPrivilegesCacheResponse clearPrivilegesCacheResponse = client.execute(
            ClearPrivilegesCacheAction.INSTANCE,
            new ClearPrivilegesCacheRequest()
        ).actionGet();
        assertFalse(clearPrivilegesCacheResponse.hasFailures());

        privileges = new GetPrivilegesRequestBuilder(client).application("app-2").privileges("read").get().privileges();
        assertEquals(0, privileges.length);
    }

    public void testPopulationOfCacheWhenLoadingPrivilegesForAllApplications() {
        final Client client = client();

        ApplicationPrivilegeDescriptor[] privileges = new GetPrivilegesRequestBuilder(client).get().privileges();

        assertEquals(6, privileges.length);

        deleteApplicationPrivilege("app-2", "read");

        assertEquals(0, new GetPrivilegesRequestBuilder(client).application("app-2").privileges("read").get().privileges().length);

        assertEquals(5, new GetPrivilegesRequestBuilder(client).get().privileges().length);

        addApplicationPrivilege("app-2", "read", "r:e:f:g", "r:t:u:v");

        assertEquals(6, new GetPrivilegesRequestBuilder(client).get().privileges().length);

        deleteApplicationPrivilege("app-2", "read");

        assertFalse(
            client.prepareBulk()
                .add(new DeleteRequest(SECURITY_MAIN_ALIAS, DOC_TYPE_VALUE + "_app-1:write"))
                .add(new DeleteRequest(SECURITY_MAIN_ALIAS, DOC_TYPE_VALUE + "_app-2:write"))
                .setRefreshPolicy(IMMEDIATE)
                .get()
                .hasFailures()
        );

        assertEquals(0, new GetPrivilegesRequestBuilder(client).application("app-2").privileges("write").get().privileges().length);

        assertEquals(1, new GetPrivilegesRequestBuilder(client).application("app-1").privileges("write").get().privileges().length);
    }

    public void testSuffixWildcard() {
        final Client client = client();

        assertEquals(6, new GetPrivilegesRequestBuilder(client).application("app-*").get().privileges().length);

        assertEquals(
            RestStatus.OK,
            client.prepareDelete(SECURITY_MAIN_ALIAS, DOC_TYPE_VALUE + "_app-1:read").setRefreshPolicy(IMMEDIATE).get().status()
        );

        assertEquals(1, new GetPrivilegesRequestBuilder(client).application("app-1").privileges("read").get().privileges().length);
    }

    public void testHasPrivileges() {
        assertTrue(
            checkPrivilege("app-1", "read").getApplicationPrivileges()
                .get("app-1")
                .stream()
                .findFirst()
                .orElseThrow()
                .getPrivileges()
                .get("read")
        );

        assertFalse(
            checkPrivilege("app-1", "check").getApplicationPrivileges()
                .get("app-1")
                .stream()
                .findFirst()
                .orElseThrow()
                .getPrivileges()
                .get("check")
        );

        addApplicationPrivilege("app-1", "check", "c:a:b:c");
        assertTrue(
            checkPrivilege("app-1", "check").getApplicationPrivileges()
                .get("app-1")
                .stream()
                .findFirst()
                .orElseThrow()
                .getPrivileges()
                .get("check")
        );

        deleteApplicationPrivilege("app-1", "read");
        assertFalse(
            checkPrivilege("app-1", "read").getApplicationPrivileges()
                .get("app-1")
                .stream()
                .findFirst()
                .orElseThrow()
                .getPrivileges()
                .get("read")
        );

        assertTrue(
            checkPrivilege("app-2", "check").getApplicationPrivileges()
                .get("app-2")
                .stream()
                .findFirst()
                .orElseThrow()
                .getPrivileges()
                .get("check")
        );
    }

    @AwaitsFix(bugUrl = "https:
    public void testRolesCacheIsClearedWhenPrivilegesIsChanged() {
        final Client client = client();

        final String testRole = "test_role_cache_role";
        final String testRoleCacheUser = "test_role_cache_user";
        final PutRoleResponse putRoleResponse = new PutRoleRequestBuilder(client).name(testRole)
            .cluster("all")
            .addIndices(new String[] { "*" }, new String[] { "read" }, null, null, null, false)
            .get();
        assertTrue(putRoleResponse.isCreated());
        final Hasher hasher = getFastStoredHashAlgoForTests();
        final PutUserResponse putUserResponse = new PutUserRequestBuilder(client).username(testRoleCacheUser)
            .roles(testRole)
            .password(new SecureString("longerpassword".toCharArray()), hasher)
            .get();
        assertTrue(putUserResponse.created());

        final Client testRoleCacheUserClient = client.filterWithHeader(
            singletonMap(
                "Authorization",
                "Basic " + Base64.getEncoder().encodeToString((testRoleCacheUser + ":longerpassword").getBytes(StandardCharsets.UTF_8))
            )
        );
        new ClusterHealthRequestBuilder(testRoleCacheUserClient).get();

        final DeleteResponse deleteResponse = client.prepareDelete(SECURITY_MAIN_ALIAS, "role-" + testRole).get();
        assertEquals(DocWriteResponse.Result.DELETED, deleteResponse.getResult());

        new ClusterHealthRequestBuilder(testRoleCacheUserClient).get();

        if (randomBoolean()) {
            deleteApplicationPrivilege("app-1", "read");
        } else {
            addApplicationPrivilege("app-3", "read", "r:q:r:s");
        }
        expectThrows(ElasticsearchSecurityException.class, () -> new ClusterHealthRequestBuilder(testRoleCacheUserClient).get());
    }

    private HasPrivilegesResponse checkPrivilege(String applicationName, String privilegeName) {
        final Client client = client().filterWithHeader(
            singletonMap(
                "Authorization",
                "Basic " + Base64.getEncoder().encodeToString(("app_user:" + TEST_PASSWORD).getBytes(StandardCharsets.UTF_8))
            )
        );

        final HasPrivilegesRequest hasPrivilegesRequest = new HasPrivilegesRequest();
        hasPrivilegesRequest.username(APP_USER_NAME);
        hasPrivilegesRequest.applicationPrivileges(
            RoleDescriptor.ApplicationResourcePrivileges.builder()
                .application(applicationName)
                .privileges(privilegeName)
                .resources("foo")
                .build()
        );
        hasPrivilegesRequest.clusterPrivileges("monitor");
        hasPrivilegesRequest.indexPrivileges(RoleDescriptor.IndicesPrivileges.builder().indices("*").privileges("read").build());
        return client.execute(HasPrivilegesAction.INSTANCE, hasPrivilegesRequest).actionGet();
    }

    private void addApplicationPrivilege(String applicationName, String privilegeName, String... actions) {
        final List<ApplicationPrivilegeDescriptor> applicationPrivilegeDescriptors = Collections.singletonList(
            new ApplicationPrivilegeDescriptor(applicationName, privilegeName, Set.of(actions), emptyMap())
        );
        final PutPrivilegesRequest putPrivilegesRequest = new PutPrivilegesRequest();
        putPrivilegesRequest.setPrivileges(applicationPrivilegeDescriptors);
        assertEquals(1, client().execute(PutPrivilegesAction.INSTANCE, putPrivilegesRequest).actionGet().created().keySet().size());
    }

    private void deleteApplicationPrivilege(String applicationName, String privilegeName) {
        assertEquals(
            singleton(privilegeName),
            new DeletePrivilegesRequestBuilder(client()).application(applicationName)
                .privileges(new String[] { privilegeName })
                .get()
                .found()
        );
    }
}
