/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.rule;

import android.support.test.InstrumentationRegistry;

import org.junit.rules.TemporaryFolder;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Rule that creates the {@link RealmConfiguration } in a temporary directory and deletes the Realm created with that
 * configuration once the test finishes. Be sure to close all Realm instances before finishing the test. Otherwise
 * {@link Realm#deleteRealm(RealmConfiguration)} will throw an exception in the {@link #after()} method.
 * The temp directory will be deleted regardless if the {@link Realm#deleteRealm(RealmConfiguration)} fails or not.
 */
public class TestRealmConfigurationFactory extends TemporaryFolder {
    private Map<RealmConfiguration, Boolean> map = new ConcurrentHashMap<RealmConfiguration, Boolean>();
    private Set<RealmConfiguration> configurations = Collections.newSetFromMap(map);
    protected boolean unitTestFailed = false;

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                before();
                try {
                    base.evaluate();
                } catch (Throwable throwable) {
                    unitTestFailed = true;
                    throw throwable;
                } finally {
                    after();
                }
            }
        };
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        Realm.init(InstrumentationRegistry.getTargetContext());
    }

    @Override
    protected void after() {
        try {
            for (RealmConfiguration configuration : configurations) {
                Realm.deleteRealm(configuration);
            }
        } catch (IllegalStateException e) {
            // Only throw the exception caused by deleting the opened Realm if the test case itself doesn't throw.
            if (!unitTestFailed) {
                throw e;
            }
        } finally {
            // This will delete the temp directory.
            super.after();
        }
    }

    public RealmConfiguration createConfiguration() {
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .directory(getRoot())
                .build();

        configurations.add(configuration);
        return configuration;
    }
}
