package com.hao_xiao_zi.intellistorecopichelper.model.dto.picture;

import com.hao_xiao_zi.intellistorecopichelper.api.aliyunai.model.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-17
 * Time: 15:03
 */
@Data
public class CreatePictureOutPaintingTaskDTO implements Serializable {

    /**
     * 图片id
     */
    private Long id;

    /**
     * 处理参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;


    private static final long serialVersionUID = 1208238192867505400L;
}
