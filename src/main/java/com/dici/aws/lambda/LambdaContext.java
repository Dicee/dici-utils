package com.dici.aws.lambda;

import com.dici.time.TimeUtils;
import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.ThreadContext;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static com.dici.time.TimeUtils.logFriendlyDuration;

@Log4j2
@RequiredArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
public class LambdaContext {
    public static final LambdaContext DEFAULT = new LambdaContext(System.getenv());

    private static final String LAMBDA_ID_KEY = "LambdaId";
    // allows knowing which Lambda instance logged a given line. This can come handy at times (for example it helps detecting JVM crashes)
    private static final String LAMBDA_ID = UUID.randomUUID().toString().substring(0, 8);
    // https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html
    private static final String AWS_LAMBDA_INITIALIZATION_TYPE = "AWS_LAMBDA_INITIALIZATION_TYPE";
    private static final String AWS_LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME";

    @NonNull private final Map<String, String> env;

    public boolean isRunningInLambda() {
        return env.get(AWS_LAMBDA_FUNCTION_NAME) != null;
    }

    /// Puts the Lambda instance's unique id in the thread context and logs the current JVM uptime. This sometimes helps to understand if a request was
    /// executing close to a cold start, which is useful for high latency investigations.
    public void setupLogging() {
        ThreadContext.put(LAMBDA_ID_KEY, LAMBDA_ID);

        Duration jvmUptime = Duration.ofMillis(ManagementFactory.getRuntimeMXBean().getUptime());
        log.info("JVM uptime is {} and initialization type is {}.", logFriendlyDuration(jvmUptime), getInitializationType());
    }

    /// Detects whether the lambda is booting as part of provisioned concurrency (before receiving traffic) or actually executing a request
    /// (on-demand). This is useful to understand if the Lambda is already warmed up or not.
    private LambdaInitializationType getInitializationType() {
        String type = env.get(AWS_LAMBDA_INITIALIZATION_TYPE);
        return switch (type) {
            case "on-demand" -> LambdaInitializationType.ON_DEMAND;
            case "provisioned-concurrency" -> LambdaInitializationType.PROVISIONED_CONCURRENCY;
            default -> {
                log.warn("Could not determine lambda initialization type for [{}]", type);
                yield LambdaInitializationType.UNKNOWN;
            }
        };
    }

    public enum LambdaInitializationType {
        ON_DEMAND,
        PROVISIONED_CONCURRENCY,
        UNKNOWN // in case they add a new value, we don't want to crash just for that
    }
}
