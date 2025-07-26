package com.hao_xiao_zi.intellistorecopichelper.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hao_xiao_zi.intellistorecopichelper.api.aliyunai.AliYunAiApi;
import com.hao_xiao_zi.intellistorecopichelper.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.hao_xiao_zi.intellistorecopichelper.api.aliyunai.model.CreateoutPaintingTaskResponse;
import com.hao_xiao_zi.intellistorecopichelper.api.imagesearch.ImageSearchApiFacade;
import com.hao_xiao_zi.intellistorecopichelper.api.imagesearch.model.ImageSearchResult;
import com.hao_xiao_zi.intellistorecopichelper.common.ResultUtils;
import com.hao_xiao_zi.intellistorecopichelper.exception.BusinessException;
import com.hao_xiao_zi.intellistorecopichelper.exception.ErrorCode;
import com.hao_xiao_zi.intellistorecopichelper.exception.ThrowUtils;
import com.hao_xiao_zi.intellistorecopichelper.manager.CosManager;
import com.hao_xiao_zi.intellistorecopichelper.manager.auth.SpaceUserAuthManager;
import com.hao_xiao_zi.intellistorecopichelper.manager.auth.StpKit;
import com.hao_xiao_zi.intellistorecopichelper.manager.auth.annotation.SaSpaceCheckPermission;
import com.hao_xiao_zi.intellistorecopichelper.manager.auth.model.SpaceUserPermissionConstant;
import com.hao_xiao_zi.intellistorecopichelper.manager.upload.FileUploadByLocal;
import com.hao_xiao_zi.intellistorecopichelper.manager.upload.FileUploadTemplate;
import com.hao_xiao_zi.intellistorecopichelper.manager.upload.FileUploadURL;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.file.UploadPictureResult;
import com.hao_xiao_zi.intellistorecopichelper.model.dto.picture.*;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Picture;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.Space;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import com.hao_xiao_zi.intellistorecopichelper.model.enums.PictureReviewStatusEnum;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.PictureVO;
import com.hao_xiao_zi.intellistorecopichelper.model.vo.UserVO;
import com.hao_xiao_zi.intellistorecopichelper.service.PictureService;
import com.hao_xiao_zi.intellistorecopichelper.mapper.PictureMapper;
import com.hao_xiao_zi.intellistorecopichelper.service.SpaceService;
import com.hao_xiao_zi.intellistorecopichelper.service.UserService;
import com.hao_xiao_zi.intellistorecopichelper.utils.ColorSimilarUtils;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.hutool.core.collection.CollUtil.forEach;
import static com.hao_xiao_zi.intellistorecopichelper.constant.RedisConstant.PICTURE_QUERY_LIST_VO_KEY;

