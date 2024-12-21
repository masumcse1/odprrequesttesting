package com.report.dto;

import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@ToString
public class Client {
    private Integer id;
    private String name;
    private String url;
    private Integer capacity;
    private String street;
    private String zipCode;
    private String place;
    private LocalDateTime contractTerminationDate;
    private Integer distributionManagerIdCultSwitch;
    private LocalDateTime cultSwitchDMFromDate;


}
