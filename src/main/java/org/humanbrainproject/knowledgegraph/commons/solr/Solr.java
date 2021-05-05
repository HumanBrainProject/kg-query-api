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

package org.humanbrainproject.knowledgegraph.commons.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.request.schema.FieldTypeDefinition;
import org.apache.solr.client.solrj.request.schema.SchemaRequest;
import org.apache.solr.client.solrj.response.Group;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.util.NamedList;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.query.entity.BoundingBox;
import org.humanbrainproject.knowledgegraph.query.entity.ThreeDVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
@ToBeTested(systemTestRequired = true)
public class Solr implements InitializingBean {

    @Value("${org.humanbrainproject.knowledgegraph.solr.base}")
    String solrBase;

    @Value("${org.humanbrainproject.knowledgegraph.solr.core}")
    String solrCore;


    @Override
    public void afterPropertiesSet() {
        try{
            registerCore();
        }
        catch (SolrServerException | IOException e){
            logger.error("Was not able to register core on Solr - spatial indexing / search might not be available.");
        }
    }


    private SolrClient solr;

    protected Logger logger = LoggerFactory.getLogger(Solr.class);

    private SolrClient getSolr() {
        if (solr == null) {
            solr = new HttpSolrClient.Builder(solrBase).build();
        }
        return solr;
    }

    private void registerFieldType(String name, String clazz, String subFieldType, int dimension) throws IOException, SolrServerException {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", name);
        attributes.put("class", clazz);
        attributes.put("subFieldType", subFieldType);
        attributes.put("dimension", dimension);
        fieldTypeDefinition.setAttributes(attributes);
        if (!isFieldTypeRegistered(name)) {
            getSolr().request(new SchemaRequest.AddFieldType(fieldTypeDefinition), solrCore);
        } else {
            getSolr().request(new SchemaRequest.ReplaceFieldType(fieldTypeDefinition), solrCore);
            //logger.info("Field type " + name + " already exists - skipping");
        }
    }

    private boolean isFieldTypeRegistered(String name) throws SolrServerException, IOException {
        List<Object> fieldTypes = getSolr().request(new SchemaRequest.FieldTypes(), solrCore).getAll("fieldTypes");
        return checkNameListForValueExistance(name, fieldTypes);
    }

    private boolean isFieldRegistered(String name) throws SolrServerException, IOException {
        List<Object> fields = getSolr().request(new SchemaRequest.Fields(), solrCore).getAll("fields");
        return checkNameListForValueExistance(name, fields);
    }

