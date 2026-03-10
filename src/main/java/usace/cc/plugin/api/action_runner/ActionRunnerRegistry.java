package usace.cc.plugin.api.action_runner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ActionRunnerRegistry {

    //Static methods
    private static ActionRunnerRegistry instance = null;

    public static ActionRunnerRegistry getInstance(){
        if (instance==null){
            instance = new ActionRunnerRegistry();
        }
        return instance;
    }
    
    private Map<String, Class<? extends ActionRunner>> runnerClasses = new HashMap<>();

    public ActionRunnerRegistry(){
        runnerClasses = new HashMap<>();
     }
    public void registerActionRunnerClass(String name, Class<? extends ActionRunner> clazz) {
        runnerClasses.put(name, clazz);
    }
    public Optional<Class<? extends ActionRunner>> getActionRunnerClass(String name) {
        return Optional.ofNullable(runnerClasses.get(name));
    }





    


}




