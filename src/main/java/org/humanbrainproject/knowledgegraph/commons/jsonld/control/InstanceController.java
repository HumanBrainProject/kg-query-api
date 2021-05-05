/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */

package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

import com.github.jsonldjava.core.JsonLdConsts;
import org.humanbrainproject.knowledgegraph.commons.entity.JsonLdObject;
import org.humanbrainproject.knowledgegraph.commons.entity.JsonLdStructure;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.ArangoInferredRepository;
import org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.control.EqualsFilter;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.AbsoluteNexusInstanceReference;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.humanbrainproject.knowledgegraph.instances.control.InstanceManipulationController;
import org.humanbrainproject.knowledgegraph.query.entity.JsonDocument;
import org.humanbrainproject.knowledgegraph.users.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class InstanceController<T extends JsonLdObject> {

    private Logger logger = LoggerFactory.getLogger(InstanceController.class);
    @Autowired
    ArangoInferredRepository inferredRepository;

    @Autowired
    InstanceManipulationController instanceManipulationController;

    public T findUniqueInstance(List<EqualsFilter> filters, JsonLdStructure<T> structure, boolean asSystemUser) {
        List<Map> instances = inferredRepository.findInstancesBySchemaAndFilter(User.STRUCTURE.getNexusSchemaReference(), filters, asSystemUser);
        if (instances != null && !instances.isEmpty()) {
            if (instances.size() > 1) {
                logger.error(String.format("Found %d instances instead of a unique", instances.size()));
            }
            try {
                return structure.getEntityClass().getConstructor(JsonLdStructure.class, JsonDocument.class).newInstance(structure, new JsonDocument(instances.get(0)));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                logger.error("Was not able to create instance - is the constructur missing?", e);
            }
        }
        return null;
    }


    public List<T> listInstances(List<EqualsFilter> filters, JsonLdStructure<T> structure, boolean asSystemUser) {
        List<Map> instances = inferredRepository.findInstancesBySchemaAndFilter(structure.getNexusSchemaReference(), filters, asSystemUser);
        if (instances != null) {
            return instances.stream().map(i -> {
                try {
                    return structure.getEntityClass().getConstructor(JsonLdStructure.class, JsonDocument.class).newInstance(structure, new JsonDocument(i));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toList());
        }
        return null;
    }


    public T getInstance(AbsoluteNexusInstanceReference instanceReference, JsonLdStructure<T> jsonLdStructure) {
        return findUniqueInstance(Collections.singletonList(new EqualsFilter(JsonLdConsts.ID, instanceReference.getAbsoluteUrl())), jsonLdStructure, false);
    }


    public T createInstance(T instance) {
        NexusInstanceReference newInstance = instanceManipulationController.createNewInstance(instance.getJsonLdStructure().getNexusSchemaReference(), instance.asJsonLd(), null);
        instance.setInstanceReference(newInstance);
        return instance;
    }

    public void removeInstance(T instance) {
        instanceManipulationController.deprecateInstanceByNexusId(instance.getInstanceReference());
    }

}
