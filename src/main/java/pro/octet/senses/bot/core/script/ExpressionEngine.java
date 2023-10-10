package pro.octet.senses.bot.core.script;

import de.odysseus.el.ExpressionFactoryImpl;
import de.odysseus.el.util.SimpleContext;
import lombok.extern.slf4j.Slf4j;
import pro.octet.senses.bot.utils.CommonUtils;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.util.Map;

/**
 * EL expression engine<br/>
 * <p>EXAMPLE<b>: ${ vars >= 1 && xyz == 'chars' }<b/><p/>
 *
 * @author William
 * @since 22.0816.2.6
 */
@Slf4j
public final class ExpressionEngine {

    private static volatile ExpressionEngine engine;
    private static ExpressionFactory expressionFactory;

    private ExpressionEngine() {
    }

    /**
     * Get an instance of expression engine
     *
     * @return ExpressionEngine
     */
    public static ExpressionEngine getInstance() {
        if (engine == null) {
            synchronized (ExpressionEngine.class) {
                if (engine == null) {
                    engine = new ExpressionEngine();
                    expressionFactory = new ExpressionFactoryImpl();
                }
            }
        }
        return engine;
    }

    /**
     * Build a simple expression context
     *
     * @param parameters Expression parameters map
     * @return SimpleContext
     */
    public SimpleContext buildSimpleContext(Map<String, Object> parameters) {
        SimpleContext expressionContext = new SimpleContext();
        if (parameters != null) {
            log.debug("Building simple EL context, parameters: {}.", parameters);
            parameters.forEach((key, value) -> expressionContext.setVariable(key, expressionFactory.createValueExpression(value, value.getClass())));
        }
        return expressionContext;
    }

    /**
     * Executes the expression and returns a boolean result
     *
     * @param expression expression
     * @param context    context
     * @return boolean
     */
    public boolean execute(String expression, SimpleContext context) {
        ValueExpression expr = expressionFactory.createValueExpression(context, expression, boolean.class);
        log.debug("Execute EL expression, context: {}, expression string: {}, value expression: {}.", CommonUtils.toJson(context), expression, CommonUtils.toJson(expr));
        return (boolean) expr.getValue(context);
    }

}
