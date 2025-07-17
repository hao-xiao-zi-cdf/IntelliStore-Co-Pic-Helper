package com.hao_xiao_zi.intellistorecopichelper.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.mapper.PictureMapper;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.SpaceAnalyzeDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.SpaceCategoryAnalyzeDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.SpaceUsageAnalyzeDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze.SpaceCategoryAnalyzeVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze.SpaceUsageAnalyzeVO;
import com.hao_xiao_zi.intellistorecopichelper.service.PictureService;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceAnalyzeService;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceService;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-17
 * Time: 20:11
 */
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    @Override
    public Boolean checkSpaceAnalyzeAuth(SpaceAnalyzeDTO spaceAnalyzeDTO, User loginUser) {

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(spaceAnalyzeDTO), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        boolean queryPublic = spaceAnalyzeDTO.isQueryPublic();
        boolean queryAll = spaceAnalyzeDTO.isQueryAll();

        // 是否为管理员,查询所有图片或公开图片
        if (queryAll || queryPublic){
            if (!userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员，无权限");
            }
            return false;
        }else{
            // 查询指定空间图片
            Long spaceId = spaceAnalyzeDTO.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getSpaceById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR,"空间资源不存在");
            // 判断当前用户是否为空间创建者或管理员
            if (!Objects.equals(loginUser.getId(), space.getUserId()) || !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"非空间创建人或管理员，无权限");
            }
            return true;
        }
    }

    @Override
    public void fillAnalyzeQueryWrapper(SpaceAnalyzeDTO spaceAnalyzeDTO, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeDTO.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeDTO.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeDTO.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    @Override
    public SpaceUsageAnalyzeVO getSpaceUsageAnalyze(SpaceUsageAnalyzeDTO spaceUsageAnalyzeDTO, User loginUser) {

        // 参数校验和权限校验
        Boolean isPersonal = checkSpaceAnalyzeAuth(spaceUsageAnalyzeDTO, loginUser);

        // 区分查询公共图库，全部图片或指定空间图片
        if (isPersonal) {
            // 查询指定空间图片
            Long spaceId = spaceUsageAnalyzeDTO.getSpaceId();
            Space space = spaceService.getSpaceById(spaceId);
            return SpaceUsageAnalyzeVO.builder()
                    .usedSize(space.getTotalSize())
                    .maxSize(space.getMaxSize())
                    .sizeUsageRatio(NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(),2).doubleValue())
                    .usedCount(space.getTotalCount())
                    .maxCount(space.getMaxCount())
                    .countUsageRatio(NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(),2).doubleValue())
                    .build();
        }else{
            // 查询公共图库图片或所有图片
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeDTO, queryWrapper);
            // 在数据库层计算图片总大小和总张数
            queryWrapper.select("sum(picSize) as usedSize", "count(*) as usedCount");
            Map<String, Object> result = pictureService.getBaseMapper().selectMaps(queryWrapper).get(0);
            return SpaceUsageAnalyzeVO.builder()
                    .usedSize(Long.parseLong(result.get("usedSize").toString()))
                    .maxSize(null)
                    .sizeUsageRatio(null)
                    .usedCount(Long.parseLong(result.get("usedCount").toString()))
                    .maxCount(null)
                    .countUsageRatio(null).build();
        }
    }

    @Override
    public List<SpaceCategoryAnalyzeVO> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeDTO spaceCategoryAnalyzeDTO, User loginUser) {

        // 参数校验和权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeDTO, loginUser);

        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeDTO, queryWrapper);
        queryWrapper.select("category", "count(*) as count", "sum(picSize) as totalSize");
        queryWrapper.groupBy("category");

        // 查询并转换结果
        List<Map<String, Object>> result = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return result.stream().map(map -> {
            String category = map.get("category").toString();
            Long count = Long.parseLong(map.get("count").toString());
            Long totalSize = Long.parseLong(map.get("totalSize").toString());
            return SpaceCategoryAnalyzeVO.builder()
                    .category(category)
                    .count(count)
                    .totalSize(totalSize)
                    .build();
        }).collect(Collectors.toList());
    }
}
