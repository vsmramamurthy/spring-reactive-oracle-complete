
package com.example.demo;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("templates")
public class Template {
    @Id
    private Long id;
    private String templateId;
    private String query;

    // Getters and setters
}
