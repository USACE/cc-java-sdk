package usace.cc.plugin.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import usace.cc.plugin.api.cloud.aws.FileStoreS3;
import usace.cc.plugin.api.eventstore.tiledb.TileDbEventStore;
import usace.cc.plugin.api.local.FileStoreLocal;


public class DataStoreTypeRegistry{
    public static Map<StoreType, Class<?>> registry;
    static{
        registry = new HashMap<>();
        registry.put(StoreType.S3, FileStoreS3.class);
        registry.put(StoreType.FS, FileStoreLocal.class);
        registry.put(StoreType.TILEDB, TileDbEventStore.class);
        
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> newStore(StoreType st) {
        if (registry.containsKey(st)){
            try{
                var clazz = registry.get(st);
                var instance = clazz.getDeclaredConstructor().newInstance();
                return Optional.of((T)instance);
            } catch (Exception ex){
                //@TODO add logging.  
                //Currently just fall thorugh to the final Optional.empty()
                ex.printStackTrace();
            }
        }
        return Optional.empty();
    }  
}

