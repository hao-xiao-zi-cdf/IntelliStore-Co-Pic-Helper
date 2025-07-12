package com.hao_xiao_zi.intellistorecopichelper.manager;

import cn.hutool.core.io.FileUtil;
import com.hao_xiao_zi.intellistorecopichelper.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-07
 * Time: 10:40
 */
@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传文件
     *
     * @param key  存储路径
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 上传文件(返回解析图片信息)
     * 文档地址：https://cloud.tencent.com/document/product/436/55377#.E4.B8.8A.E4.BC.A0.E6.97.B6.E5.9B.BE.E7.89.87.E6.8C.81.E4.B9.85.E5.8C.96.E5.A4.84.E7.90.86
     * @param key  存储路径
     * @param file 文件
     */
    public PutObjectResult putPicture(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        // 用于图片上传后自动处理和返回元信息的配置类
        PicOperations picOperations = new PicOperations();

        // 1 代表需要解析后的图片信息
        picOperations.setIsPicInfo(1);

        // 图片压缩（转成webp格式）
        String webKey = FileUtil.mainName(file) + ".webp";
        // 创建处理规则列表
        List<PicOperations.Rule> rules = new LinkedList<>();
        // 添加处理规则，一条处理规则对应一条处理结果
        PicOperations.Rule rule = new PicOperations.Rule();
        // 存储桶
        rule.setBucket(cosClientConfig.getBucket());
        // 文件路径
        rule.setFileId(webKey);
        // 处理规则
        rule.setRule("imageMogr2/format/webp");
        rules.add(rule);

        // 添加到图片处理配置类中
        picOperations.setRules(rules);

        // 设置到请求参数
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }

}
