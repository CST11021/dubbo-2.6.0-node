



伪代码

```java
public T createProxy() {
	
  // 检查配置
	checkConfig();

  // 加载注册中心URL
	List<URL> urls = loadRegistries();

  // 获取Invoker
	List<Invoker> invokers;
	for (URL url : urls) {
		invoker = protocol.refer(interfaceClass, urls);
	}

  // 构建服务字典
	Directory directory = buildDirectory(invokers);

  // 
	Invoker invoker = cluster.join(directory);

	return proxyFactory.getProxy(invoker);
}
```

