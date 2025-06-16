package com.mytry.editortry.Try.service;


import com.mytry.editortry.Try.model.User;
import com.mytry.editortry.Try.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;



    public User getUserByUsername(String username){

        return userRepository.findWithProjectsByUsername(username)
                .orElseThrow(()-> new UsernameNotFoundException(username+" not found"));
    }

}
