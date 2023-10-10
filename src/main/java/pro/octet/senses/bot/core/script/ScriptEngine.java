package pro.octet.senses.bot.core.script;

import com.google.common.collect.Maps;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.kohsuke.groovy.sandbox.SandboxTransformer;
import pro.octet.senses.bot.utils.CommonUtils;

import java.util.concurrent.ConcurrentMap;

/**
 * Groovy script engine
 *
 * @author William
 * @see groovy.lang.GroovyShell
 * @see groovy.lang.Binding
 * @see groovy.lang.Script
 * @since 22.0816.2.6
 */
@Slf4j
public final class ScriptEngine {

    private static final ConcurrentMap<String, Pair<String, Script>> SCRIPT_CACHE = Maps.newConcurrentMap();
    private static volatile ScriptEngine engine;
    private static volatile GroovyShell groovyShell;

    private ScriptEngine() {
    }

    /**
     * Get an instance of script engine
     *
     * @return ScriptEngine
     */
    public static ScriptEngine getInstance() {
        if (engine == null) {
            synchronized (ScriptEngine.class) {
                if (engine == null) {
                    engine = new ScriptEngine();
                }
            }
        }
        return engine;
    }

    /**
     * Get default script shell
     *
     * @return GroovyShell
     */
    public GroovyShell getDefaultShell() {
        if (groovyShell == null) {
            synchronized (this) {
                if (groovyShell == null) {
                    groovyShell = new GroovyShell(new CompilerConfiguration().addCompilationCustomizers(new SandboxTransformer()));
                }
            }
        }
        return groovyShell;
    }

    /**
     * Get script shell through parameter binding
     *
     * @param binding Parameter binding
     * @return GroovyShell
     */
    public GroovyShell getShell(Binding binding) {
        if (groovyShell == null) {
            synchronized (this) {
                if (groovyShell == null) {
                    groovyShell = new GroovyShell(binding, new CompilerConfiguration().addCompilationCustomizers(new SandboxTransformer()));
                }
            }
        }
        return groovyShell;
    }

    /**
     * Get script cache queue
     *
     * @return Script cache queue
     */
    public ConcurrentMap<String, Pair<String, Script>> getScriptCache() {
        return SCRIPT_CACHE;
    }

    /**
     * Insert script into cache queue
     *
     * @param id            Script ID
     * @param hash          Script hashcode
     * @param scriptContent Script code
     */
    public void addScript(String id, String hash, String scriptContent) {
        if (!contains(id)) {
            log.debug("script cache not contains {}, add a new script.", id);
            SCRIPT_CACHE.put(id, Pair.of(hash, getDefaultShell().parse(scriptContent)));
        }
    }

    /**
     * Returns true if the cache queue contains scripts, otherwise false
     *
     * @param id Script ID
     * @return boolean
     */
    public boolean contains(String id) {
        return SCRIPT_CACHE.containsKey(id);
    }

    /**
     * Check whether the two scripts have changed
     *
     * @param id   Script ID
     * @param hash Script hashcode
     * @return boolean
     */
    public boolean isDifference(String id, String hash) {
        if (!contains(id)) {
            return false;
        }
        return SCRIPT_CACHE.get(id).getLeft().equals(hash);
    }

    /**
     * Get script by ID
     *
     * @param id Script ID
     * @return Script
     */
    public Script getScript(String id) {
        if (!contains(id)) {
            return null;
        }
        return SCRIPT_CACHE.get(id).getRight();
    }

    /**
     * Generate script hash code
     *
     * @param scriptContent Script code
     * @return String
     */
    public String getScriptHash(String scriptContent) {
        if (StringUtils.isNotEmpty(scriptContent)) {
            return CommonUtils.getMd5(scriptContent);
        }
        return null;
    }

    /**
     * Remove script from cache queue
     *
     * @param id Script ID
     */
    public void remove(String id) {
        if (contains(id)) {
            SCRIPT_CACHE.remove(id);
        }
    }
}
