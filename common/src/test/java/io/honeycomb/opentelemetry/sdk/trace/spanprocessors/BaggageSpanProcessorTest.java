package io.honeycomb.opentelemetry.sdk.trace.spanprocessors;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BaggageSpanProcessorTest {

    @Test
    public void test_baggageSpanProcessor_adds_attributes_to_spans(@Mock ReadWriteSpan span) {

        try (BaggageSpanProcessor processor = new BaggageSpanProcessor()) {
            AttributeKey<String> attr = AttributeKey.stringKey("key");
            Baggage.current()
                .toBuilder()
                .put(attr.getKey(), "value")
                .build()
                .makeCurrent();

            processor.onStart(Context.current(), span);
            Mockito.verify(span).setAttribute("key", "value");
        }
    }
}
