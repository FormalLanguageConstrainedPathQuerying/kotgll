/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.watcher.notification;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.SecureSettings;
import org.elasticsearch.common.settings.SecureString;
import org.elasticsearch.common.settings.Setting;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.common.util.LazyInitializable;
import org.elasticsearch.core.Nullable;
import org.elasticsearch.core.Tuple;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Basic notification service
 */
public abstract class NotificationService<Account> {

    private final String type;
    private final Logger logger;
    private final Settings bootSettings;
    private final List<Setting<?>> pluginSecureSettings;
    private volatile Map<String, LazyInitializable<Account, SettingsException>> accounts;
    private volatile LazyInitializable<Account, SettingsException> defaultAccount;
    private volatile Settings cachedClusterSettings;
    private volatile SecureSettings cachedSecureSettings;

    @SuppressWarnings("this-escape")
    public NotificationService(
        String type,
        Settings settings,
        ClusterSettings clusterSettings,
        List<Setting<?>> pluginDynamicSettings,
        List<Setting<?>> pluginSecureSettings
    ) {
        this(type, settings, pluginSecureSettings);
        clusterSettings.addSettingsUpdateConsumer(this::clusterSettingsConsumer, pluginDynamicSettings);
    }

    NotificationService(String type, Settings settings, List<Setting<?>> pluginSecureSettings) {
        this.type = type;
        this.logger = LogManager.getLogger(NotificationService.class);
        this.bootSettings = settings;
        this.pluginSecureSettings = pluginSecureSettings;
    }

    protected synchronized void clusterSettingsConsumer(Settings settings) {
        this.cachedClusterSettings = settings;
        buildAccounts();
    }

    public synchronized void reload(Settings settings) {
        try {
            this.cachedSecureSettings = extractSecureSettings(settings, pluginSecureSettings);
        } catch (GeneralSecurityException e) {
            logger.error("Keystore exception while reloading watcher notification service", e);
            return;
        }
        buildAccounts();
    }

    private void buildAccounts() {
        final Settings.Builder completeSettingsBuilder = Settings.builder().put(bootSettings, false);
        if (this.cachedClusterSettings != null) {
            completeSettingsBuilder.put(this.cachedClusterSettings, false);
        }
        if (this.cachedSecureSettings != null) {
            completeSettingsBuilder.setSecureSettings(this.cachedSecureSettings);
        }
        final Settings completeSettings = completeSettingsBuilder.build();
        final Set<String> accountNames = getAccountNames(completeSettings);
        this.accounts = createAccounts(completeSettings, accountNames, (name, accountSettings) -> createAccount(name, accountSettings));
        this.defaultAccount = findDefaultAccountOrNull(completeSettings, this.accounts);
    }

    protected abstract Account createAccount(String name, Settings accountSettings);

    public Account getAccount(String name) {
        final Map<String, LazyInitializable<Account, SettingsException>> accounts;
        final LazyInitializable<Account, SettingsException> defaultAccount;
        synchronized (this) { 
            accounts = this.accounts;
            defaultAccount = this.defaultAccount;
        }
        LazyInitializable<Account, SettingsException> theAccount = accounts.getOrDefault(name, defaultAccount);
        if (theAccount == null && name == null) {
            throw new IllegalArgumentException(
                "no accounts of type ["
                    + type
                    + "] configured. "
                    + "Please set up an account using the [xpack.notification."
                    + type
                    + "] settings"
            );
        }
        if (theAccount == null) {
            throw new IllegalArgumentException("no account found for name: [" + name + "]");
        }
        return theAccount.getOrCompute();
    }

    private String getNotificationsAccountPrefix() {
        return "xpack.notification." + type + ".account.";
    }

    private Set<String> getAccountNames(Settings settings) {
        return settings.getByPrefix(getNotificationsAccountPrefix()).names();
    }

    @Nullable
    protected String getDefaultAccountName(Settings settings) {
        return settings.get("xpack.notification." + type + ".default_account");
    }

    protected Map<String, LazyInitializable<Account, SettingsException>> createAccounts(
        Settings settings,
        Set<String> accountNames,
        BiFunction<String, Settings, Account> accountFactory
    ) {
        final Map<String, LazyInitializable<Account, SettingsException>> accounts = new HashMap<>();
        for (final String accountName : accountNames) {
            final Settings accountSettings = settings.getAsSettings(getNotificationsAccountPrefix() + accountName);
            accounts.put(accountName, new LazyInitializable<>(() -> accountFactory.apply(accountName, accountSettings)));
        }
        return Collections.unmodifiableMap(accounts);
    }

    private @Nullable LazyInitializable<Account, SettingsException> findDefaultAccountOrNull(
        Settings settings,
        Map<String, LazyInitializable<Account, SettingsException>> accounts
    ) {
        final String defaultAccountName = getDefaultAccountName(settings);
        if (defaultAccountName == null) {
            if (accounts.isEmpty()) {
                return null;
            } else {
                return accounts.values().iterator().next();
            }
        } else {
            final LazyInitializable<Account, SettingsException> account = accounts.get(defaultAccountName);
            if (account == null) {
                throw new SettingsException("could not find default account [" + defaultAccountName + "]");
            }
            return account;
        }
    }

    /**
     * Extracts the {@link SecureSettings}` out of the passed in {@link Settings} object. The {@code Setting} argument has to have the
     * {@code SecureSettings} open/available. Normally {@code SecureSettings} are available only under specific callstacks (eg. during node
     * initialization or during a `reload` call). The returned copy can be reused freely as it will never be closed (this is a bit of
     * cheating, but it is necessary in this specific circumstance). Only works for secure settings of type string (not file).
     *
     * @param source
     *            A {@code Settings} object with its {@code SecureSettings} open/available.
     * @param securePluginSettings
     *            The list of settings to copy.
     * @return A copy of the {@code SecureSettings} of the passed in {@code Settings} argument.
     */
    private static SecureSettings extractSecureSettings(Settings source, List<Setting<?>> securePluginSettings)
        throws GeneralSecurityException {
        final SecureSettings sourceSecureSettings = Settings.builder().put(source, true).getSecureSettings();
        final Map<String, Tuple<SecureString, byte[]>> cache = new HashMap<>();
        if (sourceSecureSettings != null && securePluginSettings != null) {
            for (final String settingKey : sourceSecureSettings.getSettingNames()) {
                for (final Setting<?> secureSetting : securePluginSettings) {
                    if (secureSetting.match(settingKey)) {
                        cache.put(
                            settingKey,
                            new Tuple<>(sourceSecureSettings.getString(settingKey), sourceSecureSettings.getSHA256Digest(settingKey))
                        );
                    }
                }
            }
        }
        return new SecureSettings() {
            @Override
            public boolean isLoaded() {
                return true;
            }

            @Override
            public SecureString getString(String setting) {
                return cache.get(setting).v1();
            }

            @Override
            public Set<String> getSettingNames() {
                return cache.keySet();
            }

            @Override
            public InputStream getFile(String setting) {
                throw new IllegalStateException("A NotificationService setting cannot be File.");
            }

            @Override
            public byte[] getSHA256Digest(String setting) {
                return cache.get(setting).v2();
            }

            @Override
            public void close() throws IOException {}

            @Override
            public void writeTo(StreamOutput out) throws IOException {
                throw new IllegalStateException("Unsupported operation");
            }
        };
    }
}
