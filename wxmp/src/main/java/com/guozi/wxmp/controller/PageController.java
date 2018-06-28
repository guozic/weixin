package com.guozi.wxmp.controller;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpOAuth2AccessToken;
import me.chanjar.weixin.mp.bean.result.WxMpUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

/**
 * @author guozi
 * @date 2018-06-27 11:15
 */
@Controller
public class PageController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());



    @Autowired
    private WxMpService wxService;

    @RequestMapping(value = "/Payment")
    public String index() {
        return "Payment";
    }

    @RequestMapping(value = "/Collar")
    public String test() {
        return "Collar";
    }

    /**
     * 发红包
     * @return
     */
    @RequestMapping(value = "/balance")
    public String Payment() {
        return "balance";
    }

    /**
     * 用户点击连接 回调此路径 传过来code
     * @param code
     * @return
     */
    @GetMapping("/getAccessToken")
    public String getUserInfo(String code, HttpSession session){

        try {
            //获得access token
            WxMpOAuth2AccessToken wxMpOAuth2AccessToken = wxService.oauth2getAccessToken(code);

            this.logger.info("----- 用户回调后 获得access token -----------"+wxMpOAuth2AccessToken);

            //获得用户基本信息
            WxMpUser wxMpUser = wxService.oauth2getUserInfo(wxMpOAuth2AccessToken, null);

            this.logger.info("--  获得用户基本信息 ----------->>>"+wxMpUser.toString());

            //刷新access token
            //用户的唯一标示 openId 在 wxMpOAuth2AccessToken中
            wxMpOAuth2AccessToken = wxService.oauth2refreshAccessToken(wxMpOAuth2AccessToken.getRefreshToken());

            //验证access token是否有效
            boolean valid = wxService.oauth2validateAccessToken(wxMpOAuth2AccessToken);

            session.setAttribute("user",wxMpUser);

            return "balance";
        } catch (WxErrorException e) {
            e.printStackTrace();
        }
        return "accessTokenFail";
    }

}
