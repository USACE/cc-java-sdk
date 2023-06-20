package usace.cc.plugin;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public abstract class ActionParameterBase extends StdDeserializer<ActionParameterBase> {

    protected ActionParameterBase(Class<?> vc) {
        super(vc);
    }
    public abstract ActionParameterBase UpdatePaths();
}
