package org.humanbrainproject.knowledgegraph.query.entity.fieldFilter;

import org.humanbrainproject.knowledgegraph.query.entity.GraphQueryKeys;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldFilter extends Exp{
    private Op op;
    private Exp exp;

    private static Pattern PARAMETER_PATTERN = Pattern.compile("^\\$\\{(.+)\\}$");

    public FieldFilter(Op op, Exp exp) {
        this.op = op;
        this.exp = exp;
    }

    public Op getOp() {
        return op;
    }

    public Exp getExp() {
        return exp;
    }

    public static FieldFilter fromMap(Map<String, Object> map, Map<String, String> allParameters){
        if(map.get(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName()) != null){
            Op op =  Op.valueOf( ((String) map.get(GraphQueryKeys.GRAPH_QUERY_FILTER_OP.getFieldName())).toUpperCase());
            Object m = map.get(GraphQueryKeys.GRAPH_QUERY_FILTER_VALUE.getFieldName());
            if(m != null) {
                if (m instanceof Map) {
                    Exp exp = fromMapRec((Map<String, Object>) m, allParameters );
                    return new FieldFilter(op,exp);
                }
                if( m instanceof  String){
                    String value = (String)m;
                    Matcher matcher = PARAMETER_PATTERN.matcher(value);
                    Value v;
                    if(matcher.find()){
                        String theGroup = matcher.group(1).toLowerCase();
                        String t = null;
                        if(allParameters != null){
                            t = allParameters.get(theGroup);
                        }
                        if(t == null){
                            t = value;
                        }
                        v = new Value(t);
                    }else{
                        v = new Value(value);
                    }
                    return new FieldFilter(op, v);
                }
            }
        }
        return null;
    }

    private static Exp fromMapRec(Map<String, Object> map, Map<String, String>allParameters){
        return FieldFilter.fromMap(map, allParameters);
    }
}
