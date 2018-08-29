package org.humanbrainproject.knowledgegraph.control.jsonld;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.control.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JSON-LD can appear in many different facets. This class provides tools to harmonize the structure of a JSON / JSON-LD file
 * to allow simplified automatic processing
 */
@Component
public class JsonLdStandardization {

    @Value("${org.humanbrainproject.knowledgegraph.jsonld.endpoint}")
    String endpoint;


    private static final JsonLdOptions NO_ARRAY_COMPACTION_JSONLD_OPTIONS = createOptionsWithoutArrayCompaction();
    private static final JsonLdOptions DEFAULT_JSON_LD_OPTIONS = new JsonLdOptions();

    private static JsonLdOptions createOptionsWithoutArrayCompaction() {
        JsonLdOptions jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setCompactArrays(false);
        return jsonLdOptions;
    }

    /**
     * Takes the given json element and adds a @context with the default namespace as @vocab. This is e.g. required if the input is a JSON-only file.
     *
     * @param input            - a json payload already parsed by {@link JsonUtils}
     * @param defaultNamespace - a default namespace such as "http://schema.hbp.eu/foo"
     * @return
     */
    @SuppressWarnings("unchecked")
    public Object ensureContext(Object input, String defaultNamespace) {
        if (input instanceof Map<?, ?>) {
            List context = collectContextElements(((Map) input).get(JsonLdConsts.CONTEXT));
            boolean hasVocab = false;
            for (Object contextElement : context) {
                if (contextElement instanceof Map) {
                    if (((Map) contextElement).containsKey(JsonLdConsts.VOCAB)) {
                        hasVocab = true;
                        break;
                    }
                }
            }
            if (!hasVocab) {
                Map vocab = new LinkedHashMap();
                vocab.put(JsonLdConsts.VOCAB, defaultNamespace);
                context.add(vocab);
            }
            ((Map) input).put(JsonLdConsts.CONTEXT, context);
        }
        return input;
    }


    public Object filterKeysByVocabBlacklists(Object input){
        List<String> blacklist = Arrays.asList(Constants.NEXUS_TERMS_VOCAB);
        if(input instanceof List){
            ((List)input).forEach(this::filterKeysByVocabBlacklists);
        }
        else if (input instanceof Map){
            Set keySet = new HashSet(((Map) input).keySet());
            for (Object key : keySet) {
                boolean removed = false;
                for (String filterSchema : blacklist) {
                    if(key.toString().startsWith(filterSchema)){
                        removed = true;
                        ((Map)input).remove(key);
                        break;
                    }
                }
                if(!removed){
                    filterKeysByVocabBlacklists(((Map)input).get(input));
                }
            }
        }
        return input;
    }


    /**
     * Fully qualify your JSON-LD. This means, that all alias declaration of the provided context are resolved and
     * the resulting document therefore can be treated as a standard JSON format with semantic key declarations.
     *
     * @param jsonPayload
     * @return
     * @throws IOException
     */
    public Object fullyQualify(String jsonPayload) throws IOException {
        return fullyQualify(JsonUtils.fromString(jsonPayload));
    }

    /**
     * Fully qualify your JSON-LD. This means, that all alias declaration of the provided context are resolved and
     * the resulting document therefore can be treated as a standard JSON format with semantic key declarations.
     *
     * @param input - a json payload already parsed by {@link JsonUtils}
     * @return
     */
    public Object fullyQualify(Object input) {
        try {
            RestTemplate template = new RestTemplate();
            String fullyQualified = template.postForObject(endpoint, input, String.class);
            return JsonUtils.fromString(fullyQualified);
        } catch (HttpClientErrorException e){
            throw new JsonLdError(JsonLdError.Error.UNKNOWN_ERROR, "Was not able to fully qualify the content - there is something wrong with the payload", e);
        } catch (Exception e) {
            throw new RuntimeException("Was not able to fully qualify the content", e);
        }
    }

    public List<Map> applyContext(List<Map> objects, Object context) {
        handleVocab(context);
        return objects.parallelStream().map(o -> {
            List<Map<String, String>> keys = getKeys(o, new ArrayList<>());
            Map<String, Object> lookupMap = new LinkedHashMap<>();
            lookupMap.put("http://jsonldstandardization/keymapping", keys);
            Map<String, Object> lookup = JsonLdProcessor.compact(lookupMap, context, DEFAULT_JSON_LD_OPTIONS);
            Map<String, String> keymapping = expandedToContextualizedKeys((List<Map<String, String>>) lookup.get("http://jsonldstandardization/keymapping"));
            applyKeyMap(o, keymapping);
            return o;
        }).collect(Collectors.toList());
    }


    private void handleVocab(Object context){
        if(context instanceof Map && ((Map)context).containsKey(JsonLdConsts.VOCAB)){
            Map ctx = ((Map)context);
            ctx.put("vocab", ctx.get(JsonLdConsts.VOCAB));
        }
    }


    private void applyKeyMap(Object object, Map keymapping){
        if (object instanceof Map) {
            Map map = (Map) object;
            Set keys = new HashSet(map.keySet());
            for (Object key : keys) {
                Object originalValue = map.get(key);
                applyKeyMap(originalValue, keymapping);
                if(keymapping.containsKey(key)){
                    Object new_key = keymapping.get(key);
                    if(!new_key.equals(key)) {
                        map.put(new_key, originalValue);
                        map.remove(key);
                    }
                }
            }
        }
        if (object instanceof Collection) {
            for (Object o : ((Collection) object)) {
                applyKeyMap(o, keymapping);
            }
        }
    }


    private Map<String, String> expandedToContextualizedKeys(List<Map<String, String>> lookup){
        Map<String, String> keymap = new HashMap<>();
        for (Map<String, String> lookupMap : lookup) {
            String v = lookupMap.get(JsonLdConsts.ID);
            if(v.startsWith("vocab:")){
                v = v.substring("vocab:".length());
            }
//            if(v.startsWith(":")){
//                v = v.substring(1);
//            }
            keymap.put(lookupMap.get("http://jsonldstandardization/original"), v);
        }
        return keymap;
    }


    private List<Map<String, String>> getKeys(Object element, List<Map<String, String>> allKeys) {
        if (element instanceof Map) {
            Map map = (Map) element;
            for (Object k : map.keySet()) {
                HashMap<String, String> m = new LinkedHashMap<>();
                m.put(JsonLdConsts.ID, k.toString());
                m.put("http://jsonldstandardization/original", k.toString());
                allKeys.add(m);
                getKeys(map.get(k), allKeys);
            }
        }
        if (element instanceof Collection) {
            for (Object o : ((Collection) element)) {
                getKeys(o, allKeys);
            }
        }
        return allKeys;
    }


    public Object getContext(String specification) throws JSONException {
        Gson gson = new Gson();
        if (specification != null) {
            JSONObject jsonObject = new JSONObject(specification);
            if (jsonObject.has(JsonLdConsts.CONTEXT)) {
                return gson.fromJson(jsonObject.getJSONObject(JsonLdConsts.CONTEXT).toString(), Map.class);
            }
        }
        return null;
    }

    private List collectContextElements(Object input) {
        if (input instanceof List) {
            return (List) input;
        } else {
            List list = new ArrayList();
            if (input != null) {
                list.add(input);
            }
            return list;
        }
    }
}
