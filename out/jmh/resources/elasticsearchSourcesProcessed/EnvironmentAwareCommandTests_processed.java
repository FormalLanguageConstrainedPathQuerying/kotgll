/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.common.cli;

import joptsimple.OptionSet;

import org.elasticsearch.Build;
import org.elasticsearch.cli.Command;
import org.elasticsearch.cli.CommandTestCase;
import org.elasticsearch.cli.ProcessInfo;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.junit.Before;

import java.util.function.Consumer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class EnvironmentAwareCommandTests extends CommandTestCase {

    private Build.Type buildType;
    private Consumer<Environment> callback;

    @Before
    public void resetHooks() {
        buildType = Build.Type.TAR;
        callback = null;
    }

    @Override
    protected Command newCommand() {
        return new EnvironmentAwareCommand("test command") {
            @Override
            public void execute(Terminal terminal, OptionSet options, Environment env, ProcessInfo processInfo) {
                if (callback != null) {
                    callback.accept(env);
                }
            }

            @Override
            protected Build.Type getBuildType() {
                return buildType;
            }
        };
    }

    public void testNonDockerEnvVarSettingsIgnored() throws Exception {
        envVars.put("ES_SETTING_FOO_BAR", "baz");
        envVars.put("some.setting", "1");
        callback = env -> {
            Settings settings = env.settings();
            assertThat(settings.hasValue("foo.bar"), is(false));
            assertThat(settings.hasValue("some.settings"), is(false));
        };
        execute();
    }

    public void testDockerEnvVarSettingsIgnored() throws Exception {
        envVars.put("XPACK_SECURITY_FIPS__MODE_ENABLED", "false");
        envVars.put("ES_XPACK_SECURITY_FIPS__MODE_ENABLED", "false");
        envVars.put("ES.SETTING.XPACK.SECURITY.FIPS_MODE.ENABLED", "false");
        envVars.put("es_setting_xpack_security_fips__mode_enabled", "false");
        envVars.put("singleword", "value");
        envVars.put("setting.Ignored", "value");
        callback = env -> {
            Settings settings = env.settings();
            assertThat(settings.hasValue("xpack.security.fips_mode.enabled"), is(false));
            assertThat(settings.hasValue("singleword"), is(false));
            assertThat(settings.hasValue("setting.Ignored"), is(false));
            assertThat(settings.hasValue("setting.ignored"), is(false));
        };
        execute();
    }

    public void testDockerEnvVarSettingsTranslated() throws Exception {
        buildType = Build.Type.DOCKER;
        envVars.put("ES_SETTING_SIMPLE_SETTING", "value");
        envVars.put("ES_SETTING_UNDERSCORE__HERE", "value");
        envVars.put("ES_SETTING_UNDERSCORE__DOT_BAZ", "value");
        envVars.put("ES_SETTING_DOUBLE____UNDERSCORE", "value");
        envVars.put("ES_SETTING_TRIPLE___BAZ", "value");
        envVars.put("lowercase.setting", "value");
        callback = env -> {
            Settings settings = env.settings();
            assertThat(settings.get("simple.setting"), equalTo("value"));
            assertThat(settings.get("underscore_here"), equalTo("value"));
            assertThat(settings.get("underscore_dot.baz"), equalTo("value"));
            assertThat(settings.get("triple_.baz"), equalTo("value"));
            assertThat(settings.get("double__underscore"), equalTo("value"));
            assertThat(settings.get("lowercase.setting"), equalTo("value"));
        };
        execute();
    }

    public void testDockerEnvVarSettingsOverrideCommandLine() throws Exception {
        buildType = Build.Type.DOCKER;
        envVars.put("ES_SETTING_SIMPLE_SETTING", "override");
        callback = env -> {
            Settings settings = env.settings();
            assertThat(settings.get("simple.setting"), equalTo("override"));
        };
        execute("-Esimple.setting=original");
    }
}
