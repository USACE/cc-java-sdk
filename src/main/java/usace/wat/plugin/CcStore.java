package usace.wat.plugin;

import java.rmi.RemoteException;

public interface CcStore {
    public boolean PutObject(PutObjectInput input);
    public boolean PullObject(PullObjectInput input);
    public byte[] GetObject(GetObjectInput input) throws Exception;
    public Payload GetPayload() throws Exception;
    //public void SetPayload(Payload payload); only used in the go sdk to support cloudcompute which is written in go.
    public String RootPath();
    public boolean HandlesDataStoreType(StoreType datastoretype);
}
