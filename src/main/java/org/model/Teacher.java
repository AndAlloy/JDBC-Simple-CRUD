package org.model;

import lombok.*;
import org.dao.annotation.Id;

import java.sql.Date;

@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Teacher {
    @Id
    private int id;
    private String name;
    private Date dateofbirth;
    private int subject_id;
}
