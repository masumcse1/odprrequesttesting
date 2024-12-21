package com.report.dto;

import lombok.Data;
import lombok.ToString;

import java.util.List;
@Data
@ToString
public class BookingContent {
    private String currentPage;
    private Integer totalPages;
    private List<Booking> data;
    private String previousPage;
    private String nextPage;
    private Integer totalRecords;
}