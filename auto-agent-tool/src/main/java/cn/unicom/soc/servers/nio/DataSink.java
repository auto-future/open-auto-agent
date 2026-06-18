package cn.unicom.soc.servers.nio;

/**
 * @Description
 * @Author chenss
 * @CreateTime 2025-07-12 08:11:22
 * @ModifyTime
 */
public interface DataSink {
    void sendData(String requestData);

    String sendDataAndStatus(String string);
}
