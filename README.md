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

    @Bean
    public ClientValidator clientValidator() {
        return param -> {
            Map paramMap = (Map) param;
            Integer clientId = (Integer) paramMap.get("clientId");
            //TODO 检查clientId合法性
            return new SimpleClientInfo(clientId);
        };
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

## 开启TLS
***默认情况下没有开启TLS，如需开启TLS做如下配置***

### 服务端配置
```properties
jssh.netty.server.configuration.ssl.enable=true
jssh.netty.server.configuration.ssl.keyStorePath=/ssl/serverkey.jks
jssh.netty.server.configuration.ssl.keyStorePassword=storepass
jssh.netty.server.configuration.ssl.keyPassword=keypass
jssh.netty.server.configuration.ssl.protocol=TLSv1.2
jssh.netty.server.configuration.ssl.clientMode=false
jssh.netty.server.configuration.ssl.needClientAuth=true
```

### 客户端配置
```properties
jssh.netty.client.configuration.ssl.enable=true
jssh.netty.client.configuration.ssl.keyStorePath=/ssl/clientkey.jks
jssh.netty.client.configuration.ssl.keyStorePassword=storepass
jssh.netty.client.configuration.ssl.keyPassword=keypass
jssh.netty.client.configuration.ssl.protocol=TLSv1.2
jssh.netty.client.configuration.ssl.clientMode=true
jssh.netty.client.configuration.ssl.needClientAuth=true
```

### 用keytool生成证书

将生成的证书放到resources/ssl/目录下

***单向认证-客户端校验服务端***
```
生成服务端证书及证书仓库
keytool -genkey -alias "serverkey" -keysize 2048 -validity 365 -keyalg RSA -dname "CN=localhost" -keypass "keypass" -storepass "storepass" -keystore "d:\key\serverkey.jks"

导出服务端证书
keytool -export -alias "serverkey" -keystore "d:\key\serverkey.jks" -storepass "storepass" -file "d:\key\serverkey.cer"

生成客户端证书及证书仓库
keytool -genkey -alias "clientkey" -keysize 2048 -validity 365 -keyalg RSA -dname "CN=localhost" -keypass "keypass" -storepass "storepass" -keystore "d:\key\clientkey.jks"

将服务端证书导入客户端证书仓库中
keytool -import -trustcacerts -alias "serverkey" -file "D:\key\serverkey.cer" -storepass "storepass" -keystore "D:\key\clientkey.jks"
```
***双向认证***
```
导出客户端证书
keytool -export -alias "clientkey" -keystore "d:\key\clientkey.jks" -storepass "storepass" -file "d:\key\clientkey.cer"

将客户端证书导入服务端证书仓库中
keytool -import -trustcacerts -alias "clientkey" -file "D:\key\clientkey.cer" -storepass "storepass" -keystore "D:\key\serverkey.jks"
```

## 自定义序列化
***如有需要可自定义序列化方法，***
***比如用json替换系统内置的序列化方式***

```java
@Bean
public FileMessageSerialFactory fileMessageSerialFactory() {
    return JsonSerial::new;
}

static class JsonSerial extends DefaultSerial {

    static {
        ParserConfig.getGlobalInstance().setAutoTypeSupport(true);
    }

    @Override
    protected boolean writeByCustomSerial(ByteBuf buf, Object obj) {

        String json = JSONObject.toJSONString(obj, SerializerFeature.WriteClassName);
        byte[] bytes = json.getBytes(DefaultSerial.default_charset);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
        return true;
    }

    @Override
    protected Object readByCustomSerial(ByteBuf buf) {
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        String json = new String(bytes, DefaultSerial.default_charset);
        return JSONObject.parse(json);
    }
}
```
