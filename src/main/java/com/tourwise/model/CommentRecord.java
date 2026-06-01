package com.tourwise.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRecord {
    private Long id;
    private Long logId;
    private Long userId;
    private Long parentId;
    private String content;
}
