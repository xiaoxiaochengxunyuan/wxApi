package com.dengsheng.wxapi.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.dengsheng.wxapi.base.BaseApiService;
import com.dengsheng.wxapi.util.HttpClientUtils;
import com.dengsheng.wxapi.util.SignUtil;
import com.dengsheng.wxapi.util.WeiXinUtils;

@Controller
@RequestMapping("/wx_public_api")
public class WxPublicApiController extends BaseApiService {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
    
    @Autowired
	private WeiXinUtils weiXinUtils;
    
	private String errorPage = "errorPage";
	
	/**
     * 微信公众平台服务器配置验证
     * @param request
     * @param response
     */
    @GetMapping
    public void validate(HttpServletRequest request, HttpServletResponse response) {
        // 微信加密签名，signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
        String signature = request.getParameter("signature");
        // 时间戳
        String timestamp = request.getParameter("timestamp");
        // 随机数
        String nonce = request.getParameter("nonce");
        // 随机字符串
        String echostr = request.getParameter("echostr");
        PrintWriter out = null;
        try {
            out = response.getWriter();
            // 通过检验signature对请求进行校验，若校验成功则原样返回echostr，否则接入失败
            if (SignUtil.checkSignature(signature, timestamp, nonce)) {
                out.print(echostr);
            }
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            out.close();
            out = null;
        }
    }

	// 生成授权链接
	@RequestMapping("/authorizedUrl")
	public String authorizedUrl() {
		return "redirect:" + weiXinUtils.getAuthorizedUrl();
	}

	// 微信授权回调地址
	@RequestMapping("/callback")
	@ResponseBody
	public Object callback(String code, HttpServletRequest request) {
		// 1.使用Code 获取 access_token
		String accessTokenUrl = weiXinUtils.getAccessTokenUrl(code);
		JSONObject resultAccessToken = HttpClientUtils.httpGet(accessTokenUrl);
		boolean containsKey = resultAccessToken.containsKey("errcode");

		if (containsKey) {
			request.setAttribute("errorMsg", "系统错误!");
			return errorPage;
		}
		// 2.使用access_token获取用户信息
		String accessToken = resultAccessToken.getString("access_token");
		String openid = resultAccessToken.getString("openid");
		// 3.拉取用户信息(需scope为 snsapi_userinfo)
		String userInfoUrl = weiXinUtils.getUserInfo(accessToken, openid);
		JSONObject userInfoResult = HttpClientUtils.httpGet(userInfoUrl);
		System.out.println("userInfoResult:" + userInfoResult);
		request.setAttribute("nickname", userInfoResult.getString("nickname"));
		request.setAttribute("city", userInfoResult.getString("city"));
		request.setAttribute("headimgurl", userInfoResult.getString("headimgurl"));
		return userInfoResult;
	}
}
