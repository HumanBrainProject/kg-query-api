package org.humanbrainproject.knowledgegraph.commons.solr;


import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.humanbrainproject.knowledgegraph.query.entity.BoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Component
@ToBeTested(systemTestRequired = true)
public class Mercator {

    @Autowired
    Gson gson;

    @Value("${org.humanbrainproject.knowledgegraph.mercator.base}")
    String mercatorBase;

    @Value("${org.humanbrainproject.knowledgegraph.mercator.core}")
    String mercatorCore;


//    @Override
//    public void afterPropertiesSet() {
//        try{
//            registerCore();
//        }
//        catch (SolrServerException | IOException e){
//            logger.error("Was not able to register core on Solr - spatial indexing / search might not be available.");
//        }
//    }



    protected Logger logger = LoggerFactory.getLogger(Solr.class);


//    private void registerFieldType(String name, String clazz, String subFieldType, int dimension) throws IOException, SolrServerException {
//        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
//        Map<String, Object> attributes = new LinkedHashMap<>();
//        attributes.put("name", name);
//        attributes.put("class", clazz);
//        attributes.put("subFieldType", subFieldType);
//        attributes.put("dimension", dimension);
//        fieldTypeDefinition.setAttributes(attributes);
//        if (!isFieldTypeRegistered(name)) {
//            getSolr().request(new SchemaRequest.AddFieldType(fieldTypeDefinition), mercatorCore);
//        } else {
//            getSolr().request(new SchemaRequest.ReplaceFieldType(fieldTypeDefinition), mercatorCore);
//            //logger.info("Field type " + name + " already exists - skipping");
//        }
//    }
//
//    private boolean isFieldTypeRegistered(String name) throws SolrServerException, IOException {
//        List<Object> fieldTypes = getSolr().request(new SchemaRequest.FieldTypes(), mercatorCore).getAll("fieldTypes");
//        return checkNameListForValueExistance(name, fieldTypes);
//    }
//
//    private boolean isFieldRegistered(String name) throws SolrServerException, IOException {
//        List<Object> fields = getSolr().request(new SchemaRequest.Fields(), mercatorCore).getAll("fields");
//        return checkNameListForValueExistance(name, fields);
//    }
//
//    private boolean checkNameListForValueExistance(String name, List<Object> fields) {
//        for (Object type : fields) {
//            if (type instanceof List) {
//                for (Object o : ((List) type)) {
//                    if (o instanceof NamedList) {
//                        if (((NamedList) o).get("name").equals(name)) {
//                            return true;
//                        }
//                    }
//                }
//            }
//        }
//        return false;
//    }
//
//    private void registerField(String name, String type, boolean stored, boolean indexed, boolean multivalued, boolean docValues) throws IOException, SolrServerException {
//        Map<String, Object> attributes = new LinkedHashMap<>();
//        attributes.put("name", name);
//        attributes.put("type", type);
//        attributes.put("stored", stored);
//        attributes.put("indexed", indexed);
//        attributes.put("multiValued", multivalued);
//        attributes.put("docValues", docValues);
//        if (!isFieldRegistered(name)) {
//            getSolr().request(new SchemaRequest.AddField(attributes), mercatorCore);
//        } else {
//            getSolr().request(new SchemaRequest.ReplaceField(attributes), mercatorCore);
//        }
//    }
//
//
//    private void registerSchema() throws IOException, SolrServerException {
//        registerFieldType("Point3D", "solr.PointType", "pdouble", 3);
//        registerField("aid", "string", true, true, false, false);
//        registerField("r", "string", true, true, false, true);
//        registerField("c", "Point3D", true, true, true, false);
//    }
//
//    public void registerCore() throws SolrServerException, IOException {
//        if (!isCoreRegistered()) {
//            try {
//                logger.info("Registering core with name " + mercatorCore);
//                CoreAdminRequest.Create createRequest = new CoreAdminRequest.Create();
//                createRequest.setCoreName(mercatorCore);
//                createRequest.setConfigSet("_default");
//                createRequest.setInstanceDir("mycores/" + mercatorCore);
//                getSolr().request(createRequest);
//                registerSchema();
//            } catch (IOException | SolrServerException | HttpSolrClient.RemoteExecutionException e) {
//                logger.error("Was not able to register core " + mercatorCore, e);
//                throw new SolrServerException(e);
//            }
//        } else {
//            logger.info("Core " + mercatorCore + " already exists. Skipping creation");
//        }
//    }
//
//    public void removeCore() throws IOException, SolrServerException {
//        if (isCoreRegistered()) {
//            logger.info("Removing core with name " + mercatorCore);
//            CoreAdminRequest.Unload unloadRequest = new CoreAdminRequest.Unload(true);
//            unloadRequest.setCoreName(mercatorCore);
//            unloadRequest.setDeleteDataDir(true);
//            unloadRequest.setDeleteInstanceDir(true);
//            getSolr().request(unloadRequest);
//        }
//    }
//
//    private boolean isCoreRegistered() throws SolrServerException, IOException {
//        CoreAdminRequest coreAdminRequest = new CoreAdminRequest();
//        coreAdminRequest.setAction(CoreAdminParams.CoreAdminAction.STATUS);
//        coreAdminRequest.setIndexInfoNeeded(false);
//        NamedList<Object> coreAdminResponse = getSolr().request(coreAdminRequest);
//        List<Object> status = coreAdminResponse.getAll("status");
//        for (Object o : status) {
//            if (o instanceof NamedList) {
//                if (((NamedList) o).asMap(1).keySet().contains(mercatorCore)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//
//    public void registerPoints(String id, String referenceSpace, Collection<ThreeDVector> points) throws IOException, SolrServerException {
//        points.forEach(p ->
//                {
//                    SolrInputDocument document = new SolrInputDocument();
//                    document.addField("aid", id);
//                    document.addField("r", referenceSpace);
//                    document.addField("c", p.toString());
//                    try {
//                        UpdateResponse response = solr.add(mercatorCore, document);
//                        logger.info(String.format("Indexed point %s for reference %s in space \"%s\" in Solr in %d ms", p.toString(), id, referenceSpace, response.getElapsedTime()));
//                    } catch (SolrServerException | IOException e) {
//                        logger.error("Was not able to index document into Solr", e);
//                    }
//                }
//        );
//        solr.commit(mercatorCore);
//    }
//
//    public void delete(String id, String referenceSpace)  {
//        try {
//            UpdateResponse response = solr.deleteByQuery(mercatorCore, "aid:" + id+" AND r:"+referenceSpace);
//            logger.info(String.format("Removed points for id %s in space \"%s\" in Solr in %d ms", id, referenceSpace, response.getElapsedTime()));
//        } catch (SolrServerException | IOException e) {
//            e.printStackTrace();
//            logger.error("Was not able to remove document(s) in Solr", e);
//        }
//
//    }



    private List<String> query(MercatorFilter filters) throws IOException {

        RestTemplate template = new RestTemplate();
        Map<String, Object> params = new HashMap<>();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(filters), headers);
        List<String> o = Arrays.asList(template.postForEntity(mercatorBase + "/spatial-search/cores/10k/spatial_objects", entity, String[].class, params).getBody());
        return o;
    }

    public  List<String> queryIdsOfMinimalBoundingBox(BoundingBox boundingBox) throws IOException {
        List<List<Double>> viewPort = new ArrayList<>();
        List<Double> from = new ArrayList<>();
        from.add(boundingBox.getFrom().getX());
        from.add(boundingBox.getFrom().getY());
        from.add(boundingBox.getFrom().getZ());
        List<Double> to = new ArrayList<>();
        to.add(boundingBox.getTo().getX());
        to.add(boundingBox.getTo().getY());
        to.add(boundingBox.getTo().getZ());
        viewPort.add(from);
        viewPort.add(to);
        MercatorFilter filter = new MercatorFilter();
//        filter.setReferenceSpace(boundingBox.getReferenceSpace());

        filter.setViewPort(viewPort);
        return query(filter);
    }

}
