
package vavi.nio.file.du;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.*;

public class ExistsLocalPropertiesCondition implements ExecutionCondition {

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (Files.exists(Paths.get("local.properties"))) {
            return enabled("ENABLED");
        } else {
            return disabled("DISABLED");
        }
    }
}
