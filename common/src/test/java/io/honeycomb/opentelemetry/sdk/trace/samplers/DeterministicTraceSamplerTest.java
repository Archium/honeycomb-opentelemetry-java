package io.honeycomb.opentelemetry.sdk.trace.samplers;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.IdGenerator;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

public class DeterministicTraceSamplerTest {

    @Test
    public void test_neverSample() {
        int sampleRate = 0;
        Sampler sampler = new DeterministicTraceSampler(Sampler.alwaysOn(), sampleRate);

        for (int i = 0; i < 100; i++) {
            String traceID = IdGenerator.random().generateTraceId();
            SamplingResult result = sampler.shouldSample(Context.current(), traceID, "span", SpanKind.CLIENT, Attributes.empty(), Collections.emptyList());
            Assertions.assertEquals(SamplingDecision.DROP, result.getDecision());
        }
    }

    @Test
    public void test_alwaysSample() {
        int sampleRate = 1;
        Sampler sampler = new DeterministicTraceSampler(Sampler.alwaysOn(), sampleRate);

        for (int i = 0; i < 100; i++) {
            String traceID = IdGenerator.random().generateTraceId();
            SamplingResult result = sampler.shouldSample(Context.current(), traceID, "span", SpanKind.CLIENT, Attributes.empty(), Collections.emptyList());
            Assertions.assertEquals(SamplingDecision.RECORD_AND_SAMPLE, result.getDecision());
        }
    }

    @Test
    public void test_samplingResult_has_sampleRate_attribute() {

        for (int i = 0; i < 10; i++) {
            Sampler sampler = new DeterministicTraceSampler(Sampler.alwaysOn(), i);
            String traceID = IdGenerator.random().generateTraceId();
            SamplingResult result = sampler.shouldSample(Context.current(), traceID, "span", SpanKind.CLIENT, Attributes.empty(), Collections.emptyList());

            Assertions.assertEquals(
                result.getAttributes(),
                Attributes.of(AttributeKey.longKey("SampleRate"), (long) i)
            );
        }
    }

    @Test
    public void test_deterministicSampler_varies_sampling_decision_based_on_trace_id() {
        int count = 0;
        for (int i = 0; i < 100; i++) {
            Sampler sampler = new DeterministicTraceSampler(Sampler.alwaysOn(), 2);
            String traceID = IdGenerator.random().generateTraceId();
            SamplingResult result = sampler.shouldSample(Context.current(), traceID, "span", SpanKind.CLIENT, Attributes.empty(), Collections.emptyList());
            if (result.getDecision() == SamplingDecision.RECORD_AND_SAMPLE) {
                count++;
            }
        }

        Assertions.assertTrue(count > 25 && count < 75);
    }

    @Test
    public void test_delegatesToDecoratedSampler() {
        int sampleRate = 1;
        Sampler sampler = new DeterministicTraceSampler(Sampler.alwaysOff(), sampleRate);

        for (int i = 0; i < 100; i++) {
            String traceID = IdGenerator.random().generateTraceId();
            SamplingResult result = sampler.shouldSample(Context.current(), traceID, "span", SpanKind.CLIENT, Attributes.empty(), Collections.emptyList());
            Assertions.assertEquals(SamplingDecision.DROP, result.getDecision());
        }
    }

    @Test
    public void test_includesAttributesOfDecoratedSampler() {
        AttributeKey<String> attributeKey = AttributeKey.stringKey("test.attribute.name");
        String attributeValue = "test.attribute.value";
        Attributes attributes = Attributes.of(attributeKey, attributeValue);
        SamplingResult decoratorResult = SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE, attributes);
        Sampler decoratedSampler = new Sampler() {
            @Override
            public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
                return decoratorResult;
            }

            @Override
            public String getDescription() {return "test_includesAttributesOfDecoratedSampler()";}
        };

        int sampleRate = 1;
        Sampler sampler = new DeterministicTraceSampler(decoratedSampler, sampleRate);

        for (int i = 0; i < 100; i++) {
            String traceID = IdGenerator.random().generateTraceId();
            SamplingResult result = sampler.shouldSample(Context.current(), traceID, "span", SpanKind.CLIENT, Attributes.empty(), Collections.emptyList());
            Assertions.assertEquals(result.getAttributes().get(attributeKey), attributeValue);
        }
    }
}