    private boolean checkNameListForValueExistance(String name, List<Object> fields) {
        for (Object type : fields) {
            if (type instanceof List) {
                for (Object o : ((List) type)) {
                    if (o instanceof NamedList) {
                        if (((NamedList) o).get("name").equals(name)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void registerField(String name, String type, boolean stored, boolean indexed, boolean multivalued, boolean docValues) throws IOException, SolrServerException {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("name", name);
        attributes.put("type", type);
        attributes.put("stored", stored);
        attributes.put("indexed", indexed);
        attributes.put("multiValued", multivalued);
        attributes.put("docValues", docValues);
        if (!isFieldRegistered(name)) {
            getSolr().request(new SchemaRequest.AddField(attributes), solrCore);
        } else {
            getSolr().request(new SchemaRequest.ReplaceField(attributes), solrCore);
        }
    }


    private void registerSchema() throws IOException, SolrServerException {
        registerFieldType("Point3D", "solr.PointType", "pdouble", 3);
        registerField("aid", "string", true, true, false, false);
        registerField("r", "string", true, true, false, true);
        registerField("c", "Point3D", true, true, true, false);
    }

    public void registerCore() throws SolrServerException, IOException {
        if (!isCoreRegistered()) {
            try {
                logger.info("Registering core with name " + solrCore);
                CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
                createRequest.setCoreName(solrCore);
                createRequest.setConfigSet("_default");
                createRequest.setInstanceDir("mycores/" + solrCore);
                getSolr().request(createRequest);
                registerSchema();
            } catch (IOException | SolrServerException | HttpSolrClient.RemoteExecutionException e) {
                logger.error("Was not able to register core " + solrCore, e);
                throw new SolrServerException(e);
            }
        } else {
            logger.info("Core " + solrCore + " already exists. Skipping creation");
        }
    }

    public void removeCore() throws IOException, SolrServerException {
        if (isCoreRegistered()) {
            logger.info("Removing core with name " + solrCore);
            CoreAdminRequest.Unload unloadRequest = new CoreAdminRequest.Unload(true);
            unloadRequest.setCoreName(solrCore);
            unloadRequest.setDeleteDataDir(true);
            unloadRequest.setDeleteInstanceDir(true);
            getSolr().request(unloadRequest);
        }
    }

    private boolean isCoreRegistered() throws SolrServerException, IOException {
        CoreAdminRequest coreAdminRequest = new CoreAdminRequest();
        coreAdminRequest.setAction(CoreAdminParams.CoreAdminAction.STATUS);
        coreAdminRequest.setIndexInfoNeeded(false);
        NamedList<Object> coreAdminResponse = getSolr().request(coreAdminRequest);
        List<Object> status = coreAdminResponse.getAll("status");
        for (Object o : status) {
            if (o instanceof NamedList) {
                if (((NamedList) o).asMap(1).keySet().contains(solrCore)) {
                    return true;
                }
            }
        }
        return false;
    }


    public void registerPoints(String id, String referenceSpace, Collection<ThreeDVector> points) throws IOException, SolrServerException {
        points.forEach(p ->
                {
                    SolrInputDocument document = new SolrInputDocument();
                    document.addField("aid", id);
                    document.addField("r", referenceSpace);
                    document.addField("c", p.toString());
                    try {
                        UpdateResponse response = solr.add(solrCore, document);
                        logger.info(String.format("Indexed point %s for reference %s in space \"%s\" in Solr in %d ms", p.toString(), id, referenceSpace, response.getElapsedTime()));
                    } catch (SolrServerException | IOException e) {
                        logger.error("Was not able to index document into Solr", e);
                    }
                }
        );
        solr.commit(solrCore);
    }

    public void delete(String id, String referenceSpace)  {
        try {
            UpdateResponse response = solr.deleteByQuery(solrCore, "aid:" + id+" AND r:"+referenceSpace);
            logger.info(String.format("Removed points for id %s in space \"%s\" in Solr in %d ms", id, referenceSpace, response.getElapsedTime()));
        } catch (SolrServerException | IOException e) {
            e.printStackTrace();
            logger.error("Was not able to remove document(s) in Solr", e);
        }

    }



    private QueryResponse query(SolrQuery query) throws IOException, SolrServerException {
        return getSolr().query(solrCore, query);
    }

    public List<String> queryIdsOfMinimalBoundingBox(BoundingBox boundingBox) throws IOException, SolrServerException {
        SolrQuery query = new SolrQuery("*:*");
        String coordinateQuery = String.format("c:[\"%s\" TO \"%s\"]", boundingBox.getFrom(), boundingBox.getTo());
        String referenceSpaceQuery = String.format("r:\"%s\"", boundingBox.getReferenceSpace());
        query.setFilterQueries(coordinateQuery, referenceSpaceQuery);
        query.setFields("aid");
        query.setRows(0);
        QueryResponse response = query(query);
        long matches = response.getResults().getNumFound();
        if (matches == 0) {
            return Collections.emptyList();
        }
        query.setRows(Long.valueOf(matches).intValue());
        query.setParam("group", true);
        query.setParam("group.field", "aid");
        return query(query).getGroupResponse().getValues().get(0).getValues().stream().map(Group::getGroupValue).collect(Collectors.toList());
    }


}