/**
 * @author 34255
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-07-07 12:51:56
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    public UserService userService;

    @Resource
    private FileUploadByLocal fileUploadByLocal;

    @Resource
    private FileUploadURL fileUploadURL;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CosManager cosManager;

    @Resource
    private SpaceService spaceService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private AliYunAiApi aliYunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;



    // 使用 Caffeine 构建一个本地缓存，用于存储字符串键值对
    // 初始容量为1024键值对，最大容量为 10000键值对
    // 缓存中的数据在写入后 5 分钟过期，以防止数据长期未使用占用资源
    private final Cache<String, String> LOCAL_CACHE =
        Caffeine.newBuilder().initialCapacity(1024)
                .maximumSize(10000L)
                // 缓存 5 分钟移除
                .expireAfterWrite(5L, TimeUnit.MINUTES)
                .build();

    /**
     * 上传图片方法
     *
     * @param inputSource    输入源
     * @param pictureUploadDTO 包含图片上传相关属性的DTO，用于判断创建或更新图片
     * @param request          HTTP请求对象，可用于获取请求相关的信息
     * @return 返回上传图片的结果对象，包含图片的详细信息
     */
    @Override
    public Picture uploadPicture(Object inputSource, PictureUploadDTO pictureUploadDTO, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 校验参数
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(inputSource, pictureUploadDTO), new BusinessException(ErrorCode.PARAMS_ERROR));

        // 校验空间是否存在
        Long spaceId = pictureUploadDTO.getSpaceId();
        Space space = null;
        if (spaceId != null) {
            space = spaceService.getSpaceById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            已改为注解权限校验
//            // 必须空间创建人才能上传
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
//            }
            // 校验额度
            if(space.getTotalCount() > space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"空间条数不足");
            }
            if(space.getTotalSize() > space.getMaxSize()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"空间大小不足");
            }
        }

        // 判断新增或更新操作
        Long pictureId = pictureUploadDTO.getId();
        if (pictureId != null) {
            // 查看数据库中的记录是否存在
            Picture picture = getById(pictureId);
            ThrowUtils.throwIf(ObjectUtil.isEmpty(picture), new BusinessException(ErrorCode.PARAMS_ERROR, "更新的图片不存在"));

//            已改为注解权限校验
//            // 权限校验（本人或管理员） ->  管理员在管理页面修改用户上传图片
//            ThrowUtils.throwIf(!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser),new BusinessException(ErrorCode.NO_AUTH_ERROR));
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId
            if (spaceId == null) {
                if (picture.getSpaceId() != null) {
                    spaceId = picture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                if (!ObjUtil.equal(spaceId, picture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }

        // 拼接上传路径(/公共or私有/用户id)
        String uploadPathPrefix;
        if (spaceId == null) {
            uploadPathPrefix = String.format("public/%s", userService.getLoginUser(request).getId());
        } else {
            uploadPathPrefix = String.format("private/%s", userService.getLoginUser(request).getId());
        }

        // 根据 inputSource 类型区分上传方式
        FileUploadTemplate pictureUploadTemplate = fileUploadByLocal;
        if (inputSource instanceof String) {
            pictureUploadTemplate = fileUploadURL;
        }

        // 上传到COS对象存储
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 解析返回的图片信息，封装对象
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(userService.getLoginUser(request).getId());
        picture.setSpaceId(spaceId);
        // 设置图片颜色
        picture.setPicColor(uploadPictureResult.getPicColor());
        picture.setSpaceId(pictureUploadDTO.getSpaceId());
        fillReviewParam(picture,loginUser);

        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新，需要补充 id 和编辑时间  
            picture.setId(pictureId);
            picture.setEditTime(new Date());

        }

        // 开启编程式事务
        Long finalSpaceId = spaceId;
        transactionTemplate.execute(status -> {
            boolean result = this.saveOrUpdate(picture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalSpaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, finalSpaceId)
                        .setSql("totalSize = totalSize + " + picture.getPicSize())
                        .setSql("totalCount = totalCount + 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return picture;
        });
        return picture;
    }

    /**
     * 填充图片审核参数
     * 根据用户角色设置图片的审核状态及相关信息
     *
     * @param picture 待审核的图片对象
     * @param loginUser 登录用户信息，用于判断用户角色并进行相应操作
     */
    @Override
    public void fillReviewParam(Picture picture, User loginUser){

        // 判断上传的空间类型
        if(picture.getSpaceId() != null){
            // 私有空间：系统自动审核通过
            picture.setReviewerId(0L);
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("私人空间图片，系统自动审核通过");
        }else{
            // 公共空间：判断是否为管理员
            if("admin".equals(loginUser.getUserRole())){
                // 自动审核通过
                picture.setReviewerId(loginUser.getId());
                picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
                picture.setReviewTime(new Date());
                picture.setReviewMessage("自动审核通过");
                return;
            }
            // 普通用户 ： 待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
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

//        // 已改为注解权限校验
//        // 判断是否为本人或管理员操作
//        User loginUser = userService.getLoginUser(request);
//        checkSpaceAuth(picture, loginUser);

        // 开启事务
        transactionTemplate.execute(status -> {
            // 删除图片
            boolean isOk = removeById(id);
            ThrowUtils.throwIf(!isOk, new BusinessException(ErrorCode.OPERATION_ERROR, "删除图片失败"));
            // 释放额度
            Long spaceId = picture.getSpaceId();
            if (spaceId != null) {
                boolean update = spaceService.lambdaUpdate()
                        .eq(Space::getId, spaceId)
                        .setSql("totalSize = totalSize - " + picture.getPicSize())
                        .setSql("totalCount = totalCount - 1")
                        .update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        clearPictureFile(picture);
    }

    @Async// 方法会异步执行
    @Override
    public void clearPictureFile(Picture picture){
        // 判断该图片是否被多条记录使用
        String pictureUrl = picture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // 清理压缩图
        cosManager.deleteObject(pictureUrl);
        // 清理缩略图
        String thumbnailUrl = picture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
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

    @Override
    public PictureVO getPictureVOById(Long id, HttpServletRequest request) {
        // 获取原始图片信息
        Picture picture = getPictureById(id);
        User loginUser = userService.getLoginUser(request);

        // 空间权限校验
        Long spaceId = picture.getSpaceId();
        // 私人和团队空间
        if (spaceId != null) {
            // 仅有权限的人可查看
            boolean isHasPermission = StpUtil.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!isHasPermission, new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限"));
        }else {
            // 公共空间：未过审或拒绝图片查询只能是图片创建人或管理员
            if (picture.getReviewStatus() != PictureReviewStatusEnum.PASS.getValue()) {
                ThrowUtils.throwIf(!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser), new BusinessException(ErrorCode.NO_AUTH_ERROR));
            }
        }

        // 图片信息脱敏
        PictureVO pictureVO = PictureVO.objToVo(picture);

        // 设置创建者信息和权限列表
        List<String> permissionList = spaceUserAuthManager.getPermissionList(spaceService.getById(spaceId), loginUser);
        pictureVO.setPermissionList(permissionList);
        pictureVO.setUser(BeanUtil.copyProperties(userService.getById(picture.getUserId()), UserVO.class));
        return pictureVO;
    }

    /**
     * 更新图片信息
     * 该方法接收一个包含图片更新信息的数据传输对象，并根据该对象中的信息更新数据库中的图片记录、
     *
     * @param pictrueUpdateDTO 包含图片更新信息的数据传输对象，包括需要更新的图片ID和其他相关信息
     * @param request
     */
    @Override
    public void pictureUpdate(PictrueUpdateDTO pictrueUpdateDTO, HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(pictrueUpdateDTO), ErrorCode.NOT_FOUND_ERROR);
        Long pictureId = pictrueUpdateDTO.getId();
        ThrowUtils.throwIf(pictureId == null || pictureId < 0, new BusinessException(ErrorCode.PARAMS_ERROR));

        // 查询图片资源是否存在
        Picture picture = query().eq("id", pictureId).one();
        ThrowUtils.throwIf(picture == null, new BusinessException(ErrorCode.PARAMS_ERROR, "图片资源不存在"));

        // 获取更新条件构造器
        UpdateWrapper<Picture> wrapper = getPictureUpdateWrapper(pictrueUpdateDTO);

        // 自动审核通过
        wrapper.set("reviewerId",loginUser.getId());
        wrapper.set("reviewTime",new Date());
        wrapper.set("reviewStatus",PictureReviewStatusEnum.PASS.getValue());
        wrapper.set("reviewMessage","自动审核通过");

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

//        // 已改为注解权限校验
//        // 权限校验
//        checkSpaceAuth(picture, userService.getLoginUser(request));

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
        Long spaceId = pictrueUpdateDTO.getSpaceId();
        // 使用JSONUtil转化成Json字符串，并去掉[],如果为null,tags赋值为null
        String tags = ObjectUtil.isNotEmpty(pictrueUpdateDTO.getTags()) ? JSONUtil.toJsonStr(pictrueUpdateDTO.getTags()).replace("[", "").replace("]", "") : null;

        UpdateWrapper<Picture> wrapper = new UpdateWrapper<>();
        wrapper.set(StrUtil.isNotBlank(url), "url", url);
        wrapper.set(StrUtil.isNotBlank(name), "name", name);
        wrapper.set(StrUtil.isNotBlank(introduction), "introduction", introduction);
        wrapper.set(spaceId != null, "spaceId", spaceId);
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

        // 获取分页参数
        int current = pictureQueryDTO.getCurrent();
        int pageSize = pictureQueryDTO.getPageSize();
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(current, pageSize) || current < 0 || pageSize < 0, new BusinessException(ErrorCode.PARAMS_ERROR));

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
    public IPage<PictureVO> picturePageVoQuery(PictureQueryDTO pictureQueryDTO,HttpServletRequest request) {

        // 传参代表查询私有空间的图片，无需设置审核条件
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

        // 判空
        if(ObjectUtil.isEmpty(userIds)){
            return pictureVOPage;
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
        Long reviewerId = pictureQueryDTO.getReviewerId();
        Integer reviewStatus = pictureQueryDTO.getReviewStatus();
        String reviewMessage = pictureQueryDTO.getReviewMessage();
        Long spaceId = pictureQueryDTO.getSpaceId();
        boolean nullSpaceId = pictureQueryDTO.isNullSpaceId();
        Date startEditTime = pictureQueryDTO.getStartEditTime();
        Date endEditTime = pictureQueryDTO.getEndEditTime();

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
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        queryWrapper.isNull(nullSpaceId, "spaceId");

        if (!ObjectUtil.isEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like(StrUtil.isNotBlank(tag), "tags", "\"" + tag + "\"");
            }
        }

        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField),StrUtil.isNotEmpty(sortOrder) && sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    @Override
    public void PictureReview(PictureReviewDTO pictureReviewDTO, HttpServletRequest request){

        // 参数校验
        ThrowUtils.throwIf(pictureReviewDTO == null ||
                ObjectUtil.hasEmpty(pictureReviewDTO.getId(),pictureReviewDTO.getReviewStatus()) ||
                pictureReviewDTO.getId() < 0,new BusinessException(ErrorCode.PARAMS_ERROR));

        // 判断图片是否存在
        Picture oldPicture = getById(pictureReviewDTO.getId());
        ThrowUtils.throwIf(oldPicture == null,new BusinessException(ErrorCode.PARAMS_ERROR,"图片资源不存在"));

        // 判断审核状态是否重复
        ThrowUtils.throwIf(pictureReviewDTO.getReviewStatus().equals(oldPicture.getReviewStatus()),new BusinessException(ErrorCode.PARAMS_ERROR,"审核状态重复"));

        // 更新审核状态
        Picture picture = BeanUtil.copyProperties(pictureReviewDTO, Picture.class);

        // 补充审核信息
        picture.setReviewerId(userService.getLoginUser(request).getId());
        picture.setReviewTime(new Date());

        boolean isOk = updateById(picture);
        ThrowUtils.throwIf(!isOk,new BusinessException(ErrorCode.OPERATION_ERROR,"更新审核状态失败"));
    }

    @Override
    public Integer PictureUploadByBatch(PictureUploadByBatchDTO pictureUploadByBatchDTO, HttpServletRequest request) {

        // 参数校验
        ThrowUtils.throwIf(pictureUploadByBatchDTO == null, new BusinessException(ErrorCode.PARAMS_ERROR));
        String searchText = pictureUploadByBatchDTO.getSearchText();
        Integer count = pictureUploadByBatchDTO.getCount();
        ThrowUtils.throwIf(count <= 0 || count > 30, new BusinessException(ErrorCode.PARAMS_ERROR, "抓取个数范围：1~30个"));

        // 拼接抓取路径
        String fetchPath = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document = null;
        try {
            // 抓取图片
            document = Jsoup.connect(fetchPath).get();
        } catch (IOException e) {
            log.error("获取页面失败:%s", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析HTML文档,获取图片路径
        Element dgControl = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(dgControl)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = dgControl.select("img.mimg");
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            try {
                // 上传图片
                PictureUploadDTO pictureUploadDTO = new PictureUploadDTO();
                Picture picture = this.uploadPicture(fileUrl, pictureUploadDTO, request);
                log.info("图片上传成功, id = {}", picture.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Override
    public IPage<PictureVO> picturePageVoQueryByCache(PictureQueryDTO pictureQueryDTO,HttpServletRequest request) {

        Long spaceId = pictureQueryDTO.getSpaceId();
        ThrowUtils.throwIf(userService.getLoginUser(request) == null,new BusinessException(ErrorCode.NOT_LOGIN_ERROR));

        if (spaceId != null) { //私人或团队空间
            ThrowUtils.throwIf(spaceService.getById(spaceId) == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR, "无权限");
        } else {//查看公共图库
            // 设置查询条件：未过审,重置其他审核条件
            pictureQueryDTO.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryDTO.setReviewerId(null);
            pictureQueryDTO.setReviewMessage(null);
            pictureQueryDTO.setNullSpaceId(true);
        }

        // 将查询条件转化为JSON字符串
        String jsonStr = JSONUtil.toJsonStr(pictureQueryDTO);

        // 构造key，对JSON字符串进行MD5压缩
        String queryCondition = DigestUtils.md5DigestAsHex(jsonStr.getBytes());

        // 构建Caffeine-Redis-MySQL-多级缓存

        // 查询本地缓存
//        String caffeineCacheJsonStr = LOCAL_CACHE.getIfPresent(PICTURE_QUERY_LIST_VO_KEY + queryCondition);
//        // 反序列化为分页对象
//        Page<PictureVO> caffeineCacheObj = JSONUtil.toBean(caffeineCacheJsonStr, Page.class);
//        if (caffeineCacheObj.getTotal() > 0) {
//            // 本地缓存命中，直接返回
//            return caffeineCacheObj;
//        }

        // 本地缓存未命中，查询redis
//        String redisCacheJsonStr = stringRedisTemplate.opsForValue().get(PICTURE_QUERY_LIST_VO_KEY + queryCondition);
//        // 反序列化为分页对象
//        Page<PictureVO> redisCacheObj = JSONUtil.toBean(redisCacheJsonStr, Page.class);
//        if (redisCacheObj.getTotal() > 0) {
//            // Redis缓存命中，直接返回
//            return redisCacheObj;
//        }

        // 缓存均未命中不存在，查找数据库
        Page<PictureVO> pictureVOIPage = (Page<PictureVO>) picturePageVoQuery(pictureQueryDTO,request);

        // 过期时间随机化，降低缓存雪崩（5~10分钟）
        long time = 3000 + RandomUtil.randomLong(0, 5000);
        String value = JSONUtil.toJsonStr(pictureVOIPage);
        // 数据库中存在，缓存到本地和Redis
        LOCAL_CACHE.put(PICTURE_QUERY_LIST_VO_KEY + queryCondition, value);
        stringRedisTemplate.opsForValue().set(PICTURE_QUERY_LIST_VO_KEY + queryCondition,value,time, TimeUnit.SECONDS);

        // 返回数据
        return pictureVOIPage;
    }

    @Override
    public void checkSpaceAuth(Picture picture, User loginUser) {
        // 先空间权限校验 -> 用户权限校验
        // 判断图片是否为私有空间
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 仅本人和管理员可操作
            ThrowUtils.throwIf(!Objects.equals(loginUser.getId(), picture.getUserId()) && !userService.isAdmin(loginUser), new BusinessException(ErrorCode.NO_AUTH_ERROR, "非本人或管理员，无权限操作"));
        } else {
            // 仅空间创建人可以操作
            ThrowUtils.throwIf(!loginUser.getId().equals(picture.getUserId()), new BusinessException(ErrorCode.NO_AUTH_ERROR, "非空间创建人,无权限操作"));
        }
    }

    @Override
    public List<ImageSearchResult> searchPictureByPicture(SearchPictureByPictureDTO searchPictureByPictureDTO) {
        ThrowUtils.throwIf(searchPictureByPictureDTO == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureDTO.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture oldPicture = getPictureById(pictureId);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        return ImageSearchApiFacade.searchImage(oldPicture.getUrl());
    }

    @Override
    public List<PictureVO> searchPictureByColor(SearchPictureByColorDTO searchPictureByColorDTO,User loginUser) {

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.isEmpty(searchPictureByColorDTO), ErrorCode.PARAMS_ERROR);
        Long spaceId = searchPictureByColorDTO.getSpaceId();
        String color = searchPictureByColorDTO.getColor();
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(color), ErrorCode.PARAMS_ERROR);
        Space space = spaceService.getSpaceById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

//        // 已改为注解权限校验
//        // 空间权限校验
//        ThrowUtils.throwIf(!loginUser.getId().equals(space.getUserId()),ErrorCode.NO_AUTH_ERROR);

        // 查询空间的所有颜色不为空的图片
        QueryWrapper<Picture> wrapper = new QueryWrapper<>();
        wrapper.eq("spaceId",spaceId)
                .isNotNull("picColor");
        List<Picture> pictureList = list(wrapper);

        if(pictureList.isEmpty()) {
            return Collections.emptyList();
        }

        // 将目标颜色转换为Color对象,否则每计算一张图都需要进行转换
        Color targetColor = Color.decode(color);

        // 颜色相似度计算，进行排序，返回前15张图片
        List<Picture> sortedPictures = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    // 提取图片主色调
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片放到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                // 取前 12 个
                .limit(12)
                .collect(Collectors.toList());

        // 图片信息脱敏
        return sortedPictures.stream()
                .map(PictureVO::objToVo)
                .collect(Collectors.toList());
    }

    /**
     * 图片批量编辑
     *
     * @param pictureEditByBatchDTO 图片批量编辑请求参数对象，包含编辑的图片范围以及编辑信息
     * @param loginUser 登录用户
     */
    @Override
    public void pictureEditByBatch(PictureEditByBatchDTO pictureEditByBatchDTO, User loginUser) {

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(pictureEditByBatchDTO), ErrorCode.PARAMS_ERROR);
        Long spaceId = pictureEditByBatchDTO.getSpaceId();
        List<Long> idList = pictureEditByBatchDTO.getIdList();
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(CollUtil.isEmpty(idList) || spaceId == null || spaceId < 0, ErrorCode.PARAMS_ERROR);

//        // 已改为注解权限校验
//        // 空间权限校验
//        Space space = spaceService.getById(spaceId);
//        ThrowUtils.throwIf(ObjectUtil.isEmpty(space), ErrorCode.NOT_FOUND_ERROR,"空间不存在");
//        if(!space.getUserId().equals(loginUser.getId())){
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"非空间创建人，无权限");
//        }

        // 根据idList和spaceId获取编辑图片列表,只查询出id和space_id,优化查询性能
        List<Picture> pictureList = list(new QueryWrapper<Picture>().
                select("id","spaceId").
                eq("spaceId",spaceId).
                in("id",idList));

        if(CollectionUtils.isEmpty(pictureList)){
            return ;
        }

        // 批量编辑图片列表信息
        String category = pictureEditByBatchDTO.getCategory();
        List<String> tag = pictureEditByBatchDTO.getTags();
        String nameRule = pictureEditByBatchDTO.getNameRule();
        // 按命名规则批量编辑图片名称
        fillPictureWithNameRule(pictureList, nameRule);
        // 转JSON字符串，并去除[]
        String tagJson = JSONUtil.toJsonStr(tag).replace("[", "").replace("]", "");

        // 遍历编辑图片列表，设置编辑信息
        for (Picture picture : pictureList) {
            if (ObjectUtil.isNotEmpty(category)) {
                picture.setCategory(category);
            }
            if (ObjectUtil.isNotEmpty(tag)) {
                picture.setTags(tagJson);
            }
        }

        // 批量更新到数据库
        boolean isOk = updateBatchById(pictureList);
        ThrowUtils.throwIf(!isOk, ErrorCode.OPERATION_ERROR,"批量编辑图片信息失败");
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    @Override
    public CreateoutPaintingTaskResponse createOutPaintingTask(CreatePictureOutPaintingTaskDTO createPictureOutPaintingTaskDTO, User loginUser) {

        // 参数校验
        ThrowUtils.throwIf(ObjectUtil.hasEmpty(createPictureOutPaintingTaskDTO), ErrorCode.PARAMS_ERROR);
        Long pictureId = createPictureOutPaintingTaskDTO.getId();
        CreateOutPaintingTaskRequest.Parameters parameters = createPictureOutPaintingTaskDTO.getParameters();
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(pictureId == null || pictureId < 0 || parameters == null, ErrorCode.PARAMS_ERROR);

        // 空间权限校验
        Picture picture = getPictureById(pictureId);
        ThrowUtils.throwIf(ObjectUtil.isEmpty(picture), ErrorCode.NOT_FOUND_ERROR,"图片资源不存在");

//        // 已改为注解权限校验
//        Space space = spaceService.getById(picture.getSpaceId());
//        ThrowUtils.throwIf(space != null && !Objects.equals(space.getUserId(), loginUser.getId()),new BusinessException(ErrorCode.NO_AUTH_ERROR,"非空间创建人，没有权限"));

        // 封装参数
        CreateOutPaintingTaskRequest createOutPaintingTaskRequest = new CreateOutPaintingTaskRequest();
        createOutPaintingTaskRequest.setInput(new CreateOutPaintingTaskRequest.Input());
        createOutPaintingTaskRequest.getInput().setImageUrl(picture.getUrl());
        createOutPaintingTaskRequest.setParameters(createPictureOutPaintingTaskDTO.getParameters());

        // 调用api创建扩图任务
        return aliYunAiApi.createOutPaintingTask(createOutPaintingTaskRequest);
    }
}




