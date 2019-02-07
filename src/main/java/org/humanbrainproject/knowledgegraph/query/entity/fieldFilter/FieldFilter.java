package org.humanbrainproject.knowledgegraph.query.entity.fieldFilter;

import org.apache.commons.text.StringSubstitutor;
import org.humanbrainproject.knowledgegraph.query.entity.GraphQueryKeys;

import java.util.List;
import java.util.Map;

public class FieldFilter extends Exp{
    private Op op;
    private Exp exp;

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
                    Value v = new Value(StringSubstitutor.replace(value, allParameters));
                    return new FieldFilter(op, v);
                }
            }
        }
        return null;
    }

    public List<Value> getValues(List<Value> collector){
        Exp exp = getExp();
        if(exp instanceof Value){
            collector.add((Value)exp);
        }
        else if(exp instanceof FieldFilter){
            ((FieldFilter)exp).getValues(collector);
        }
        return collector;
    }


    private static Exp fromMapRec(Map<String, Object> map, Map<String, String> allParameters){
        return FieldFilter.fromMap(map, allParameters);
    }
}
