package storage.cloud.cloudstorage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Getter
    @Column(name = "user_name", length = 100, nullable = false)
    private String userName;

    @Getter
    @Column(name = "password", length = 100, nullable = false)
    private String password;

    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
    }
}
