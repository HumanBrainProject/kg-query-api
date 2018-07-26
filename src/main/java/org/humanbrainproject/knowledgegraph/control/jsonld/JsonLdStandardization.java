package org.humanbrainproject.knowledgegraph.control.jsonld;

import com.github.jsonldjava.core.JsonLdConsts;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

/**
 * JSON-LD can appear in many different facets. This class provides tools to harmonize the structure of a JSON / JSON-LD file
 * to allow simplified automatic processing
 */
@Component
public class JsonLdStandardization {

    private static final JsonLdOptions EMPTY_OPTIONS = new JsonLdOptions();

    /**
     * Takes the given json element and adds a @context with the default namespace as @vocab. This is e.g. required if the input is a JSON-only file.
     *
     * @param input - a json payload already parsed by {@link JsonUtils}
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
        input = JsonLdProcessor.expand(input, EMPTY_OPTIONS);
        return JsonLdProcessor.compact(input, Collections.emptyMap(), EMPTY_OPTIONS);
    }

    private void removeNestedTypeDeclarations(){

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
