    package com.example.oauth2prac.service;

    import com.example.oauth2prac.config.oauth2.OAuthAttributes;
    import com.example.oauth2prac.entity.User;
    import com.example.oauth2prac.repository.UserRepository;
    import lombok.RequiredArgsConstructor;
    import org.springframework.security.core.authority.SimpleGrantedAuthority;
    import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
    import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
    import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
    import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
    import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
    import org.springframework.security.oauth2.core.user.OAuth2User;
    import org.springframework.stereotype.Service;

    import java.util.Collections;

    @Service
    @RequiredArgsConstructor
    public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
        private final UserRepository userRepository;

        @Override
        public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
            OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

            OAuthAttributes attributes = OAuthAttributes.of(registrationId,userNameAttributeName,oAuth2User.getAttributes());

            User user = saveOrUpdate(attributes);

            return new DefaultOAuth2User(Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                    attributes.getAttributes(),
                    attributes.getNameAttributeKey());
        }

        private User saveOrUpdate(OAuthAttributes attributes) {
            User user = userRepository.findByEmail(attributes.getEmail())
                    .map(entity -> entity.update(attributes.getName(),attributes.getPicture()))
                    .orElse(attributes.toEntity());

            return userRepository.save(user);
        }
    }
