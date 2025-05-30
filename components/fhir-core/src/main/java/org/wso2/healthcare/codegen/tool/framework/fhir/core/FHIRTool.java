/*
 * Copyright (c) 2023, WSO2 LLC. (http://www.wso2.org).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.healthcare.codegen.tool.framework.fhir.core;

import org.wso2.healthcare.codegen.tool.framework.commons.config.ToolConfig;
import org.wso2.healthcare.codegen.tool.framework.commons.core.AbstractTool;
import org.wso2.healthcare.codegen.tool.framework.commons.core.Tool;
import org.wso2.healthcare.codegen.tool.framework.commons.core.ToolContext;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents the FHIR tool library which will hold the contextual data and utilities for the
 * FHIR tool implementations to generate artifacts.
 */
public class FHIRTool extends AbstractTool {

    private String fhirVersion;
    public static final String BASE_OAS_MODEL_PROPERTY = "baseOAS";
    private AbstractFHIRToolContext toolContext;
    private Map<String, Tool> toolImplementations;

    public FHIRTool(String fhirVersion) {
        this.fhirVersion = fhirVersion;
        toolImplementations = new HashMap<>();
    }

    public void initialize(ToolConfig toolConfig) throws CodeGenException {
        toolContext = FHIRToolContextFactory.getToolContext(fhirVersion);
        toolContext.setConfig(toolConfig);

        AbstractFHIRSpecParser specParser = FHIRSpecParserFactory.getParser(fhirVersion);
        specParser.parse(toolConfig);
    }

    public ToolContext getToolContext() {
        return toolContext;
    }

    public void setToolContext(ToolContext toolContext) {
        this.toolContext = (AbstractFHIRToolContext) toolContext;
    }

    public Map<String, Tool> getToolImplementations() {
        return toolImplementations;
    }

    public void setToolImplementations(Map<String, Tool> toolImplementations) {
        this.toolImplementations = toolImplementations;
    }
}
