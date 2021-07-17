# jssh-netty

服务端：
```java
@Configuration
@EnableServerEndpoint(basePackages = "xxx.service")
public class ServerNettyConfig {

    @Bean(name = "server", destroyMethod = "close")
    @ConfigurationProperties
    public DefaultServerNettyManager serverNettyManager() {
        DefaultServerNettyManager manager = new DefaultServerNettyManager();
        manager.setClientValidator(clientValidator());
        manager.setTcpPort(new InetSocketAddress(8001));
        return manager;
    }

    @Bean
    public ClientValidator clientValidator() {
        return param -> {
            Map paramMap = (Map) param;
            Integer clientId = (Integer) paramMap.get("clientId");
            //TODO 检查clientId合法性
            return new SimpleClientInfo(clientId);
        };
    }

    @Bean
    public ActionScanner actionScanner() {
        return new ActionScanner(serverNettyManager());
    }

    @EventListener(ApplicationStartedEvent.class)
    public void startNetty() throws Exception {
        serverNettyManager().start();
    }
}

@ServerEndpoint
public interface NettyEndpointService {

    Object sendMessageToClient(Object clientId, String parameters);
}

@Service
public class ServerMessageService {

    @Action
    public Object sendMessageToServer(String parameters) throws Exception {
        //服务端收到消息
        return true;
    }
}
```
当服务端向客户端发送消息时，直接调用ServerEndpoint中的方法即可，第一个参数固定为客户端标识即clientId，表示向哪个客户端发送消息。

客户端：
```java
@Configuration
@EnableClientEndpoint(basePackages = "xxx.service")
public class ClientNettyConfig {

    @Bean(name = "client", destroyMethod = "close")
    @ConfigurationProperties
    public DefaultClientNettyManager clientNettyManager() {
        DefaultClientNettyManager manager = new DefaultClientNettyManager();
        manager.setClientInfoProvider(clientInfoProvider());
        manager.setTcpPort(new InetSocketAddress("serverIp", 8001));
        return manager;
    }

    @Bean
    public ClientInfoProvider clientInfoProvider() {
        return (() -> {
            SortedMap<String, Object> clientInfo = new TreeMap<>();
            //TODO 获取当前客户端clientId
            Integer clientId = 0;
            clientInfo.put("clientId", clientId);
            return clientInfo;
        });
    }

    @Bean
    public ActionScanner actionScanner() {
        return new ActionScanner(clientNettyManager());
    }

    @EventListener(ApplicationStartedEvent.class)
    public void startNetty() throws Exception {
        clientNettyManager().start();
    }
}

@ClientEndpoint
public interface NettyEndpointService {

    Object sendMessageToServer(String parameters);
}

@Service
public class ClientMessageService {

    @Action
    public Object sendMessageToClient(String parameters) throws Exception {
        //客户端收到消息
        return true;
    }
}


```
当客户端向服务端发送消息时，直接调用ClientEndpoint中的方法即可。