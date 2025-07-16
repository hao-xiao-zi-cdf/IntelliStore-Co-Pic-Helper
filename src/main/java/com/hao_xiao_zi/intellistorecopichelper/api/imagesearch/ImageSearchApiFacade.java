package com.hao_xiao_zi.intellistorecopichelper.api.imagesearch;

import com.hao_xiao_zi.intellistorecopichelper.api.imagesearch.model.ImageSearchResult;
import com.hao_xiao_zi.intellistorecopichelper.api.imagesearch.sub.GetFirstUrlApi;
import com.hao_xiao_zi.intellistorecopichelper.api.imagesearch.sub.GetImageListApi;
import com.hao_xiao_zi.intellistorecopichelper.api.imagesearch.sub.GetImagePageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-16
 * Time: 16:40
 */
@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}
