package com.williamcallahan.service.tools;

import com.openai.models.FunctionDefinition;

import java.util.Map;

/** Contract for an LLM-callable tool. */
public interface Tool {
    /** Tool/function name (must match {@link #definition()}). */
    String name();

    /** OpenAI tool definition (function tool). */
    FunctionDefinition definition();

    /** Executes the tool and returns a JSON-serializable object (Map/List/primitive/POJO). */
    Object execute(Map<String, Object> arguments) throws Exception;
}
