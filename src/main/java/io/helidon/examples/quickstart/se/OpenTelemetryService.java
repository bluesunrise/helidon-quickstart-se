package io.helidon.examples.quickstart.se;

import io.grpc.Context;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.helidon.webserver.ServerRequest;
import io.opentelemetry.OpenTelemetry;
import io.opentelemetry.common.AttributeValue;
import io.opentelemetry.context.ContextUtils;
import io.opentelemetry.context.Scope;
import io.opentelemetry.exporters.jaeger.JaegerGrpcSpanExporter;
import io.opentelemetry.exporters.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.TracerSdkProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpansProcessor;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.Status;
import io.opentelemetry.trace.Tracer;

import java.util.HashMap;
import java.util.Map;

public class OpenTelemetryService {

    private static Tracer tracer = null;

    public static void init() {
        tracer = OpenTelemetry.getTracerProvider().get("groundwork-instrumentation","semver:1.0.0");
        LoggingSpanExporter loggingExporter = new LoggingSpanExporter();
        TracerSdkProvider tracerProvider = OpenTelemetrySdk.getTracerProvider();
        tracerProvider.addSpanProcessor(SimpleSpansProcessor.create(loggingExporter));
        setupJaegerExporter();
    }

    public static Span startSpan(String spanName, ServerRequest request) {
        Span.Builder spanBuilder = tracer.spanBuilder(spanName).setSpanKind(Span.Kind.SERVER);
        Span span = null;
        Context ctx = OpenTelemetry.getPropagators().getHttpTextFormat().extract(Context.current(), request, HelidonHeaderExtractor.getter);
        try (Scope scope = ContextUtils.withScopedContext(ctx)) {
            // Build a span automatically using the received context
            span = spanBuilder.startSpan();
        }
        // Set the Semantic Convention
        span.setAttribute("component", "http");
        span.setAttribute("http.method", "GET");
      /*
      One of the following is required:
      - http.scheme, http.host, http.target
      - http.scheme, http.server_name, net.host.port, http.target
      - http.scheme, net.host.name, net.host.port, http.target
      - http.url
      */
        span.setAttribute("http.scheme", "http");
        span.setAttribute("http.host", "localhost:" + request.remotePort());
        span.setAttribute("http.target", spanName);
        return span;
    }

    public static void addEvent(Span span, String eventAttribute) {
        Map<String, AttributeValue> event = new HashMap<>();
        event.put("answer", AttributeValue.stringAttributeValue(eventAttribute));
        span.addEvent("Finish Processing", event);
        span.setStatus(Status.OK);

    }

    public static void endSpan(Span span) {
        span.end();
    }

    public static void setupJaegerExporter() {
        // Create a channel towards Jaeger end point
        ManagedChannel jaegerChannel =
                ManagedChannelBuilder.forAddress("localhost", 14250).usePlaintext().build();
        // Export traces to Jaeger
        JaegerGrpcSpanExporter jaegerExporter =
                JaegerGrpcSpanExporter.newBuilder()
                        .setServiceName("demoExporter")
                        .setChannel(jaegerChannel)
                        .setDeadlineMs(30000)
                        .build();

        // Set to process the spans by the Jaeger Exporter
        OpenTelemetrySdk.getTracerProvider().addSpanProcessor(SimpleSpansProcessor.create(jaegerExporter));
    }

}
