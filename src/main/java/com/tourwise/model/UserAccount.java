package com.tourwise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount {
    private Long id;
    private String username;
    private String phone;
    private String passwordHash;
    private Integer status;
    private String role;
}
