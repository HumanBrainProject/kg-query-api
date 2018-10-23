package deprecated.control.template;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.gson.Gson;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.io.StringWriter;

@Component
@Scope(scopeName = "singleton")
public class MustacheTemplating {

    private MustacheFactory factory = null;
    private Gson gson = null;

    private MustacheFactory getFactory(){
        if(factory==null){
            factory = new DefaultMustacheFactory();
        }
        return factory;
    }

    private Gson getGson(){
        if(gson==null){
            gson = new Gson();
        }
        return gson;
    }


    public String applyTemplate(String template, Object json){
        try(StringReader templ = new StringReader(template)){
            Mustache mustache = getFactory().compile(templ, "template");
            StringWriter stringWriter = new StringWriter();
            mustache.execute(stringWriter, json);
            return stringWriter.toString();
        }
    }


}
