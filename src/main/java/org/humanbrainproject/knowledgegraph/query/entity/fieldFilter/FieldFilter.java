package org.humanbrainproject.knowledgegraph.query.entity.fieldFilter;

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

    public static FieldFilter fromMap(Map<String, Object> map){
        if( map.get(FieldFilterConstants.OP) != null){
            Op op =  Op.valueOf( ((String) map.get(FieldFilterConstants.OP)).toUpperCase());
            Object m = map.get(FieldFilterConstants.VALUE);
            if(m != null) {
                if (m instanceof Map) {
                    Exp exp = fromMapRec((Map<String, Object>) m );
                    return new FieldFilter(op,exp);
                }
                if( m instanceof  String){
                    Value v = new Value((String) m);
                    return new FieldFilter(op, v);
                }
            }
        }
        return null;
    }

    private static Exp fromMapRec(Map<String, Object> map){
        return FieldFilter.fromMap( map);
    }
}
