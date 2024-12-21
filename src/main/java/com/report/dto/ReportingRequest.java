package com.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReportingRequest {

    @JsonProperty("Net bookings")
    private Double netBookings;

    @JsonProperty("Gross bookings")
    private Double grossBookings;

    @JsonProperty("Cancellations")
    private Double cancellations;

    @JsonProperty("Gross booking value (GBV)")
    private Double gbv;

    @JsonProperty("Cancellation value")
    private Double cancellationVolume;

    @JsonProperty("Net booking value (NBV)")
    private Double revenueFromDB;

    @JsonProperty("Sold roomnights (gross)")
    private Double soldRoomNightsGross;

    @JsonProperty("Sold roomnights (net)")
    private Double soldRoomNightsNet;

    @JsonProperty("Avg. booking value (net)")
    private Double revenuePerBooking;

    @JsonProperty("Avg. daily room rate (net)")
    private Double adr;

    @JsonProperty("Direct revenue")
    private Double directRevenue;

    @JsonProperty("Indirect revenue")
    private Double indirectRevenue;

    @JsonProperty("Cancellation rate")
    private Double cancellationRate;

    @JsonProperty("Bookings/day (gross)")
    private Double bookingsPerDay;

    @JsonProperty("Roomnights/booking")
    private Double roomnightsPerBooking;

    @JsonProperty("Producing channels")
    private Integer connectedChannels;

    @JsonProperty("Calendar days")
    private Integer daysOfCurrentMonth;

    @JsonProperty("Available rooms")
    private Integer hotelCapacity;

    @JsonProperty("Roomnights to sell")
    private Double roomnightsToSell;

    @JsonProperty("Online occupancy rate")
    private Double onlineOccupancyRate;

    @JsonProperty("Online RevPAR")
    private Double onlineRevPAR;


    @JsonIgnore
    private List<String> producingChannelsList = new ArrayList<>();
    @JsonIgnore
    private Double directGBV;
    @JsonIgnore
    private Double indirectGBV;
    @JsonIgnore
    private Double cancellationRoomNights;
    @JsonIgnore
    private Double roomnightsInCancellations;
    @JsonIgnore
    private Double directCancellationValue;
    @JsonIgnore
    private Double indirectCancellationValue;
    @JsonIgnore
    private Double grossSoldRoomnights;
}
