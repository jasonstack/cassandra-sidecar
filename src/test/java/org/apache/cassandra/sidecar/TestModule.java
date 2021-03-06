/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.cassandra.sidecar;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.LoggerHandler;
import org.apache.cassandra.sidecar.mocks.MockHealthCheck;
import org.apache.cassandra.sidecar.routes.HealthService;

/**
 * Provides the basic dependencies for unit tests.
 */
public class TestModule extends AbstractModule
{
    private Vertx vertx;

    public TestModule(Vertx vertx)
    {
        this.vertx = vertx;
    }

    @Provides
    @Singleton
    public Vertx getVertx()
    {
        return vertx;
    }

    @Provides
    @Singleton
    public HealthService healthService(Configuration config, MockHealthCheck check)
    {
        return new HealthService(config, check, null);
    }

    @Provides
    @Singleton
    public MockHealthCheck healthCheck()
    {
        return new MockHealthCheck();
    }

    @Provides
    @Singleton
    public HttpServer vertxServer(Vertx vertx, Router router, Configuration conf)
    {
        HttpServerOptions options = new HttpServerOptions().setLogActivity(true);
        options.setKeyStoreOptions(new JksOptions()
                                       .setPath(conf.getKeyStorePath())
                                       .setPassword(conf.getKeystorePassword()))
                   .setSsl(conf.isSslEnabled());
        HttpServer server = vertx.createHttpServer(options);
        server.requestHandler(router);
        return server;
    }

    @Provides
    @Singleton
    public Router vertxRouter(Vertx vertx, HealthService healthService)
    {
        Router router = Router.router(vertx);
        router.route().handler(LoggerHandler.create());
        router.route().path("/api/v1/__health").handler(healthService::handleHealth);
        return router;
    }

    @Provides
    @Singleton
    public Configuration configuration()
    {
        return abstractConfig();
    }

    protected Configuration abstractConfig()
    {
        return new Configuration.Builder()
                           .setCassandraHost("INVALID_FOR_TEST")
                           .setCassandraPort(0)
                           .setHost("127.0.0.1")
                           .setPort(6475)
                           .setHealthCheckFrequency(1000)
                           .setSslEnabled(false)
                           .build();
    }
}
