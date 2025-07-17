package com.hao_xiao_zi.intellistorecopichelper.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.SpaceAnalyzeDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.SpaceCategoryAnalyzeDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.SpaceUsageAnalyzeDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze.SpaceCategoryAnalyzeVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze.SpaceUsageAnalyzeVO;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-17
 * Time: 20:11
 */
public interface SpaceAnalyzeService {

    Boolean checkSpaceAnalyzeAuth(SpaceAnalyzeDTO spaceAnalyzeDTO, User loginUser);

    void fillAnalyzeQueryWrapper(SpaceAnalyzeDTO spaceAnalyzeDTO, QueryWrapper<Picture> queryWrapper);

    SpaceUsageAnalyzeVO getSpaceUsageAnalyze(SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO, User loginUser);

    List<SpaceCategoryAnalyzeVO> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO, User loginUser);
}
