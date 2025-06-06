/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.org).
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

import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r4.FHIRR4SpecParser;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r5.FHIRR5SpecParser;

public class FHIRSpecParserFactory {
    public static AbstractFHIRSpecParser getParser(String fhirVersion) {
        if (fhirVersion.equalsIgnoreCase("r4")) {
            return new FHIRR4SpecParser();
        } else if (fhirVersion.equalsIgnoreCase("r5")) {
            return new FHIRR5SpecParser();
        } else {
            throw new IllegalArgumentException("Unsupported FHIR Version. Supported versions are: r4, r5");
        }
    }
}
