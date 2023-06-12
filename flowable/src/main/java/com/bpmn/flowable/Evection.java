package com.bpmn.flowable;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author frodoking
 * @ClassName: Evection
 * @date 2023/5/24
 */
@Data
public class Evection implements Serializable {

    private Long id;

    private String evectionName;

    /**
     * 出差的天数
     */
    private double num;

    private Date beginDate;
    private Date endDate;
    private String destination;
    private String reason;
}
