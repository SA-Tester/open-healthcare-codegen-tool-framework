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

package org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r5.oas;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.text.CaseUtils;
import org.hl7.fhir.r5.model.ElementDefinition;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.StructureDefinition;
import org.wso2.healthcare.codegen.tool.framework.commons.exception.CodeGenException;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.model.FHIRSearchParamDef;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.oas.APIDefinitionConstants;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.oas.OASGenUtils;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.oas.OASGenerator;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.oas.model.APIDefinition;
import org.wso2.healthcare.codegen.tool.framework.fhir.core.versions.r5.common.FHIRR5SpecificationData;

import java.util.*;

/**
 * This class generates OAS definitions, specifically R5 FHIR resources, inheriting from OASGenerator.
 */
public class R5OASGenerator extends OASGenerator {
    private static final R5OASGenerator OAS_GENERATOR_INSTANCE = new R5OASGenerator();

    private R5OASGenerator() {
        super();
    }

    public static R5OASGenerator getInstance() {
        return OAS_GENERATOR_INSTANCE;
    }

    /**
     * Generates OAS definition for a given FHIR resource definition.
     *
     * @param apiDefinition       API definition object
     * @param structureDefinition FHIR resource definition
     * @return Generated OAS definition
     */
    public OpenAPI generateResourceSchema(APIDefinition apiDefinition, StructureDefinition structureDefinition) throws CodeGenException {
        OpenAPI resourceOAS = new OpenAPI();

        resourceOAS.setComponents(fhirOASBaseStructure.getComponents());
        apiDefinition.setOpenAPI(resourceOAS);
        populateOASPaths(apiDefinition);
        populateOASInfo(apiDefinition);
        populateOASInternalValues(apiDefinition);

        for (ElementDefinition element : structureDefinition.getSnapshot().getElement()) {
            try {
                String id = element.getId();
                ObjectSchema objectSchema = new ObjectSchema();

                Set<String> requiredElementsCollector = new LinkedHashSet<>();
                ComposedSchema allOfSchema = new ComposedSchema();
                Map<String, Schema> propertySchemaMap = new HashMap<>();
                Schema parentReference = new Schema();

                parentReference.$ref(APIDefinitionConstants.OAS_REF_SCHEMAS + "DomainResource");
                allOfSchema.addAllOfItem(parentReference);

                for (ElementDefinition.TypeRefComponent type : element.getType()) {
                    StringBuilder elementName;

                    // Skip processing the element if the element is not an immediate child or a sliced element
                    // eg: Extension.extension:ombCategory.id
                    if (id.codePoints().filter(ch -> ch == (int) '.').count() > 1 || id.contains(":")) {
                        continue;
                    }

                    if (id.contains("[")) {
                        elementName = new StringBuilder(id.substring(id.lastIndexOf(".") + 1, id.lastIndexOf("[")));
                    } else {
                        elementName = new StringBuilder(id.substring(id.lastIndexOf(".") + 1));
                    }

                    if (element.getType().size() != 1) {
                        elementName.append(CaseUtils.toCamelCase(type.getCode(), true, (char[]) null));
                    }

                    ObjectSchema propertySchema = new ObjectSchema();
                    if (resourceOAS.getComponents().getSchemas().containsKey(type.getCode())) {
                        //schema object available, add ref
                        propertySchema.$ref(APIDefinitionConstants.OAS_REF_SCHEMAS + type.getCode());
                    } else if (APIDefinitionConstants.DATA_TYPE_BACKBONE.equals(type.getCode())) {
                        propertySchema.$ref(APIDefinitionConstants.OAS_REF_SCHEMAS + elementName);
                    } else {
                        String oasDataType = OASGenUtils.mapToOASDataType(type.getCode().substring(type.getCode().lastIndexOf(".") + 1));
                        propertySchema.setType(oasDataType);
                        propertySchema.setPattern(OASGenUtils.getRegexForDataType(oasDataType));
                    }
                    propertySchema.setDescription(element.getDefinition());
                    propertySchemaMap.put(elementName.toString(), propertySchema);

                    if (element.getMin() != 0) {
                        requiredElementsCollector.add(elementName.toString());
                    }
                }

                objectSchema.setProperties(propertySchemaMap);
                allOfSchema.addAllOfItem(objectSchema);

                List<String> requiredElements = new ArrayList<>(requiredElementsCollector);
                allOfSchema.setRequired(requiredElements);
                resourceOAS.getComponents().addSchemas(structureDefinition.getType(), allOfSchema);
            } catch (Exception e) {
                throw new CodeGenException("Error occurred while generating OAS definition for structure definition: "
                        + structureDefinition.getType() + " element " + element.getId(), e);
            }
        }
        return resourceOAS;
    }

    /**
     * Populates internal values of the OAS definition.
     *
     * @param apiDefinition API definition object
     */
    private void populateOASInternalValues(APIDefinition apiDefinition) {
        apiDefinition.getOpenAPI().getInfo().setDescription(OASGenUtils.generateDescription(
                apiDefinition.getResourceType(),
                apiDefinition.getSupportedProfiles()
        ));

        Tag tag = new Tag();
        tag.setName(apiDefinition.getResourceType());

        Tag fhiVersionTag = new Tag();
        fhiVersionTag.setName(APIDefinitionConstants.FHIR_VERSION_R5);

        for (String igName : apiDefinition.getSupportedProfiles()) {
            Tag igTag = new Tag();
            igTag.setName(igName);
            apiDefinition.getOpenAPI().addTagsItem(igTag);
        }
        apiDefinition.getOpenAPI().addTagsItem(tag);
        apiDefinition.getOpenAPI().addTagsItem(fhiVersionTag);

        Map<String, Object> extensions = new HashMap<>();
        extensions.put(APIDefinitionConstants.OAS_EXTENSION_OH_FHIR_RESOURCE_TYPE, apiDefinition.getResourceType());
        extensions.put(APIDefinitionConstants.OAS_EXTENSION_OH_FHIR_PROFILE, apiDefinition.getSupportedProfiles());
        apiDefinition.getOpenAPI().setExtensions(extensions);

        if (apiDefinition.getOpenAPI().getPaths().get("/") != null) {
            Operation rootGet = apiDefinition.getOpenAPI().getPaths().get("/").getGet();

            if (rootGet != null) {
                for (FHIRSearchParamDef searchParamDef : FHIRR5SpecificationData.getDataHolderInstance().getInternationalSearchParameters(apiDefinition.getResourceType())) {
                    if (!R5OASGenUtils.isAdded((SearchParameter) searchParamDef.getSearchParameter(), rootGet)) {
                        SearchParameter searchParameter = (SearchParameter) searchParamDef.getSearchParameter();

                        rootGet.addParametersItem(R5OASGenUtils.generateParameter(
                                searchParameter.getCode(),
                                searchParameter.getDescription(),
                                searchParameter.getType().toCode(),
                                "query",
                                false
                        ));
                    }
                }

                for (String commonParam : apiDefinition.getOpenAPI().getComponents().getParameters().keySet()) {
                    rootGet.addParametersItem(new Parameter().$ref(APIDefinitionConstants.OAS_REF_PARAMETERS + commonParam));
                }
            }
        }
    }
}
