package com.hao_xiao_zi.intellistorecopichelper.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze.*;

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

    List<SpaceTagAnalyzeVO> getSpaceTagAnalyze(SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, User loginUser);

    List<SpaceSizeAnalyzeVO> getSpaceSizeAnalyze(SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, User loginUser);

    List<SpaceUserAnalyzeVO> getSpaceUserAnalyze(SpaceUserAnalyzeDTO spaceUserAnalyzeDTO, User loginUser);

    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeDTO spaceRankAnalyzeRequest, User loginUser);
}
