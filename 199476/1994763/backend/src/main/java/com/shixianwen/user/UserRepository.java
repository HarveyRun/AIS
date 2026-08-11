package com.shixianwen.user;

import com.shixianwen.certification.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByPhoneAndAccountStatus(String phone, String accountStatus);
    Optional<User> findByUidAndAccountStatus(String uid, String accountStatus);

    @Query("""
        select distinct u from User u
        join Certification c on c.user = u
        where u.accountStatus = 'ACTIVE'
          and u.answererStatus = 'APPROVED'
          and c.status = 'APPROVED'
          and (:keyword is null or :keyword = '' or lower(c.title) like lower(concat('%', :keyword, '%'))
               or lower(c.description) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(u.nickname, '')) like lower(concat('%', :keyword, '%')))
        order by u.id desc
        """)
    List<User> searchAnswerers(@Param("keyword") String keyword);
}
