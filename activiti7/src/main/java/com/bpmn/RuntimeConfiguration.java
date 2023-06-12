package com.bpmn;

import org.activiti.api.task.runtime.events.TaskAssignedEvent;
import org.activiti.api.task.runtime.events.TaskCompletedEvent;
import org.activiti.api.task.runtime.events.listener.TaskCandidateEventListener;
import org.activiti.api.task.runtime.events.listener.TaskRuntimeEventListener;
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

    private final Logger logger = LoggerFactory.getLogger(RuntimeConfiguration.class);

    @Bean
    public UserDetailsService userDetailsService() {
        ExtendedInMemoryUserDetailsManager extendedInMemoryUserDetailsManager = new ExtendedInMemoryUserDetailsManager();

        String[][] usersGroupsAndRoles = {
                {"admin", "password", "ROLE_ACTIVITI_ADMIN", "GROUP_developers"},
                {"system", "password", "ROLE_ACTIVITI_USER", "GROUP_developers"},
                {"zsan", "password", "ROLE_ACTIVITI_USER", "GROUP_developers"},
                {"lsi", "password", "ROLE_ACTIVITI_USER", "GROUP_developers"},
                {"wwu", "password", "ROLE_ACTIVITI_USER", "GROUP_developers"},
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

    @Bean
    public TaskRuntimeEventListener<TaskAssignedEvent> taskAssignedListener() {
        return taskAssigned -> logger.info(">>> Task Assigned: '"
                + taskAssigned.getEntity().getName() +
                "' We can send a notification to the assginee: " + taskAssigned.getEntity().getAssignee());
    }

    @Bean
    public TaskRuntimeEventListener<TaskCompletedEvent> taskCompletedListener() {
        return taskCompleted -> logger.info(">>> Task Completed: '"
                + taskCompleted.getEntity().getName() +
                "' We can send a notification to the owner: " + taskCompleted.getEntity().getOwner());
    }
}
