/*
 * Copyright (c) 2018, 2019 Oracle and/or its affiliates. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.quickstart.se;

import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;
import io.opentelemetry.trace.Span;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * A simple service to greet you. Examples:
 *
 * Get default greeting message:
 * curl -X GET http://localhost:8080/greet
 *
 * Get greeting message for Joe:
 * curl -X GET http://localhost:8080/greet/Joe
 *
 * Change greeting
 * curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 *
 * The message is returned as a JSON object
 */

public class TraceDemoService implements Service {

    /**
     * The config value for the key {@code greeting}.
     */
    private final AtomicReference<String> greeting = new AtomicReference<>();

    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    private static final Logger LOGGER = Logger.getLogger(TraceDemoService.class.getName());

    TraceDemoService(Config config) {
        greeting.set(config.get("app.tgreeeting").asString().orElse("Ciao"));
    }

    /**
     * A service registers itself by updating the routing rules.
     * @param rules the routing rules.
     */
    @Override
    public void update(Routing.Rules rules) {
        rules
            .get("/", this::getDefaultMessageHandler)
            .get("/route1", this::getRoute1)
            .get("/route2", this::getRoute2);
    }

    /**
     * Do some work
     * @param request the server request
     * @param response the server response
     */
    private void getRoute1(ServerRequest request,
                            ServerResponse response) {
        Span span = OpenTelemetryService.startSpan("/demo/route1", request);
        try { Thread.sleep(randomize()); } catch (InterruptedException e) {}
        String eventAttribute = sendResponse(response, "route1");
        OpenTelemetryService.addEvent(span, eventAttribute);
        OpenTelemetryService.endSpan(span);
    }

    private void getRoute2(ServerRequest request,
                           ServerResponse response) {
        Span span = OpenTelemetryService.startSpan("/demo/route2", request);
        try { Thread.sleep(randomize()); } catch (InterruptedException e) {}
        String eventAttribute = sendResponse(response, "route2");
        OpenTelemetryService.addEvent(span, eventAttribute);
        OpenTelemetryService.endSpan(span);
    }

    private String sendResponse(ServerResponse response, String name) {
        String msg = String.format("%s %s!", greeting.get(), name);

        JsonObject returnObject = JSON.createObjectBuilder()
                .add("message", msg)
                .build();
        response.send(returnObject);
        return returnObject.toString();
    }

    private void getDefaultMessageHandler(ServerRequest request,
                                          ServerResponse response) {
        sendResponse(response, "Traces");
    }

    private int randomize() {
        Random r = new Random();
        int randomInt = r.nextInt(5) + 1;
        System.out.println("--- sleeping " + randomInt + " seconds ...");
        return randomInt * 1000;
    }

}
