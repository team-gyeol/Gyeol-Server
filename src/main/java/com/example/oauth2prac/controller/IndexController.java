    package com.example.oauth2prac.controller;

    import com.example.oauth2prac.config.oauth2.OAuthAttributes;
    import lombok.RequiredArgsConstructor;
    import org.springframework.security.core.annotation.AuthenticationPrincipal;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
    import org.springframework.security.oauth2.core.user.OAuth2User;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.GetMapping;


    @Controller
    @RequiredArgsConstructor
    public class IndexController {


        @GetMapping("/")
        public String index() {
            return "index";
        }

        @GetMapping("/success")
        public String success(Model model, @AuthenticationPrincipal OAuth2User oAuth2User) {
            if (oAuth2User == null) {
                model.addAttribute("errorMessage", "로그인 정보가 없습니다.");
                return "error";
            }

            String registerationId = ((OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getAuthorizedClientRegistrationId();
            OAuthAttributes oAuthAttributes = OAuthAttributes.of(registerationId, "sub", oAuth2User.getAttributes());

            model.addAttribute("name", oAuthAttributes.getName());
            model.addAttribute("email", oAuthAttributes.getEmail());

            return "success";
        }

        @GetMapping("/error")
        public String error(Model model) {
            if (!model.containsAttribute("errorMessage")) {
                model.addAttribute("errorMessage", "알 수 없는 오류가 발생했습니다.");
            }
            return "error";
        }
    }
