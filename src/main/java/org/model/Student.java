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
public class Student {
    @Id
    private int id;
    private String name;
    private String groupname;
    private Date dateofbirth;

}
