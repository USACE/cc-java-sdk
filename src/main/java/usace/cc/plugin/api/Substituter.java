package usace.cc.plugin.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Substituter {

    public static class InvalidSubstitutionException extends RuntimeException{
        InvalidSubstitutionException(String msg){
            super(msg);
        }
    }

    public static class EmbeddedVar{
        public String template;
        public String type;
        public String varname;
        public boolean isArrayOrMap;
        public int arrayIndex;
        public String mapIndex;

        public static EmbeddedVar matcherToEmbeddedVar(Matcher matcher){
            var ev = new EmbeddedVar();
            ev.template=matcher.group(0);
            ev.type=matcher.group(1);
            ev.varname=matcher.group(2);
            ev.arrayIndex=-1;
            if (matcher.group(3)!=null){
                ev.isArrayOrMap=true;
            } else if (matcher.group(4)!=null){
                //matched a numeric array index
                ev.isArrayOrMap=true;
                var i = Integer.parseInt(matcher.group(4));
                ev.arrayIndex=i;                
            } else if (matcher.group(5)!=null){
                ev.isArrayOrMap=true;
                ev.mapIndex=matcher.group(5);
            } else if (matcher.group(6)!=null){
                ev.isArrayOrMap=true;
                ev.mapIndex=matcher.group(6);
            }
            return ev;
        }
 
    }

    private static final Pattern substitutionPattern = Pattern.compile(
        "\\{(ATTR|VAR|ENV)::([a-zA-Z_][a-zA-Z0-9_-]*)(?:" +
            "(\\[\\s*\\])" +                     // group 3: captures "[]" when present
            "|\\[\\s*([0-9]+)\\s*\\]" +          // group 4: numeric index
            "|\\[\\s*'([^']*)'\\s*\\]" +         // group 5: single-quoted key
            "|\\[\\s*\"([^\"]*)\"\\s*\\]" +      // group 6: double-quoted key
        ")?\\}"
    );

    public static List<EmbeddedVar> match(String subval){
        Matcher matcher = substitutionPattern.matcher(subval);
        var embeddedVars = new ArrayList<EmbeddedVar>();
        while (matcher.find()) {
            var ev = EmbeddedVar.matcherToEmbeddedVar(matcher);
            embeddedVars.add(ev);
        }
        return embeddedVars;
    }

    public static Object getSubstitutionValue(EmbeddedVar ev, Map<String,Object> attributes) {
        switch(ev.type){
            case "ATTR":
                return attributes.get(ev.varname);
            case "ENV":
                return System.getenv(ev.varname);
            default:
                return null;
        }
    }

    public static Map<String,String> parameterSubstitution(String templateKey, String template, Map<String,Object> attributes, boolean allowAttrSubstitution){
        var output = new HashMap<String,String>();
        output.put(templateKey, template);

        var embeddedVars = match(template);
        for (var ev : embeddedVars){

            //if this is an ATTR sub, and we are disallowing ATTR subss, then skip the loop item
            if (ev.type=="ATTR" && !allowAttrSubstitution){
                continue;
            }

            //process the substitution
            var evVal = getSubstitutionValue(ev, attributes);
            if(evVal==null){
                throw new InvalidSubstitutionException(String.format("invalid substitution for %s in %s", ev.varname, template));
            }
             
            if (evVal instanceof String){
                var sval = (String)evVal;
                for (Map.Entry<String, String> outputset  : output.entrySet()) {
                    var tmpl=outputset.getValue();
                    output.put(outputset.getKey(), tmpl.replace(ev.template,sval));
                }
            } else if (evVal instanceof List){
                var lvals = (List<?>)evVal;
                if (ev.arrayIndex==-1 && ev.isArrayOrMap){
                    //-1 -> inflate the entire array into the parameter
                    
                    var newoutput = new HashMap<String,String>();
                    for (Map.Entry<String, String> outputset  : output.entrySet()) {
                        var tmpl=outputset.getValue();
                        var key = outputset.getKey();
                        for (int i=0;i<lvals.size();i++){
                            Object lval = lvals.get(i);
                            var newkey=String.format("%s-%s",key,i);
                            newoutput.put(newkey, tmpl.replace(ev.template,lval.toString()));
                        }
                    }
                    output=newoutput;
                } else if (ev.arrayIndex > -1 && ev.isArrayOrMap){
                    //have an index.  substitute on the index
                    var val = lvals.get(ev.arrayIndex);
                    for (Map.Entry<String, String> outputset  : output.entrySet()) {
                        var tmpl=outputset.getValue();
                        output.put(outputset.getKey(), tmpl.replace(ev.template,val.toString()));
                    }
                } else {
                    //have a List but the user referenced the var without array semantics
				    //concatonate the slice into csv and substitute the csv string
                    var sval = lvals.stream()
                                .map(Object::toString)
                                .collect(Collectors.joining(","));
                    for (Map.Entry<String, String> outputset  : output.entrySet()) {
                        var tmpl=outputset.getValue();
                        output.put(outputset.getKey(), tmpl.replace(ev.template,sval));
                    }
                    
                }
            } else if (evVal instanceof Map) {
                //all keys MUST be strings
                var lvals = (Map<String,?>)evVal;
                if(ev.mapIndex==null && ev.isArrayOrMap){
                    //no map index, so inflate the entire map into the parameter
                    var newoutput = new HashMap<String,String>();
                    for (Map.Entry<String, String> outputset  : output.entrySet()) {
                        for(Map.Entry<String,?> lvalEntry:lvals.entrySet()){
                            var lvalkey = lvalEntry.getKey();
                            var strval = lvalEntry.getValue().toString();
                            var newkey = String.format("%s-%s", outputset.getKey(),lvalkey);
                            var tmpl = outputset.getValue();
                            newoutput.put(newkey, tmpl.replace(ev.template, strval));
                        }
                    }
                    output=newoutput;
                
                //have a map index, so get the map value and substitute
                } else {
                    for (Map.Entry<String, String> outputset  : output.entrySet()) {
                        var lval=lvals.get(ev.mapIndex);
                        var tmpl = outputset.getValue();
                        output.put(outputset.getKey(), tmpl.replace(ev.template, lval.toString()));
                    } 
                }
            } else {
               //handle same as a string, but coerce values to a string
                for (Map.Entry<String, String> outputset  : output.entrySet()) {
                    var tmpl=outputset.getValue();
                    output.put(outputset.getKey(), tmpl.replace(ev.template,evVal.toString()));
                }
            }
        }


        return output;
    }
}
