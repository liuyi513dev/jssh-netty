package jssh.netty.rpc.core.server;

import java.net.InetSocketAddress;

public class ServerBindInfo {

    private InetSocketAddress channelSocket;

    public ServerBindInfo(InetSocketAddress channelSocket) {
        this.channelSocket = channelSocket;
    }

    public InetSocketAddress getChannelSocket() {
        return channelSocket;
    }

    public void setChannelSocket(InetSocketAddress channelSocket) {
        this.channelSocket = channelSocket;
    }
}
