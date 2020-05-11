package io.helidon.examples.quickstart.se;

import io.helidon.webserver.ServerRequest;
import io.opentelemetry.context.propagation.HttpTextFormat;

public class HelidonHeaderExtractor {

    // Extract the context from helidon headers
    public static HttpTextFormat.Getter<ServerRequest> getter =
            new HttpTextFormat.Getter<ServerRequest>() {
                @Override
                public String get(ServerRequest carrier, String key) {
                    if (carrier.headers().value(key).isPresent()) {
                        return carrier.headers().value(key).get();
                    }
                    return "";
                }
            };
}
