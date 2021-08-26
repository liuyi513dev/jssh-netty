package jssh.netty.rpc.core.client;

import java.net.InetSocketAddress;

public class ServerSocketInfo {

    private InetSocketAddress channelSocket;

    public ServerSocketInfo(InetSocketAddress channelSocket) {
        this.channelSocket = channelSocket;
    }

    public InetSocketAddress getChannelSocket() {
        return channelSocket;
    }

    public void setChannelSocket(InetSocketAddress channelSocket) {
        this.channelSocket = channelSocket;
    }
}
