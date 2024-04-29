/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.idp.saml.sp;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.script.ScriptModule;
import org.elasticsearch.script.ScriptService;
import org.elasticsearch.script.mustache.MustacheScriptEngine;
import org.elasticsearch.xcontent.XContentType;
import org.elasticsearch.xpack.idp.saml.test.IdpSamlTestCase;
import org.junit.Before;
import org.opensaml.saml.saml2.core.NameID;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class WildcardServiceProviderResolverTests extends IdpSamlTestCase {

    private static final String SERVICES_JSON = """
        {
          "services": {
            "service1a": {
              "entity_id": "https:
              "acs": "https:
              "tokens": [ "service" ],
              "template": {
                "name": "{{service}} at example.com (A)",
                "privileges": {
                  "resource": "service1:example:{{service}}",
                  "roles": [ "sso:(.*)" ]
                },
                "attributes": {
                  "principal": "http:
                  "name": "http:
                  "email": "http:
                  "roles": "http:
                }
              }
            },
            "service1b": {
              "entity_id": "https:
              "acs": "https:
              "tokens": [ "service" ],
              "template": {
                "name": "{{service}} at example.com (B)",
                "privileges": {
                  "resource": "service1:example:{{service}}",
                  "roles": [ "sso:(.*)" ]
                },
                "attributes": {
                  "principal": "http:
                  "name": "http:
                  "email": "http:
                  "roles": "http:
                }
              }
            },
            "service2": {
              "entity_id": "https:
              "acs": "https:
              "tokens": [ "id" ],
              "template": {
                "name": "{{id}} at example.net",
                "privileges": {
                  "resource": "service2:example:{{id}}",
                  "roles": [ "sso:(.*)" ]
                },
                "attributes": {
                  "principal": "http:
                  "name": "http:
                  "email": "http:
                  "roles": "http:
                }
              }
            }
          }
        }"""; 
    private WildcardServiceProviderResolver resolver;

    @Before
    public void setUpResolver() {
        final Settings settings = Settings.EMPTY;
        final ScriptService scriptService = new ScriptService(
            settings,
            Collections.singletonMap(MustacheScriptEngine.NAME, new MustacheScriptEngine()),
            ScriptModule.CORE_CONTEXTS,
            () -> 1L
        );
        final ServiceProviderDefaults samlDefaults = new ServiceProviderDefaults("elastic-cloud", NameID.TRANSIENT, Duration.ofMinutes(15));
        resolver = new WildcardServiceProviderResolver(settings, scriptService, new SamlServiceProviderFactory(samlDefaults));
    }

    public void testParsingOfServices() throws IOException {
        loadJsonServices();
        assertThat(resolver.services().keySet(), containsInAnyOrder("service1a", "service1b", "service2"));

        final WildcardServiceProvider service1a = resolver.services().get("service1a");
        assertThat(
            service1a.extractTokens("https:
            equalTo(
                Map.ofEntries(
                    Map.entry("service", "abcdef"),
                    Map.entry("entity_id", "https:
                    Map.entry("acs", "https:
                )
            )
        );
        expectThrows(
            IllegalArgumentException.class,
            () -> service1a.extractTokens("https:
        );
        assertThat(service1a.extractTokens("urn:foo:bar", "https:
        assertThat(service1a.extractTokens("https:

        final WildcardServiceProvider service1b = resolver.services().get("service1b");
        assertThat(
            service1b.extractTokens("https:
            equalTo(
                Map.ofEntries(
                    Map.entry("service", "xyzzy"),
                    Map.entry("entity_id", "https:
                    Map.entry("acs", "https:
                )
            )
        );
        assertThat(service1b.extractTokens("https:
        expectThrows(
            IllegalArgumentException.class,
            () -> service1b.extractTokens("https:
        );
        assertThat(service1b.extractTokens("urn:foo:bar", "https:
    }

    public void testResolveServices() throws IOException {
        loadJsonServices();

        final SamlServiceProvider sp1 = resolver.resolve("https:

        assertThat(sp1, notNullValue());
        assertThat(sp1.getEntityId(), equalTo("https:
        assertThat(sp1.getAssertionConsumerService().toString(), equalTo("https:
        assertThat(sp1.getName(), equalTo("abcdef at example.com (A)"));
        assertThat(sp1.getPrivileges().getResource(), equalTo("service1:example:abcdef"));

        final SamlServiceProvider sp2 = resolver.resolve("https:
        assertThat(sp2, notNullValue());
        assertThat(sp2.getEntityId(), equalTo("https:
        assertThat(sp2.getAssertionConsumerService().toString(), equalTo("https:
        assertThat(sp2.getName(), equalTo("qwerty at example.com (A)"));
        assertThat(sp2.getPrivileges().getResource(), equalTo("service1:example:qwerty"));

        final SamlServiceProvider sp3 = resolver.resolve("https:
        assertThat(sp3, notNullValue());
        assertThat(sp3.getEntityId(), equalTo("https:
        assertThat(sp3.getAssertionConsumerService().toString(), equalTo("https:
        assertThat(sp3.getName(), equalTo("xyzzy at example.com (B)"));
        assertThat(sp3.getPrivileges().getResource(), equalTo("service1:example:xyzzy"));

        final SamlServiceProvider sp4 = resolver.resolve("https:
        assertThat(sp4, notNullValue());
        assertThat(sp4.getEntityId(), equalTo("https:
        assertThat(sp4.getAssertionConsumerService().toString(), equalTo("https:
        assertThat(sp4.getName(), equalTo("12345 at example.net"));
        assertThat(sp4.getPrivileges().getResource(), equalTo("service2:example:12345"));

        expectThrows(
            IllegalArgumentException.class,
            () -> resolver.resolve("https:
        );
    }

    public void testCaching() throws IOException {
        loadJsonServices();

        final String serviceName = randomAlphaOfLengthBetween(4, 12);
        final String entityId = "https:
        final String acs = randomBoolean()
            ? "https:
            : "https:

        final SamlServiceProvider original = resolver.resolve(entityId, acs);
        for (int i = randomIntBetween(10, 20); i > 0; i--) {
            final SamlServiceProvider cached = resolver.resolve(entityId, acs);
            assertThat(cached, sameInstance(original));
        }
    }

    private void loadJsonServices() throws IOException {
        assertThat("Resolver has not been setup correctly", resolver, notNullValue());
        resolver.reload(createParser(XContentType.JSON.xContent(), SERVICES_JSON));
    }
}
