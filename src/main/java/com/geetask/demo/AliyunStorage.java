package com.geetask.demo;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ListPartsRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PartListing;
import com.aliyun.oss.model.PartSummary;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.geetask.chunked.AbstractStorage;
import com.geetask.chunked.ChunkRequest;
import com.geetask.chunked.ChunkResponse;
import com.geetask.chunked.InitRequest;
import com.geetask.chunked.InitResponse;

public class AliyunStorage extends AbstractStorage {
	
	private Logger logger  = LoggerFactory.getLogger(AliyunStorage.class);

	public OSSClient ossClient;

	public String bucketName = "输入您的bucket";
	
	public String accessId = "输入accessId";
	
	public String accessKey = "输入accessKey";


	public AliyunStorage() {
		ClientConfiguration conf = new ClientConfiguration();
		conf.setMaxErrorRetry(10);
		DefaultCredentials credentials = new DefaultCredentials(accessId, accessKey);
		DefaultCredentialProvider provider = new DefaultCredentialProvider(credentials);
		ossClient = new OSSClient("http://oss-cn-shenzhen.aliyuncs.com",provider, conf);
	}

	@PreDestroy
	public void destroy(){
		ossClient.shutdown();
	}

	/**
	 * 获取uploadId
	 * 
	 * @param key
	 * @return
	 */
	public String getUploadId(String key) {
		InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, key);
		InitiateMultipartUploadResult rst = ossClient.initiateMultipartUpload(request);
		logger.info("获取阿里云UploadId："+rst.getUploadId());
		return rst.getUploadId();
	}

	@Override
	public InitResponse initChunkUpload(InitRequest initRequest, InitResponse initResponse, HttpServletRequest request,
			HttpServletResponse response) {
		String extension = this.fileExtension(initRequest.getName());
		String fileName = DigestUtils.md5Hex(UUID.randomUUID().toString() + initRequest.getTimestamp());
		Path keyPath = Paths.get(getUploaderDir()).resolve(initRequest.getDirSuffix()).resolve(fileName + extension);
		initResponse.setUploadId(this.getUploadId(keyPath.toString()));
		initResponse.setKey(keyPath.toString().replace('\\', '/'));
		return initResponse;
	}

	@Override
	public ChunkResponse write(ChunkRequest chunkRequest, ChunkResponse chunkResponse, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		chunkResponse.setKey(chunkRequest.getKey());
		chunkResponse.setUploadId(chunkRequest.getUploadId());
		String chunks = chunkRequest.getChunks();
		// 没有分片时，执行直接保存文件
		if (null == chunks || chunks.equals("0")) {
			ObjectMetadata meta = null;
			if (chunkRequest.getType() != null) {
				meta = new ObjectMetadata();
				meta.setContentType(chunkRequest.getType());
			}
			PutObjectResult ptRst = ossClient.putObject(bucketName, chunkRequest.getKey(),
					chunkRequest.getInputStream(), meta);
			if (!(null != ptRst.getResponse() && null != ptRst.getResponse().getErrorResponseAsString())) {
				chunkResponse.setCompleted(true);
			} else {
				throw new IOException(ptRst.getResponse().getErrorResponseAsString());
			}
		} else {
			logger.info("发送给阿里云的Key："+chunkRequest.getKey());
			UploadPartResult rst = uploadPart(chunkRequest.getInputStream(), chunkRequest.getChunkSize(),
					chunkRequest.getUploadId(), Integer.valueOf(chunkRequest.getChunk()) + 1, chunkRequest.getKey(),
					chunkRequest.getType());
			if (null != rst.getResponse().getErrorResponseAsString()) {
				throw new IOException(rst.getResponse().getErrorResponseAsString());
			}
			PartListing list = getPartsByUploadId(chunkRequest.getUploadId(), chunkRequest.getKey());
			int chunksNum = Integer.valueOf(chunks);
			if (list.getParts().size() == chunksNum) {
				List<PartETag> listParts = new ArrayList<>();
				for (PartSummary sumary : list.getParts()) {
					listParts.add(new PartETag(sumary.getPartNumber(), sumary.getETag()));
				}
				CompleteMultipartUploadResult crst = completePartsUpload(chunkRequest.getUploadId(),
						chunkRequest.getKey(), listParts);
				if (null != crst.getResponse().getErrorResponseAsString()) {
					throw new IOException(crst.getResponse().getErrorResponseAsString());
				}
			}
		}

		chunkResponse.setCompleted(false);
		return chunkResponse;
	}

	@Override
	public boolean delete(String key) {
		ossClient.deleteObject(bucketName, key);
		return true;
	}

	/**
	 * 上传分片文件
	 * 
	 * @param inputStream
	 * @param partSize
	 * @param uploadId
	 * @param partNumber
	 * @param key
	 * @param contentType
	 * @return
	 */
	public UploadPartResult uploadPart(InputStream inputStream, long partSize, String uploadId, int partNumber,
			String key, String contentType) {
		UploadPartRequest request = new UploadPartRequest();
		request.setBucketName(this.bucketName);
		request.setInputStream(inputStream);
		request.setKey(key);
		request.setUploadId(uploadId);
		request.setPartNumber(partNumber);
		request.setPartSize(partSize);
		return ossClient.uploadPart(request);
	}

	/**
	 * 合并文件
	 * 
	 * @param uploadId
	 * @param key
	 * @param partETags
	 * @return
	 */
	public CompleteMultipartUploadResult completePartsUpload(String uploadId, String key, List<PartETag> partETags) {
		CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(bucketName, key, uploadId,
				partETags);
		return ossClient.completeMultipartUpload(request);
	}

	/**
	 * 查询分片
	 * 
	 * @param uploadId
	 * @param key
	 * @return
	 */
	public PartListing getPartsByUploadId(String uploadId, String key) {
		ListPartsRequest request = new ListPartsRequest(bucketName, key, uploadId);
		return ossClient.listParts(request);
	}
}
