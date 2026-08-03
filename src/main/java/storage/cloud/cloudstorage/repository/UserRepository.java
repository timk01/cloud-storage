package storage.cloud.cloudstorage.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import storage.cloud.cloudstorage.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    //User save(User entity);

    Optional<User> findByUserName(String name);

    boolean existsByUserName(String name);
}
