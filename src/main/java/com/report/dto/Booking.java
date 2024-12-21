package com.report.dto;

import lombok.Data;
import lombok.ToString;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@ToString
public class Booking {
    private Integer id;
    private String channelBookingId;
    private Client3 client;
    private Channel2 channel;
    private LocalDateTime bookingDate;
    private LocalDate arrivalDate;
    private LocalDate departureDate;
    private Integer numberOfRooms;
    private Integer numberOfNightsLos;
    private LocalDateTime cancellationDate;
    private String currency;

    private Integer numberOfRoomNights;
    private Boolean isCancelled;
    private Number gbv;

    public Boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(Boolean cancelled) {
        isCancelled = cancelled;
    }
}
