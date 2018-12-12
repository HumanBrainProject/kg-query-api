package org.humanbrainproject.knowledgegraph.commons.jsonld.control;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import com.google.gson.Gson;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.HBPVocabulary;
import org.humanbrainproject.knowledgegraph.commons.vocabulary.NexusVocabulary;
import org.humanbrainproject.knowledgegraph.indexing.entity.nexus.NexusInstanceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    JsonTransformer jsonTransformer;

    @Value("${org.humanbrainproject.knowledgegraph.jsonld.endpoint}")
    String endpoint;

    private static final JsonLdOptions NO_ARRAY_COMPACTION_JSONLD_OPTIONS = createOptionsWithoutArrayCompaction();
    private static final JsonLdOptions DEFAULT_JSON_LD_OPTIONS = new JsonLdOptions();

    protected Logger logger = LoggerFactory.getLogger(JsonLdStandardization.class);

    private static JsonLdOptions createOptionsWithoutArrayCompaction() {
        JsonLdOptions jsonLdOptions = new JsonLdOptions();
        jsonLdOptions.setCompactArrays(false);
        return jsonLdOptions;
    }

    /**
     * Takes the given json element and adds a @context with the default namespace as @vocab. This is e.g. required if the input is a JSON-only file.
     *
     * @param input            - a json payload already parsed by {@link JsonUtils}
     * @param defaultNamespace - a default namespace such as "https://schema.hbp.eu/foo"
     * @return
     */
    @SuppressWarnings("unchecked")
    public Map ensureContext(Map input, String defaultNamespace) {
        if (input != null) {
            List context = collectContextElements(input.get(JsonLdConsts.CONTEXT));
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
            input.put(JsonLdConsts.CONTEXT, context);
        }
        return input;
    }

    /**
     * This logic removes the JSON-LD @list property, which defines ordered lists. This is, because internally we treat JSON-LD as JSON which ensures the insertion-order natively.
     * The reduction of the @list elements therefore simplifies the treatment (e.g. graph traversal).
     *
     * @param input
     * @param parent
     * @param parentKey
     * @param <T>
     * @return
     */
    public <T> T flattenLists(T input, Map parent, String parentKey) {
        if (input instanceof List) {
            ((List) input).forEach(i -> flattenLists(i, parent, parentKey));
        } else if (input instanceof Map) {
            if (((Map) input).containsKey(JsonLdConsts.LIST)) {
                Object list = ((Map) input).get(JsonLdConsts.LIST);
                parent.put(parentKey, list);
            } else {
                for (Object o : ((Map) input).keySet()) {
                    flattenLists(((Map) input).get(o), (Map) input, (String) o);
                }
            }
        }
        return input;
    }

    public interface NexusInstanceReferenceTransformer {
        NexusInstanceReference transform(NexusInstanceReference source);
    }


    public <T> T extendInternalReferencesWithRelativeUrl(T input, NexusInstanceReferenceTransformer transformer) {
        if (input instanceof List) {
            ((List) input).forEach(i -> extendInternalReferencesWithRelativeUrl(i, transformer));
        } else if (input instanceof Map) {
            if (((Map) input).containsKey(JsonLdConsts.ID)) {
                String referencedId = (String) ((Map) input).get(JsonLdConsts.ID);
                NexusInstanceReference fromUrl = NexusInstanceReference.createFromUrl(referencedId);
                if (fromUrl != null) {
                    if (transformer != null) {
                        String formerRelativeUrl = fromUrl.getRelativeUrl().getUrl();
                        NexusInstanceReference transformed = transformer.transform(fromUrl);
                        if (transformed != null) {
                            fromUrl = transformed;
                            if (!fromUrl.getRelativeUrl().getUrl().equals(formerRelativeUrl)) {
                                referencedId = referencedId.replace(formerRelativeUrl, fromUrl.getRelativeUrl().getUrl());
                                ((Map) input).put(JsonLdConsts.ID, referencedId);
                            }
                        }
                    }
                    ((Map) input).put(HBPVocabulary.RELATIVE_URL_OF_INTERNAL_LINK, fromUrl.getRelativeUrl().getUrl());
                }
            }
            for (Object o : ((Map) input).keySet()) {
                extendInternalReferencesWithRelativeUrl(((Map) input).get(o), transformer);
            }
        }
        return input;
    }

    public <T> T filterKeysByVocabBlacklists(T input) {
        List<String> blacklist = Arrays.asList(NexusVocabulary.NAMESPACE);
        if (input instanceof List) {
            ((List) input).forEach(this::filterKeysByVocabBlacklists);
        } else if (input instanceof Map) {
            Set keySet = new HashSet(((Map) input).keySet());
            for (Object key : keySet) {
                boolean removed = false;
                for (String filterSchema : blacklist) {
                    if (key.toString().startsWith(filterSchema)) {
                        removed = true;
                        ((Map) input).remove(key);
                        break;
                    }
                }
                if (!removed) {
                    filterKeysByVocabBlacklists(((Map) input).get(input));
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
    public Map fullyQualify(String jsonPayload) {
        return fullyQualify(jsonTransformer.parseToMap(jsonPayload));
    }

    /**
     * Fully qualify your JSON-LD. This means, that all alias declaration of the provided context are resolved and
     * the resulting document therefore can be treated as a standard JSON format with semantic key declarations.
     *
     * @param input - a json payload already parsed by {@link JsonUtils}
     * @return
     */
    public Map fullyQualify(Map input) {
        try {
            return fullyQualifyLocally(input);
        } catch (Exception localException) {
            logger.info("Was not able to fully qualify the given payload - try by service");
            logger.debug("Was not able to fully qualify the given payload - try by service", localException);
            try {
                if (endpoint != null) {
                    return fullyQualifyByService(input);
                } else {
                    throw new RuntimeException("Was not able to fully qualify the entity because the JSON-LD service endpoint was not configured.");
                }
            } catch (HttpClientErrorException e) {
                throw new JsonLdError(JsonLdError.Error.UNKNOWN_ERROR, "Was not able to fully qualify the content - there is something wrong with the payload", e);
            } catch (Exception e) {
                throw new RuntimeException("Was not able to fully qualify the content", e);
            }
        }
    }

    private Map fullyQualifyLocally(Map input) {
        Object expanded = JsonLdProcessor.expand(input, DEFAULT_JSON_LD_OPTIONS);
        return JsonLdProcessor.compact(expanded, Collections.emptyMap(), DEFAULT_JSON_LD_OPTIONS);
    }

    private Map fullyQualifyByService(Map input) {
        RestTemplate template = new RestTemplate();
        String fullyQualified = template.postForObject(endpoint, input, String.class);
        return jsonTransformer.parseToMap(fullyQualified);
    }

    public List<Map> applyContext(List<Map> objects, Object context) {
        handleVocab(context);
        return objects.parallelStream().map(o -> {
            List<Map<String, String>> keys = getKeys(o, new ArrayList<>());
            Map<String, Object> lookupMap = new LinkedHashMap<>();
            lookupMap.put("http://jsonldstandardization/keymapping", keys);
            Map<String, Object> lookup = JsonLdProcessor.compact(lookupMap, context, DEFAULT_JSON_LD_OPTIONS);
            List<Map<String, String>> mapping = new ArrayList<>();
            Object l = lookup.get("http://jsonldstandardization/keymapping");
            if (l instanceof Map){
                mapping.add( (Map<String, String>)lookup.get("http://jsonldstandardization/keymapping"));
            } else if(l instanceof List){
                mapping = (List<Map<String, String>>) l;
            } else {
                throw new ClassCastException("Could not cast keymapping type");
            }
            Map<String, String> keymapping = expandedToContextualizedKeys(mapping);
            applyKeyMap(o, keymapping);
            return o;
        }).collect(Collectors.toList());
    }


    private void handleVocab(Object context) {
        if (context instanceof Map && ((Map) context).containsKey(JsonLdConsts.VOCAB)) {
            Map ctx = ((Map) context);
            ctx.put("vocab", ctx.get(JsonLdConsts.VOCAB));
        }
    }


    private void applyKeyMap(Object object, Map keymapping) {
        if (object instanceof Map) {
            Map map = (Map) object;
            Set keys = new HashSet(map.keySet());
            for (Object key : keys) {
                Object originalValue = map.get(key);
                applyKeyMap(originalValue, keymapping);
                if (keymapping.containsKey(key)) {
                    Object new_key = keymapping.get(key);
                    if (!new_key.equals(key)) {
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


    private Map<String, String> expandedToContextualizedKeys(List<Map<String, String>> lookup) {
        Map<String, String> keymap = new HashMap<>();
        for (Map<String, String> lookupMap : lookup) {
            String v = lookupMap.get(JsonLdConsts.ID);
            if (v.startsWith("vocab:")) {
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
