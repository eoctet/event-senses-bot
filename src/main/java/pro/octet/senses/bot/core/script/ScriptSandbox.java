package pro.octet.senses.bot.core.script;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.kohsuke.groovy.sandbox.GroovyInterceptor;
import pro.octet.senses.bot.utils.CommonUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * Script sandbox
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public class ScriptSandbox extends GroovyInterceptor implements Serializable {

    private static final List<Class<?>> DENY_TYPES;
    private static final List<String> DENY_PACKAGES;

    static {
        DENY_TYPES = Arrays.asList(
                System.class,
                System.out.getClass(),
                System.err.getClass(),
                System.in.getClass(),
                SystemUtils.class,
                Runnable.class,
                ClassUtils.class,
                Runtime.class,
                ClassLoader.class,
                Class.class
        );

        DENY_PACKAGES = Arrays.asList(
                "pro.octet.senses.bot.",
                "org.apache.flink.",
                "org.apache.commons.cli.",
                "org.apache.commons.io.",
                "org.apache.commons.http.",
                "org.apache.kafka.",
                "org.springframework.",
                "org.checkerframework.",
                "com.google.",
                "com.esotericsoftware.",
                "org.mortbay.",
                "scala.",
                "java.applet.",
                "java.awt.",
                "java.io.",
                "java.nio.file.",
                "javax.",
                "sun.",
                "groovy.",
                "org.codehaus.groovy.",
                "org.kohsuke.",
                "dk.glasius",
                "com.clickhouse.",
                "ru.yandex.clickhouse.",
                "redis.clients."
        );
    }

    private void check(Class<?> clazz) {
        if (DENY_TYPES.contains(clazz)) {
            throw new SecurityException(CommonUtils.format("Illegal script code, Access denied class type -> {0}", clazz));
        }
        DENY_PACKAGES.forEach(p -> {
            if (StringUtils.startsWithIgnoreCase(clazz.getPackage().getName(), p)) {
                throw new SecurityException(CommonUtils.format("Illegal script code, Access denied package path -> {0}", clazz.getPackage().getName()));
            }
        });
    }

    @Override
    public void onSuperConstructor(Invoker invoker, Class receiver, Object... args) throws Throwable {
        check(receiver);
        super.onSuperConstructor(invoker, receiver, args);
    }

    @Override
    public Object onSuperCall(Invoker invoker, Class senderType, Object receiver, String method, Object... args) throws Throwable {
        check(senderType);
        return super.onSuperCall(invoker, senderType, receiver, method, args);
    }

    @Override
    public Object onNewInstance(Invoker invoker, Class receiver, Object... args) throws Throwable {
        check(receiver);
        return super.onNewInstance(invoker, receiver, args);
    }

    @Override
    public Object onStaticCall(Invoker invoker, Class receiver, String method, Object... args) throws Throwable {
        check(receiver);
        return super.onStaticCall(invoker, receiver, method, args);
    }
}
