/*
 * Copyright 2020 EPFL/Human Brain Project PCO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.humanbrainproject.knowledgegraph.instances.boundary;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.entity.SubSpace;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusSchemaReference;
import org.humanbrainproject.knowledgegraph.instances.control.SchemaController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@ToBeTested(integrationTestRequired = true, systemTestRequired = true)
@Component
public class Schemas {

    @Autowired
    SchemaController schemaController;

    public void createSimpleSchema(NexusSchemaReference nexusSchemaReference) {
        schemaController.createSchema(nexusSchemaReference);
    }


    public void clearAllInstancesOfSchema(NexusSchemaReference nexusSchemaReference){
        for (SubSpace subSpace : SubSpace.values()) {
            NexusSchemaReference subSpaceSchema = nexusSchemaReference.toSubSpace(subSpace);
            schemaController.clearAllInstancesFromSchema(subSpaceSchema);
        }
    }


    public void createSchemasInNewVersion(String org, String newVersion){
        List<NexusSchemaReference> allSchemas = schemaController.getAllSchemas(org);
        for (NexusSchemaReference schema : allSchemas) {
            NexusSchemaReference newReference = new NexusSchemaReference(schema.getOrganization(), schema.getDomain(), schema.getSchema(), newVersion);
            createSimpleSchema(newReference);
        }
    }


}
