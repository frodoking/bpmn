package com.bpmn;

import org.activiti.core.common.spring.identity.ExtendedInMemoryUserDetailsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Configuration
public class RuntimeConfiguration {

    private  Logger logger = LoggerFactory.getLogger(RuntimeConfiguration.class);

    @Bean
    public UserDetailsService userDetailsService() {
        ExtendedInMemoryUserDetailsManager extendedInMemoryUserDetailsManager = new ExtendedInMemoryUserDetailsManager();

        String[][] usersGroupsAndRoles = {
                {"bob", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam", "GROUP_developers"},
                {"john", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam"},
                {"hannah", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam"},
                {"other", "password", "ROLE_ACTIVITI_USER", "GROUP_otherTeam"},
                {"system", "password", "ROLE_ACTIVITI_USER"},
                {"admin", "password", "ROLE_ACTIVITI_ADMIN"},
                {"zsan", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam", "GROUP_developers"},
                {"lsi", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam", "GROUP_developers"},
                {"wwu", "password", "ROLE_ACTIVITI_USER", "GROUP_activitiTeam", "GROUP_developers"},
        };

        for (String[] user : usersGroupsAndRoles) {
            List<String> authoritiesStrings = asList(Arrays.copyOfRange(user, 2, user.length));
            logger.info("> Registering new user: " + user[0] + " with the following Authorities[" + authoritiesStrings + "]");
            extendedInMemoryUserDetailsManager.createUser(new User(user[0], passwordEncoder().encode(user[1]),
                    authoritiesStrings.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())));
        }

        return extendedInMemoryUserDetailsManager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
