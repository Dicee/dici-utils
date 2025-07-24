package com.dici.service;

import com.dici.service.exception.ExceptionTranslation;
import com.dici.service.exception.ExceptionTranslationConfig;
import com.dici.service.metrics.MetricPublisher;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Logger;

import java.util.function.Function;

/// This class provides template methods for a generic API handler in order to standardize logging,
/// error handling and metric publishing for all API endpoints.
public class HandlerTemplate {
    public static void run(
            String operation,
            MetricPublisher metricsPublisher,
            ExceptionTranslationConfig config,
            Runnable runnable,
            Logger log
    ) {
        invokeFunction(operation, metricsPublisher, config, null, __ -> {
            runnable.run();
            return null;
        }, log);
    }

    public static <I, O> O invokeFunction(
            String operation,
            MetricPublisher metricsPublisher,
            ExceptionTranslationConfig config,
            I input,
            Function<I, O> function,
            Logger log
    ) {
        try {
            return function.apply(input);
        } catch (Throwable t) {
            ExceptionTranslation.Result translationResult = config.findBestTranslation(t);
            String failureMetricName = translationResult.failureMetricName();

            String msg = "Operation %s failed with %s".formatted(operation, failureMetricName);
            log.log(translationResult.logLevel(), msg, translationResult.exception());

            metricsPublisher.publishMetrics(operation, metrics -> {
                metrics.addCount("Failure-" + failureMetricName, 1);
                metrics.addCount("Failures", 1);
            });
            throw translationResult.exception();
        } finally {
            ThreadContext.clearAll();
        }
    }
}
