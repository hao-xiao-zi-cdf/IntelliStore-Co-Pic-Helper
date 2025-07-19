package com.hao_xiao_zi.intellistorecopichelper.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.mapper.PictureMapper;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.space.analyze.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.analyze.*;
import com.hao_xiao_zi.intellistorecopichelper.service.PictureService;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceAnalyzeService;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceService;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-17
 * Time: 20:11
 */
@Service
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
            // 处理可以为null的字段
            map.putIfAbsent("category", "未分类");
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

    @Override
    public List<SpaceTagAnalyzeVO> getSpaceTagAnalyze(SpaceTagAnalyzeDTO spaceTagAnalyzeDTO, User loginUser) {

        // 参数校验和权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeDTO, loginUser);

        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeDTO, queryWrapper);

        // 查询所有符合条件的标签
        queryWrapper.select("tags");
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        // 合并所有标签并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> {
                    String jsonToParse = tagsJson;
                    // 如果不是标准JSON数组格式，添加方括号
                    if (!tagsJson.trim().startsWith("[") && !tagsJson.trim().endsWith("]")) {
                        jsonToParse = "[" + tagsJson + "]";
                    }
                    try {
                        return JSONUtil.toList(jsonToParse, String.class).stream();
                    } catch (Exception e) {
                        // JSON解析失败时的降级处理
                        return Arrays.stream(tagsJson.split(","))
                                .map(tag -> tag.trim().replaceAll("^\"|\"$", ""))
                                .filter(tag -> !tag.isEmpty());
                    }
                })
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));


        // 按降序排序
        return tagCountMap.entrySet().stream().map(entry -> {
            String tag = entry.getKey();
            Long count = entry.getValue();
            return new SpaceTagAnalyzeVO(tag, count);
        }).sorted(Comparator.comparingLong(SpaceTagAnalyzeVO::getCount).reversed()).collect(Collectors.toList());
    }

    @Override
    public List<SpaceSizeAnalyzeVO> getSpaceSizeAnalyze(SpaceSizeAnalyzeDTO spaceSizeAnalyzeDTO, User loginUser) {

        // 参数校验和权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeDTO, loginUser);

        // 构建查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeDTO, queryWrapper);
        queryWrapper.select("picSize");

        // 查询所有符合条件的图片大小
        List<Long> picSizes = pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .map(Long::parseLong)
                .collect(Collectors.toList());

        // 定义分段范围，注意使用有序 Map，统计每个分段内图片数量和总大小
        Map<String, Long> sizeRanges = new LinkedHashMap<>();
        sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
        sizeRanges.put("100KB-500KB", picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRanges.put("500KB-1MB", picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
        sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());

        // 转化返回结果
        return sizeRanges.entrySet().stream().map(entry -> {
            String sizeRange = entry.getKey();
            Long count = entry.getValue();
            return new SpaceSizeAnalyzeVO(sizeRange, count);
        }).collect(Collectors.toList());
    }

    @Override
    public List<SpaceUserAnalyzeVO> getSpaceUserAnalyze(SpaceUserAnalyzeDTO spaceUserAnalyzeDTO, User loginUser) {
        
        // 检查权限
        checkSpaceAnalyzeAuth(spaceUserAnalyzeDTO, loginUser);

        // 构造查询条件
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long userId = spaceUserAnalyzeDTO.getUserId();
        queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);
        fillAnalyzeQueryWrapper(spaceUserAnalyzeDTO, queryWrapper);

        // 分析维度：每日、每周、每月
        String timeDimension = spaceUserAnalyzeDTO.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
        }

        // 分组和排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询结果并转换
        List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return queryResult.stream()
                .map(result -> {
                    String period = result.get("period").toString();
                    Long count = ((Number) result.get("count")).longValue();
                    return new SpaceUserAnalyzeVO(period, count);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeDTO spaceRankAnalyzeDTO, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeDTO == null, ErrorCode.PARAMS_ERROR);

        // 仅管理员可查看空间排行
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");

        // 构造查询条件
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .orderByDesc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeDTO.getTopN()); // 取前 N 名

        // 查询结果
        return spaceService.list(queryWrapper);
    }

}
