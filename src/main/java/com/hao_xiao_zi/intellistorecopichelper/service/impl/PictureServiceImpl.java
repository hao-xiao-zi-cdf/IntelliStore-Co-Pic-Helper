package com.hao_xiao_zi.intellistorecopichelper.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.manager.FileManager;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.file.UploadPictureResult;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.picture.PictrueUpdateDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.picture.PictureQueryDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.picture.PictureUploadDTO;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.PictureVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.PictureService;
import com.hao_xiao_zi.intellistorecopichelper.mapper.PictureMapper;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author 34255
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-07-07 12:51:56
 */
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    public FileManager fileManager;

    @Resource
    public UserService userService;

    /**
     * 上传图片方法
     *
     * @param multipartFile    图片文件
     * @param pictureUploadDTO 包含图片上传相关属性的DTO，用于判断创建或更新图片
     * @param request          HTTP请求对象，可用于获取请求相关的信息
     * @return 返回上传图片的结果对象，包含图片的详细信息
     */
    @Override
    public Picture uploadPicture(MultipartFile multipartFile, PictureUploadDTO pictureUploadDTO, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(multipartFile, pictureUploadDTO), new BusinessException(ErrorCode.PARAMS_ERROR));

        // 判断新增或删除操作
        Long pictureId = pictureUploadDTO.getId();
        if (pictureId != null) {
            // 查看数据库中的记录是否存在
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<Picture>().eq("id", pictureId);
            ThrowUtils.throwIf(ObjectUtil.isEmpty(getOne(queryWrapper)), new BusinessException(ErrorCode.PARAMS_ERROR, "编辑的图片不存在"));
        }

        // 拼接上传路径(/公共or私有/用户id)
        String uploadPathPrefix = String.format("public/%s", userService.getLoginUser(request).getId());

        // 上传到COS对象存储
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        // 解析返回的图片信息，封装对象
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(userService.getLoginUser(request).getId());

        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间  
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }

        // 保存或更新到数据库
        boolean isOk = saveOrUpdate(picture);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败，数据库错误"));

        return picture;
    }

    /**
     * 删除图片资源
     *
     * @param id      图片ID
     * @param request 用于获取当前登录用户信息
     */
    @Override
    public void pictureDelete(Long id, HttpServletRequest request) {
        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(id), new BusinessException(ErrorCode.PARAMS_ERROR));

        // 查询图片是否存在
        Picture picture = getById(id);
        ThrowUtils.throwIf(picture == null, new BusinessException(ErrorCode.PARAMS_ERROR, "图片资源不存在"));

        // 判断是否为本人或管理员操作
        User loginUser = userService.getLoginUser(request);
        if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 删除图片
        boolean isOk = removeById(id);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "删除图片失败"));
    }

    /**
     * 根据图片ID获取图片资源
     *
     * @param id 图片ID
     * @return 返回查询到的图片对象, 包含详细信息
     */
    @Override
    public Picture getPictureById(Long id) {
        // 参数校验
        ThrowUtils.throwIf(id == null || id < 0, new BusinessException(ErrorCode.PARAMS_ERROR));

        // 查询图片是否存在
        Picture picture = query().eq("id", id).one();
        ThrowUtils.throwIf(picture == null, new BusinessException(ErrorCode.PARAMS_ERROR, "图片资源不存在"));

        // 返回图片资源
        return picture;
    }

    /**
     * 更新图片信息
     * 该方法接收一个包含图片更新信息的数据传输对象，并根据该对象中的信息更新数据库中的图片记录、
     *
     * @param pictrueUpdateDTO 包含图片更新信息的数据传输对象，包括需要更新的图片ID和其他相关信息
     */
    @Override
    public void pictureUpdate(PictrueUpdateDTO pictrueUpdateDTO) {

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(pictrueUpdateDTO), ErrorCode.NOT_FOUND_ERROR);
        Long pictureId = pictrueUpdateDTO.getId();
        ThrowUtils.throwIf(pictureId == null || pictureId < 0, new BusinessException(ErrorCode.PARAMS_ERROR));

        // 查询图片资源是否存在
        Picture picture = query().eq("id", pictureId).one();
        ThrowUtils.throwIf(picture == null, new BusinessException(ErrorCode.PARAMS_ERROR, "图片资源不存在"));

        // 更新图片资源
        Picture newPicture = BeanUtil.copyProperties(pictrueUpdateDTO, Picture.class);

        // 获取更新条件构造器
        UpdateWrapper<Picture> wrapper = getPictureUpdateWrapper(pictrueUpdateDTO);

        // 更新
        boolean isOk = update(wrapper);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "更新图片失败"));
    }

    /**
     * 编辑图片信息
     *
     * @param pictrueUpdateDTO 图片更新数据传输对象，包含要编辑的图片信息
     * @param request          用于获取登录用户信息，判断是否为本人操作
     */
    @Override
    public void pictureEdit(PictrueUpdateDTO pictrueUpdateDTO, HttpServletRequest request) {

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(pictrueUpdateDTO), ErrorCode.NOT_FOUND_ERROR);
        Long pictureId = pictrueUpdateDTO.getId();
        ThrowUtils.throwIf(pictureId == null || pictureId < 0, new BusinessException(ErrorCode.PARAMS_ERROR));

        // 查询图片资源是否存在
        Picture picture = query().eq("id", pictureId).one();
        ThrowUtils.throwIf(picture == null, new BusinessException(ErrorCode.PARAMS_ERROR, "图片资源不存在"));

        // 判断是否为本人操作
        ThrowUtils.throwIf(!userService.getLoginUser(request).getId().equals(picture.getUserId()), new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限，非本人操作"));

        // 编辑图片资源
        Picture newPicture = BeanUtil.copyProperties(pictrueUpdateDTO, Picture.class);

        // 获取更新条件构造器
        UpdateWrapper<Picture> wrapper = getPictureUpdateWrapper(pictrueUpdateDTO);

        // 设置编辑时间
        wrapper.set("editTime", Timestamp.valueOf(LocalDateTime.now()));

        // 更新
        boolean isOk = update(wrapper);
        ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "更新图片失败"));
    }

    /**
     * 根据PictureUpdateDTO对象构建一个UpdateWrapper对象，用于更新图片信息
     * 此方法专注于处理图片信息的更新，确保仅更新传入的非空字段
     *
     * @param pictrueUpdateDTO 包含待更新图片信息的数据传输对象
     * @return 一个UpdateWrapper对象，用于执行数据库更新操作
     */
    @Override
    public UpdateWrapper<Picture> getPictureUpdateWrapper(PictrueUpdateDTO pictrueUpdateDTO) {
        Long id = pictrueUpdateDTO.getId();
        String url = pictrueUpdateDTO.getUrl();
        String name = pictrueUpdateDTO.getName();
        String introduction = pictrueUpdateDTO.getIntroduction();
        String category = pictrueUpdateDTO.getCategory();

        String tags = CollUtil.join(pictrueUpdateDTO.getTags(), ",");


        UpdateWrapper<Picture> wrapper = new UpdateWrapper<>();
        wrapper.set(StrUtil.isNotBlank(url), "url", url);

        wrapper.set(StrUtil.isNotBlank(name), "name", name);
        wrapper.set(StrUtil.isNotBlank(introduction), "introduction", introduction);
        wrapper.set(StrUtil.isNotBlank(category), "category", category);
        wrapper.set(StrUtil.isNotBlank(tags), "tags", tags);
        wrapper.eq("id", id);
        return wrapper;
    }

    /**
     * 执行图片分页查询的方法
     * 该方法根据提供的查询条件和分页参数，返回一个分页的图片列表
     *
     * @param pictureQueryDTO 图片查询条件封装对象，包含分页参数和搜索文本等信息
     * @return 返回一个分页对象，包含查询到的图片列表和分页信息
     */
    @Override
    public IPage<Picture> picturePageQuery(PictureQueryDTO pictureQueryDTO) {

        // 校验参数
        ThrowUtils.throwIf(pictureQueryDTO == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        // 获取分页参数
        int current = pictureQueryDTO.getCurrent();
        int pageSize = pictureQueryDTO.getPageSize();
        String searchText = pictureQueryDTO.getSearchText();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(current, pageSize, searchText) || current < 0 || pageSize < 0, new BusinessException(ErrorCode.PARAMS_ERROR));

        // 设置分页参数
        Page<Picture> picturePage = new Page<>(current, pageSize);

        // 设置其他查询参数
        QueryWrapper<Picture> queryWrapper = getQueryWrapper(pictureQueryDTO);

        // 查询返回结果
        return page(picturePage, queryWrapper);
    }

    /**
     * 根据查询条件分页查询图片信息，并关联用户信息
     *
     * @param pictureQueryDTO 图片查询条件DTO，包含分页信息和查询条件
     * @return 返回填充了脱敏后图片信息和关联脱敏后信息的分页对象
     */
    @Override
    public IPage<PictureVO> picturePageVoQuery(PictureQueryDTO pictureQueryDTO) {
        // 调用方法获取未脱敏图片信息列表
        Page<Picture> picturePage = (Page<Picture>) picturePageQuery(pictureQueryDTO);

        // 对象拷贝，获取用户ids
        Page<PictureVO> pictureVOPage = new Page<>(pictureQueryDTO.getCurrent(), pictureQueryDTO.getPageSize());

        List<PictureVO> pictureVOList = new ArrayList<>();
        List<Long> userIds = new ArrayList<>();

        // 提取 userIds 并创建 VO 对象（未赋值 user 属性）
        for (Picture picture : picturePage.getRecords()) {
            PictureVO pictureVO = PictureVO.objToVo(picture);
            pictureVOList.add(pictureVO);
            userIds.add(picture.getUserId());
        }

        // 批量查询用户信息
        List<User> users = userService.listByIds(userIds);

        // 构建 Map<userId, UserVO>，便于后续快速查找
        Map<Long, UserVO> userVOMap = new HashMap<>();
        for (User user : users) {
            UserVO userVO = BeanUtil.copyProperties(user, UserVO.class);
            userVOMap.put(user.getId(), userVO);
        }

        // 为每个 PictureVO 设置对应的 UserVO
        for (PictureVO pictureVO : pictureVOList) {
            Long userId = pictureVO.getUserId();
            if (userId != null && userVOMap.containsKey(userId)) {
                pictureVO.setUser(userVOMap.get(userId));
            }
        }

        // 设置分页结果
        pictureVOPage.setRecords(pictureVOList);
        pictureVOPage.setTotal(picturePage.getTotal());

        return pictureVOPage;
    }

    /**
     * 返回图片分页查询条件构造器
     * 用于在图片查询中构建复杂的查询条件，包括多字段搜索、排序等
     *
     * @param pictureQueryDTO 包含查询条件的DTO对象
     * @return QueryWrapper对象，用于执行数据库查询
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryDTO pictureQueryDTO) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        Long id = pictureQueryDTO.getId();
        String name = pictureQueryDTO.getName();
        String introduction = pictureQueryDTO.getIntroduction();
        String category = pictureQueryDTO.getCategory();
        List<String> tags = pictureQueryDTO.getTags();
        Long picSize = pictureQueryDTO.getPicSize();
        Integer picWidth = pictureQueryDTO.getPicWidth();
        Integer picHeight = pictureQueryDTO.getPicHeight();
        Double picScale = pictureQueryDTO.getPicScale();
        String picFormat = pictureQueryDTO.getPicFormat();
        String searchText = pictureQueryDTO.getSearchText();
        Long userId = pictureQueryDTO.getUserId();
        String sortField = pictureQueryDTO.getSortField();
        String sortOrder = pictureQueryDTO.getSortOrder();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(
                    qw -> qw.like(StringUtils.isNotBlank(searchText), "name", searchText)
                            .or()
                            .like(StringUtils.isNotBlank(searchText), "introduction", searchText)
            );

        }

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);

        for (String tag : tags) {
            queryWrapper.like(StrUtil.isNotBlank(tag), "tags", "\"" + tag + "\"");
        }

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }
}




