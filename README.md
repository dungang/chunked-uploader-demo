# chunked-uploader-demo

分片上传demo，springboot

## 默认配置是aliyun OsS

修改类文件,设置好参数，就可以测试

```java
package com.geetask.demo;

public class AliyunStorage extends AbstractStorage {
	private Logger logger  = LoggerFactory.getLogger(AliyunStorage.class);
	public OSSClient ossClient;
	public String bucketName = "输入您的bucket";
	public String accessId = "输入accessId";
	public String accessKey = "输入accessKey";
```

测试的时候，使用chrome 的调试工具 查看 network的部分 就可以看到结果