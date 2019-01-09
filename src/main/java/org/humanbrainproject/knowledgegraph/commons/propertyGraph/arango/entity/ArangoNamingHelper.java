package org.humanbrainproject.knowledgegraph.commons.propertyGraph.arango.entity;

import org.humanbrainproject.knowledgegraph.annotations.ToBeTested;
import org.springframework.util.DigestUtils;

@ToBeTested(easy = true)
public class ArangoNamingHelper {


    public static final int MAX_CHARACTERS = 60;


    static String replaceSpecialCharacters(String value) {
        return value != null ? value.replaceAll("\\.", "_").replaceAll("[^a-zA-Z0-9\\-_]", "-") : null;
    }

    static String reduceStringToMaxSizeByHashing(String string) {
        return string == null || string.length() <= MAX_CHARACTERS ? string : String.format("hashed_%s", DigestUtils.md5DigestAsHex(string.getBytes()));
    }

    static String removeTrailingHttps(String value){
        return value!=null ? value.replaceAll("http(s)?://", "") : value;
    }

    public static String createCompatibleId(String id){
        return reduceStringToMaxSizeByHashing(removeTrailingHttps(replaceSpecialCharacters(id)));
    }
}
