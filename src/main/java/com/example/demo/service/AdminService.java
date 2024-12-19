package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // TODO: 4. find or save 예제 개선
    @Transactional
    public void reportUsers(List<Long> userIds) {
        // 필요한 모든 User 가져옴
        List<User> users = userRepository.findAllById(userIds);

        for (User user : users) {
            // updateStatusToBlocked 메서드 호출하여 상태 변경
            // Transactional로 되어있어서, 트랜잭션 종료될 때 변경사항 반영됨 -> 하나하나 save하지 않아도됨
            user.updateStatusToBlocked();
        }
    }
}
