package org.humanbrainproject.knowledgegraph.control.arango;

import org.humanbrainproject.knowledgegraph.control.VertexRepository;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
public class ArangoNamingConvention {

    public static final int MAX_CHARACTERS = 60;
    protected Logger logger = LoggerFactory.getLogger(ArangoNamingConvention.class);


    public String replaceSpecialCharacters(String value) {
        return reduceStringToMaxSizeByHashing(value != null ? value.replaceAll("https://", "").replaceAll("http://", "").replaceAll("\\.", "_").replaceAll("[^a-zA-Z0-9\\-_]", "-") : null);
    }

    public String queryKey(String value) {
        return reduceStringToMaxSizeByHashing(value != null ? value.replaceAll("-", "_") : null);
    }

    public String getIdFromReference(String reference, boolean isEmbedded) {
        if (reference != null) {
            String collectionName=null;
            String id = null;
            if(isEmbedded){
                String[] split = reference.split("@");
                if(split.length>1) {
                    collectionName = split[0];
                    id = reference;
                }
            }
            else{
                String s = reduceVertexLabel(reference);
                String[] split = s.split("(?<=v\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/");
                if (split.length == 2) {
                    collectionName = split[0];
                    id = split[1];
                }
            }
            if(collectionName!=null && id!=null)    {
                return createId(collectionName, id);
            } else {
                return reference;
            }
        }
        return null;
    }

    public String createId(String collectionName, String id) {
        return String.format("%s/%s", getVertexLabel(collectionName), reduceStringToMaxSizeByHashing(replaceSpecialCharacters(id)));
    }

    public String getCollectionNameFromId(String id){
        return id.split("/")[0];
    }

    public String getKeyFromId(String id){
        int length = getCollectionNameFromId(id).length();
        if(length+1<id.length()) {
            return id.substring(length+1);
        }
        return null;
    }

    public String getEdgeLabel(JsonLdEdge edge) {
        return getEdgeLabel(edge.getName());
    }

    private String reduceStringToMaxSizeByHashing(String string) {
        return string == null || string.length() <= MAX_CHARACTERS ? string : DigestUtils.md5DigestAsHex(string.getBytes());
    }

    public String getEdgeLabel(String edgeLabel) {
        return replaceSpecialCharacters(String.format("rel-%s", reduceStringToMaxSizeByHashing(edgeLabel)));
    }

    public String getKey(JsonLdVertex vertex) {
        return reduceStringToMaxSizeByHashing(replaceSpecialCharacters(vertex.getKey()));
    }

    private String reduceVertexLabel(String vertexLabel) {
        return vertexLabel != null ? vertexLabel.replaceAll(".*/(?=.*/.*/.*/v\\d*\\.\\d*\\.\\d*)", "") : null;
    }

    public String getVertexLabel(String vertexName) {
        return reduceStringToMaxSizeByHashing(replaceSpecialCharacters(reduceVertexLabel(vertexName)));
    }

    public String getId(JsonLdVertex vertex) {
        return String.format("%s/%s", getVertexLabel(vertex.getEntityName()), getKey(vertex));
    }

    private String getReferenceKey(String from, String to) {
        String hash = DigestUtils.md5DigestAsHex(String.format("%s-to-%s", from, to).getBytes());
        logger.info("Calculate reference hash: from \"{}\" to \"{}\" results in \"{}\"", from, to, hash);
        return hash;
    }

    public String getReferenceKey(JsonLdVertex vertex, JsonLdEdge edge){
        return getReferenceKey(getId(vertex), getEdgeTarget(edge));
    }

    public String getEdgeTarget(JsonLdEdge edge) {
        if (edge.isEmbedded() && edge.getTarget() != null) {
            return getId(edge.getTarget());
        } else if (!edge.isEmbedded() && edge.getReference() != null) {
            return getIdFromReference(edge.getReference(), edge.isEmbedded());
        }
        return null;
    }


}
