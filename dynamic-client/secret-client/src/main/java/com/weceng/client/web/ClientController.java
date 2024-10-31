package com.weceng.client.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * <p>
 * 客户端controller
 * </p>
 *
 * @author WECENG
 * @since 2024/10/22 09:48
 */
@Controller
public class ClientController {

    @GetMapping("/")
    public String index(){
        return "index";
    }

    @GetMapping("/user")
    @ResponseBody
    public OAuth2User user(@AuthenticationPrincipal OAuth2User principal) {
        return principal;
    }

}
