package com.hao_xiao_zi.intellistorecopichelper.manager.websocket.disruptor;

import com.hao_xiao_zi.intellistorecopichelper.manager.websocket.model.PictureEditRequestMessage;
import com.hao_xiao_zi.intellistorecopichelper.model.entity.User;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 34255
 * Date: 2025-07-27
 * Time: 22:17
 */
@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
