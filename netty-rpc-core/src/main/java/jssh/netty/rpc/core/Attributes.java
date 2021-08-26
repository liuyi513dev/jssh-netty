package jssh.netty.rpc.core;

import io.netty.util.AttributeKey;
import jssh.netty.rpc.core.server.ClientInfo;

public class Attributes {

    public static final AttributeKey<ClientInfo<?>> CLIENT_INFO = AttributeKey.newInstance("clientInfo");

    public static final AttributeKey<String> CHANNEL_NAME = AttributeKey.newInstance("channelName");
}
