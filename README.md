# jssh-netty

## 服务端：

### pom.xml中添加依赖

```xml
<dependency>
    <groupId>com.jssh.netty</groupId>
    <artifactId>netty-starter</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 配置Config
```java
@Configuration
public class ServerNettyConfig {

    @Resource
    @Lazy
    private DefaultServerNettyManager serverNettyManager;

    @Bean
    public ClientValidator clientValidator() {
        return param -> {
            Map paramMap = (Map) param;
            Integer clientId = (Integer) paramMap.get("clientId");
            //TODO 检查clientId合法性
            return new SimpleClientInfo(clientId);
        };
    }

    @EventListener(ApplicationStartedEvent.class)
    public void startNetty() throws Exception {
        serverNettyManager.start();
    }
}
```
### 配置application.properties

```properties
#指定Netty监听端口
jssh.netty.server.port=8090
```

### 服务端向客户端发送消息
```java
/**
自动生成代理对象，不需要实现类
**/
@ServerEndpoint
public interface NettyEndpointService {

    Object sendMessageToClient(Object clientId, String parameters);
}
```

### 服务端接收客户端的消息
```java
@Service
public class ServerMessageService {

    @Action
    public Object sendMessageToServer(String parameters) throws Exception {
        //服务端收到消息
        return true;
    }
}
```
***当服务端向客户端发送消息时，直接调用NettyEndpointService中的方法即可，第一个参数固定为客户端标识即clientId，表示向哪个客户端发送消息。***

## 客户端：

### pom.xml中添加依赖

```xml
<dependency>
    <groupId>com.jssh.netty</groupId>
    <artifactId>netty-starter</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</dependency>
```

### 配置Config
```java
@Configuration
public class ClientNettyConfig {

    @Resource
    @Lazy
    private DefaultClientNettyManager defaultClientNettyManager;

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

    @EventListener(ApplicationStartedEvent.class)
    public void startNetty() throws Exception {
        defaultClientNettyManager.start();
    }
}
```

### 配置application.properties

```properties
#指定服务端
jssh.netty.client.host=127.0.0.1
jssh.netty.client.port=8090
```

### 客户端向服务端发送消息
```java
/**
自动生成代理对象，不需要实现类
**/
@ClientEndpoint
public interface NettyEndpointService {

    Object sendMessageToServer(String parameters);
}
```

### 客户端接收服务端的消息

```java
@Service
public class ClientMessageService {

    @Action
    public Object sendMessageToClient(String parameters) throws Exception {
        //客户端收到消息
        return true;
    }
}
```
***当客户端向服务端发送消息时，直接调用NettyEndpointService中的方法即可。***