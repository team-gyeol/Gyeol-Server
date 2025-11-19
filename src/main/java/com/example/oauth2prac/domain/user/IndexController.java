    package com.example.oauth2prac.domain.user;

    import com.example.oauth2prac.global.security.SecurityUtils;
    import lombok.RequiredArgsConstructor;
    import org.springframework.stereotype.Controller;
    import org.springframework.ui.Model;
    import org.springframework.web.bind.annotation.GetMapping;


    @Controller
    @RequiredArgsConstructor
    public class IndexController {

        private final UserRepository userRepository;

        @GetMapping("/")
        public String index() {
            return "index";
        }

        @GetMapping("/success")
        public String success(Model model) {
            try {
                // JWT 인증을 통해 저장된 사용자 ID를 가져옴
                Long userId = SecurityUtils.currentUserIdOrThrow();
                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

                // 모델에 사용자 정보 추가
                model.addAttribute("name", user.getName());
                model.addAttribute("email", user.getEmail());

                return "success";

            } catch (IllegalStateException e) {
                // SecurityUtils.currentUserIdOrThrow() 에서 예외 발생 시 (토큰이 없는 경우)
                model.addAttribute("errorMessage", "로그인 정보가 없습니다.");
                return "error";
            }
        }

        @GetMapping("/error")
        public String error(Model model) {
            if (!model.containsAttribute("errorMessage")) {
                model.addAttribute("errorMessage", "알 수 없는 오류가 발생했습니다.");
            }
            return "error";
        }
    }
