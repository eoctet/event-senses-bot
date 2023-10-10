/*
 * BIFANG EVENT INTELLIGENCE ENGINE 2022
 */

package pro.octet.senses.bot.action.script;

import com.google.common.base.Preconditions;
import groovy.lang.Binding;
import groovy.lang.Script;
import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.core.action.AbstractAction;
import pro.octet.senses.bot.core.entity.Action;
import pro.octet.senses.bot.core.entity.ExecuteResult;
import pro.octet.senses.bot.core.entity.Parameter;
import pro.octet.senses.bot.core.enums.ScriptLangType;
import pro.octet.senses.bot.core.handler.Summary;
import pro.octet.senses.bot.core.model.ScriptConfig;
import pro.octet.senses.bot.core.script.ScriptEngine;
import pro.octet.senses.bot.core.script.ScriptSandbox;
import pro.octet.senses.bot.exception.ActionExecutionException;
import pro.octet.senses.bot.utils.CommonUtils;
import pro.octet.senses.bot.utils.XmlParser;

import java.util.List;

@Slf4j
public class ScriptAction extends AbstractAction {

    private final ScriptSandbox sandbox;

    public ScriptAction(Action action) {
        super(action);
        ScriptConfig scriptConfig = getActionConfig(ScriptConfig.class);
        if (!ScriptLangType.PYTHON.name().equalsIgnoreCase(scriptConfig.getLang())
                && !ScriptLangType.JAVA.name().equalsIgnoreCase(scriptConfig.getLang())) {
            throw new IllegalArgumentException("Unsupported action script lang");
        }
        this.sandbox = new ScriptSandbox();
    }

    @Override
    public ExecuteResult execute() throws ActionExecutionException {
        Summary.of().info("action.start.regexp", getAction().getName());
        ExecuteResult executeResult = new ExecuteResult();
        try {
            ScriptConfig scriptConfig = getActionConfig(ScriptConfig.class);
            String actionCode = getAction().getCode();
            String scriptContent = scriptConfig.getCode().toString();
            String hashcode = ScriptEngine.getInstance().getScriptHash(scriptContent);

            if (!ScriptEngine.getInstance().isDifference(actionCode, hashcode)) {
                ScriptEngine.getInstance().addScript(actionCode, hashcode, scriptContent);
            }
            //load parameters
            Binding binding = new Binding();
            getActionParam().forEach(binding::setProperty);
            log.debug("Create the script context, config parameter:\n{}", binding);
            //run script
            Script script = ScriptEngine.getInstance().getScript(actionCode);
            Preconditions.checkNotNull(script, "Script cannot be null.");
            script.setBinding(binding);
            sandbox.register();
            Object result = script.run();
            //update action output parameters
            if (result != null) {
                switch (scriptConfig.getFormat()) {
                    case XML:
                        executeResult.putAll(XmlParser.parseXmlToMap(String.valueOf(result)));
                        break;
                    case JSON:
                        executeResult.putAll(CommonUtils.parseJsonToMap(String.valueOf(result), String.class, Object.class));
                        break;
                    case DEFAULT:
                        List<Parameter> output = getAction().getActionOutput();
                        if (output != null && output.size() == 1) {
                            executeResult.put(output.get(0).getParam(), result);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported script result format");
                }
            }
        } catch (Exception e) {
            setExecuteThrowable(e);
        }
        return executeResult;
    }
}
