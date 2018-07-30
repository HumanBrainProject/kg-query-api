package org.humanbrainproject.knowledgegraph.control.arango;

import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdEdge;
import org.humanbrainproject.knowledgegraph.entity.jsonld.JsonLdVertex;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

@Component
public class ArangoNamingConvention {

    public static final int MAX_CHARACTERS=60;

    public String replaceSpecialCharacters(String value){
        return value!=null ? value.replaceAll("https://", "").replaceAll("http://", "").replaceAll("\\.", "_").replaceAll("[^a-zA-Z0-9\\-_]", "-") : null;
    }

    public String queryKey(String value){
        return value!=null ? value.replaceAll("-", "_") : null;
    }

    public String getKeyFromReference(String reference){
        if(reference!=null) {
            String s = reduceVertexLabel(reference);
            String[] split = s.split("(?<=v\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/");
            if (split.length > 1) {
                return String.format("%s/%s", replaceSpecialCharacters(split[0]), replaceSpecialCharacters(split[1]));
            }
            return s;
        }
        return null;
    }

    public String getEdgeLabel(JsonLdEdge edge) {
        return getEdgeLabel(edge.getName());
    }

    public String reduceLengthOfCharacters(String original){
        if(original!=null && original.length()>MAX_CHARACTERS){
            return original.substring(original.length()-MAX_CHARACTERS);
        }
        return original;
    }


    public String getEdgeLabel(String edgeLabel) {
        return reduceLengthOfCharacters(replaceSpecialCharacters(String.format("rel-%s", edgeLabel)));
    }

    public String getUuid(JsonLdVertex vertex){
        return replaceSpecialCharacters(vertex.getUuid());
    }

    public String reduceVertexLabel(String vertexLabel) {
        return vertexLabel!=null ? vertexLabel.replaceAll(".*/(?=.*/.*/.*/v\\d*\\.\\d*\\.\\d*)", "") : null;
    }

    public String getVertexLabel(JsonLdVertex vertex) {
        return getVertexLabel(vertex.getType());
    }

    public String getVertexLabel(String vertexName){
        return reduceLengthOfCharacters(replaceSpecialCharacters(reduceVertexLabel(vertexName)));
    }

    public String getDocumentHandle(JsonLdVertex vertex){
        return String.format("%s/%s", getVertexLabel(vertex), getUuid(vertex));
    }


    public String getReferenceKey(String from, String to){
        return DigestUtils.md5DigestAsHex(String.format("%s-to-%s", from, to).getBytes());
    }
}
