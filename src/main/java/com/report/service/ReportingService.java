package com.report.service;


import com.report.dto.Booking;
import com.report.dto.BookingContent;
import com.report.dto.Client;
import com.report.dto.ReportingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static com.report.common.DateUtil.getLocalDateToString;

@Service
public class ReportingService {

    @Autowired
    private RestTemplate restTemplate;
    private final String apiKey = "tsBfMrJgrY4BPWnUYUL2NZK2OzmIAr";
    Logger logger = LoggerFactory.getLogger(ReportingService.class);


    public void saveReport(Integer objectId, String month, String pdfLink) {
        String url = "https://api.cultdata.com/api/report";
        Map<String, Object> payload = Map.of(
                "month", month,
                "pdf", pdfLink,
                "client", objectId
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        restTemplate.postForEntity(url, request, String.class);

    }

    public double getIndirectRevenue(ReportingRequest request) {
        double indirectRevenue = 0;
        if (request.getRevenueFromDB() != 0) {
            indirectRevenue = ((request.getIndirectGBV() - request.getIndirectCancellationValue()) / request.getRevenueFromDB()) * 100;
        }
        return indirectRevenue;
    }

    public double getDirectRevenue(ReportingRequest request) {
        double directRevenue = 0;
        if (request.getRevenueFromDB() != 0) {
            directRevenue = ((request.getDirectGBV() - request.getDirectCancellationValue()) / request.getRevenueFromDB()) * 100;
        }
        return directRevenue;
    }


    public List<Booking> getBookings(Integer objectId, String bookingStartDate, String bookingEndDate) {
        String BOOKING_URL = "https://api.cultdata.com/api/bookings";

        // Construct the URL with query parameters
        String url = UriComponentsBuilder.fromHttpUrl(BOOKING_URL)
                .queryParam("limit", 50000)
                .queryParam("page", 1)
                .queryParam("client_id", objectId)
                .queryParam("start_booking_date", bookingStartDate)
                .queryParam("end_booking_date", bookingEndDate)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", apiKey); // Replace with the actual API key
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<BookingContent> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<BookingContent>() {
                    }
            );

            return response.getBody().getData();
        } catch (Exception e) {
            // Handle all exceptions
            logger.error("Error while fetching bookings: {}", e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    public String getBusinessUnitName(String buId) {
        String url = "https://api.cultdata.com/api/business-unit/" + buId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);

        Map<String, Object> body = response.getBody();
        return body != null ? body.getOrDefault("name", "").toString() : "";
    }

    // return  -- businessUnitId
    public Integer getBusinessUnitId(Integer dmId) {
        String url = "https://api.cultdata.com/api/distribution-manager/" + dmId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        Map<String, Object> body = response.getBody();

        // Return the "businessUnitId" as an Integer, defaulting to 0 if missing or invalid
        if (body != null && body.containsKey("businessUnitId")) {
            Object businessUnitId = body.get("businessUnitId");
            if (businessUnitId instanceof Number) {
                return ((Number) businessUnitId).intValue(); // Convert to Integer
            }
        }

        return 0;
    }

    public List<String> getClientsViaAPI() {
        String url = "https://api.cultdata.com/api/clients?limit=10000";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> clients = (List<Map<String, Object>>) body.get("data");

        return clients.stream()
                .filter(client -> client.get("capacity") != null && (Integer) client.get("capacity") > 0)
                .map(client -> client.get("id").toString())
                .collect(Collectors.toList());
    }

    //getObjectName --name
    //getAvailableRooms --capacity
    //getObjectDMId -distributionManagerIdCultSwitch
    public Client getClientsById(Integer objectId) {
        String url = "https://api.cultdata.com/api/client/" + objectId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-KEY", apiKey);
        HttpEntity<Void> request = new HttpEntity<>(headers);
       // ResponseEntity<Client> response = restTemplate.getForEntity(url, Client.class);
        ResponseEntity<Client> response = restTemplate.exchange(url, HttpMethod.GET, request, Client.class); // Use exchange to include the header
        return response.getBody();
    }


    private void initializeRequest(ReportingRequest request) {
        request.setGrossBookings(0.0);
        request.setCancellations(0.0);
        request.setGrossSoldRoomnights(0.0);
        request.setGbv(0.0);
        request.setIndirectGBV(0.0);
        request.setDirectGBV(0.0);
        request.setRevenueFromDB(0.0);
        request.setCancellationVolume(0.0);
        request.setRoomnightsInCancellations(0.0);
        request.setDirectCancellationValue(0.0);
        request.setIndirectCancellationValue(0.0);
        request.setCancellationRoomNights(0.0);
    }

    private double round(double value, int places) {
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }



    public String requestCreator(Integer objectID, Long templateID, String dateForExecution) {
        System.out.println("Object ID: " + objectID);

        ReportingRequest lastMonthReq = new ReportingRequest();
        ReportingRequest year1 = new ReportingRequest();
        ReportingRequest year2 = new ReportingRequest();
        ReportingRequest year3 = new ReportingRequest();
        ReportingRequest year4 = new ReportingRequest();

        initializeRequest(lastMonthReq);
        initializeRequest(year1);
        initializeRequest(year2);
        initializeRequest(year3);
        initializeRequest(year4);

        Client client = getClientsById(objectID);
        String objectName = client.getName();
        Integer dmId = client.getDistributionManagerIdCultSwitch();
        Integer hotelCapacity = (client != null && client.getCapacity() != null) ? client.getCapacity() : 0; //getAvailableRooms(objectID);
        //Integer buId = getBusinessUnitId(dmId);
        //  String buName = getBusinessUnitName(String.valueOf(buId));
        String buName = null;
        LocalDate dt = dateForExecution != null && !dateForExecution.isEmpty() ? LocalDate.parse(dateForExecution) : LocalDate.now().minusDays(1);
        LocalDate monthStartDate = dt.withDayOfMonth(1);
        LocalDate monthEndDate = dt.withDayOfMonth(dt.lengthOfMonth());

        int month = dt.getMonthValue();
        int year = dt.getYear();

        int year1Text = year - 2;
        int year2Text = year - 1;

        lastMonthReq.setDaysOfCurrentMonth(YearMonth.of(year, month).lengthOfMonth());
        year1.setDaysOfCurrentMonth(calDaysInYear(year1Text, 12));
        year2.setDaysOfCurrentMonth(calDaysInYear(year2Text, 12));
        year3.setDaysOfCurrentMonth(calDaysInYear(year2Text, month));
        year4.setDaysOfCurrentMonth(calDaysInYear(year, month));

        LocalDate year1StartDate = LocalDate.of(year1Text, Month.JANUARY, 1);
        LocalDate year1EndDate = LocalDate.of(year1Text, Month.DECEMBER, 31);

        LocalDate year2StartDate = LocalDate.of(year2Text, Month.JANUARY, 1);
        LocalDate year2EndDate = LocalDate.of(year2Text, Month.DECEMBER, 31);

        LocalDate year3StartDate = LocalDate.of(year2Text, Month.JANUARY, 1);
        LocalDate year3EndDate = LocalDate.of(year2Text, month, dt.lengthOfMonth());

        LocalDate year4StartDate = LocalDate.of(year, Month.JANUARY, 1);
        LocalDate year4EndDate = LocalDate.of(year, month, dt.lengthOfMonth());

        System.out.println("Generated Dates:");
        System.out.println("Year 1 Start: " + year1StartDate + ", End: " + year1EndDate);
        System.out.println("Year 2 Start: " + year2StartDate + ", End: " + year2EndDate);
        System.out.println("Year 3 Start: " + year3StartDate + ", End: " + year3EndDate);
        System.out.println("Year 4 Start: " + year4StartDate + ", End: " + year4EndDate);

        List<Booking> bookingsArray      = getBookings(objectID, getLocalDateToString(monthStartDate, "yyyy-MM-dd"), getLocalDateToString(monthEndDate, "yyyy-MM-dd"));
        List<Booking> year1BookingsArray = getBookings(objectID, getLocalDateToString(year1StartDate, "yyyy-MM-dd"), getLocalDateToString(year1EndDate, "yyyy-MM-dd"));
        List<Booking> year2BookingsArray = getBookings(objectID, getLocalDateToString(year2StartDate, "yyyy-MM-dd"), getLocalDateToString(year2EndDate, "yyyy-MM-dd"));
        List<Booking> year3BookingsArray = getBookings(objectID, getLocalDateToString(year3StartDate, "yyyy-MM-dd"), getLocalDateToString(year3EndDate, "yyyy-MM-dd"));
        List<Booking> year4BookingsArray = getBookings(objectID, getLocalDateToString(year4StartDate, "yyyy-MM-dd"), getLocalDateToString(year4EndDate, "yyyy-MM-dd"));




      long falseitems=  year1BookingsArray.stream()
                .filter(booking -> !booking.isCancelled()) // Only include non-cancelled bookings
                .count();

        long trueitems=  year1BookingsArray.stream()
                .filter(booking -> booking.isCancelled()) // Only include non-cancelled bookings
                .count();




        System.out.println("Bookings Array Size: " + bookingsArray.size());
        System.out.println("Year 1 Bookings Array Size: " + year1BookingsArray.size());
        System.out.println("Year 2 Bookings Array Size: " + year2BookingsArray.size());
        System.out.println("Year 3 Bookings Array Size: " + year3BookingsArray.size());
        System.out.println("Year 4 Bookings Array Size: " + year4BookingsArray.size());


        for (Booking booking : bookingsArray) {
            if (booking.isCancelled()) {
                lastMonthReq.setCancellationRoomNights(lastMonthReq.getCancellationRoomNights() + booking.getNumberOfRoomNights());
            }
            if (!booking.isCancelled()) {
                lastMonthReq.setGrossSoldRoomnights(lastMonthReq.getGrossSoldRoomnights() + booking.getNumberOfRoomNights());
            }
            if (booking.isCancelled()) {
                lastMonthReq.setRoomnightsInCancellations(lastMonthReq.getRoomnightsInCancellations() + booking.getNumberOfRoomNights());
            }
            if (!booking.isCancelled()) {
                lastMonthReq.setGrossBookings(lastMonthReq.getGrossBookings() + 1);
            }
            if (!booking.isCancelled()) {
                lastMonthReq.setGbv(lastMonthReq.getGbv() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled()) {
                lastMonthReq.setCancellationVolume(lastMonthReq.getCancellationVolume() + booking.getGbv().doubleValue());
            }
            if (!booking.isCancelled()) {
                lastMonthReq.getProducingChannelsList().add(booking.getChannel().getId().toString());
            }
            if (booking.isCancelled()) {
                lastMonthReq.setCancellations(lastMonthReq.getCancellations() + 1);
            }
            if (!booking.isCancelled()) {
                if ("Direct".equals(booking.getChannel().getChannelType())) {
                    lastMonthReq.setDirectGBV(lastMonthReq.getDirectGBV() + booking.getGbv().doubleValue());
                } else {
                    lastMonthReq.setIndirectGBV(lastMonthReq.getIndirectGBV() + booking.getGbv().doubleValue());
                }
            }
            if (booking.isCancelled()) {
                if ("Direct".equals(booking.getChannel().getChannelType())) {
                    lastMonthReq.setDirectCancellationValue(lastMonthReq.getDirectCancellationValue() + booking.getGbv().doubleValue());
                } else {
                    lastMonthReq.setIndirectCancellationValue(lastMonthReq.getIndirectCancellationValue() + booking.getGbv().doubleValue());
                }
            }
        }

        for (Booking booking : year1BookingsArray) {
            if (booking.isCancelled()) {
                year1.setCancellationRoomNights(year1.getCancellationRoomNights() + booking.getNumberOfRoomNights());
            }
            if (!booking.isCancelled()) {
                year1.setGrossSoldRoomnights(year1.getGrossSoldRoomnights() + booking.getNumberOfRoomNights());
            }
            if (booking.isCancelled()) {
                year1.setRoomnightsInCancellations(year1.getRoomnightsInCancellations() + booking.getNumberOfRoomNights());
            }
            if (!booking.isCancelled()) {
                year1.setGrossBookings(year1.getGrossBookings() + 1);
            }
            if (!booking.isCancelled()) {
                year1.setGbv(year1.getGbv() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled()) {
                year1.setCancellationVolume(year1.getCancellationVolume() + booking.getGbv().doubleValue());
            }
            if (!booking.isCancelled()) {
                year1.getProducingChannelsList().add(booking.getChannel().getId().toString());
            }
            if (booking.isCancelled()) {
                year1.setCancellations(year1.getCancellations() + 1);
            }
            if (!booking.isCancelled() && "Direct".equals(booking.getChannel().getChannelType())) {
                year1.setDirectGBV(year1.getDirectGBV() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled() && "Direct".equals(booking.getChannel().getChannelType())) {
                year1.setDirectCancellationValue(year1.getDirectCancellationValue() + booking.getGbv().doubleValue());
            }
            if (!booking.isCancelled() && !"Direct".equals(booking.getChannel().getChannelType())) {
                year1.setIndirectGBV(year1.getIndirectGBV() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled() && !"Direct".equals(booking.getChannel().getChannelType())) {
                year1.setIndirectCancellationValue(year1.getIndirectCancellationValue() + booking.getGbv().doubleValue());
            }
        }

        for (Booking booking : year2BookingsArray) {
            if (booking.isCancelled()) {
                year2.setCancellationRoomNights(year2.getCancellationRoomNights() + booking.getNumberOfRoomNights());
            }
            if (!booking.isCancelled()) {
                year2.setGrossSoldRoomnights(year2.getGrossSoldRoomnights() + booking.getNumberOfRoomNights());
                year2.setGrossBookings(year2.getGrossBookings() + 1);
                year2.setGbv(year2.getGbv() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled()) {
                year2.setRoomnightsInCancellations(year2.getRoomnightsInCancellations() + booking.getNumberOfRoomNights());
                year2.setCancellationVolume(year2.getCancellationVolume() + booking.getGbv().doubleValue());
                year2.setCancellations(year2.getCancellations() + 1);
            }
            if (!booking.isCancelled()) {
                year2.getProducingChannelsList().add(booking.getChannel().getId().toString());
            }
            if (!booking.isCancelled() && "Direct".equals(booking.getChannel().getChannelType())) {
                year2.setDirectGBV(year2.getDirectGBV() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled() && "Direct".equals(booking.getChannel().getChannelType())) {
                year2.setDirectCancellationValue(year2.getDirectCancellationValue() + booking.getGbv().doubleValue());
            }
            if (!booking.isCancelled() && !"Direct".equals(booking.getChannel().getChannelType())) {
                year2.setIndirectGBV(year2.getIndirectGBV() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled() && !"Direct".equals(booking.getChannel().getChannelType())) {
                year2.setIndirectCancellationValue(year2.getIndirectCancellationValue() + booking.getGbv().doubleValue());
            }
        }



        for (Booking booking : year3BookingsArray) {
            if (booking.isCancelled()) {
                year3.setCancellationRoomNights(year3.getCancellationRoomNights() + booking.getNumberOfRoomNights());
            }
            if (!booking.isCancelled()) {
                year3.setGrossSoldRoomnights(year3.getGrossSoldRoomnights() + booking.getNumberOfRoomNights());
                year3.setGrossBookings(year3.getGrossBookings() + 1);
                year3.setGbv(year3.getGbv() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled()) {
                year3.setRoomnightsInCancellations(year3.getRoomnightsInCancellations() + booking.getNumberOfRoomNights());
                year3.setCancellationVolume(year3.getCancellationVolume() + booking.getGbv().doubleValue());
                year3.setCancellations(year3.getCancellations() + 1);
            }
            if (!booking.isCancelled()) {
                year3.getProducingChannelsList().add(booking.getChannel().getId().toString());
            }
            if (!booking.isCancelled() && "Direct".equals(booking.getChannel().getChannelType())) {
                year3.setDirectGBV(year3.getDirectGBV() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled() && "Direct".equals(booking.getChannel().getChannelType())) {
                year3.setDirectCancellationValue(year3.getDirectCancellationValue() + booking.getGbv().doubleValue());
            }
            if (!booking.isCancelled() && !"Direct".equals(booking.getChannel().getChannelType())) {
                year3.setIndirectGBV(year3.getIndirectGBV() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled() && !"Direct".equals(booking.getChannel().getChannelType())) {
                year3.setIndirectCancellationValue(year3.getIndirectCancellationValue() + booking.getGbv().doubleValue());
            }
        }

        for (Booking booking : year4BookingsArray) {
            if (booking.isCancelled()) {
                year4.setCancellationRoomNights(year4.getCancellationRoomNights() + booking.getNumberOfRoomNights());
            }
            if (!booking.isCancelled()) {
                year4.setGrossSoldRoomnights(year4.getGrossSoldRoomnights() + booking.getNumberOfRoomNights());
                year4.setGrossBookings(year4.getGrossBookings() + 1);
                year4.setGbv(year4.getGbv() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled()) {
                year4.setRoomnightsInCancellations(year4.getRoomnightsInCancellations() + booking.getNumberOfRoomNights());
                year4.setCancellationVolume(year4.getCancellationVolume() + booking.getGbv().doubleValue());
                year4.setCancellations(year4.getCancellations() + 1);
            }
            if (!booking.isCancelled()) {
                year4.getProducingChannelsList().add(booking.getChannel().getId().toString());
            }
            if (!booking.isCancelled() && "Direct".equals(booking.getChannel().getChannelType())) {
                year4.setDirectGBV(year4.getDirectGBV() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled() && "Direct".equals(booking.getChannel().getChannelType())) {
                year4.setDirectCancellationValue(year4.getDirectCancellationValue() + booking.getGbv().doubleValue());
            }
            if (!booking.isCancelled() && !"Direct".equals(booking.getChannel().getChannelType())) {
                year4.setIndirectGBV(year4.getIndirectGBV() + booking.getGbv().doubleValue());
            }
            if (booking.isCancelled() && !"Direct".equals(booking.getChannel().getChannelType())) {
                year4.setIndirectCancellationValue(year4.getIndirectCancellationValue() + booking.getGbv().doubleValue());
            }
        }


// Calculating netBookings
        lastMonthReq.setNetBookings(lastMonthReq.getGrossBookings() - lastMonthReq.getCancellations());
        year1.setNetBookings(year1.getGrossBookings() - year1.getCancellations());
        year2.setNetBookings(year2.getGrossBookings() - year2.getCancellations());
        year3.setNetBookings(year3.getGrossBookings() - year3.getCancellations());
        year4.setNetBookings(year4.getGrossBookings() - year4.getCancellations());

// Calculating revenueFromDB
        lastMonthReq.setRevenueFromDB(lastMonthReq.getGbv() - lastMonthReq.getCancellationVolume());
        year1.setRevenueFromDB(year1.getGbv() - year1.getCancellationVolume());
        year2.setRevenueFromDB(year2.getGbv() - year2.getCancellationVolume());
        year3.setRevenueFromDB(year3.getGbv() - year3.getCancellationVolume());
        year4.setRevenueFromDB(year4.getGbv() - year4.getCancellationVolume());

// Setting soldRoomNightsGross
        lastMonthReq.setSoldRoomNightsGross(lastMonthReq.getGrossSoldRoomnights());
        year1.setSoldRoomNightsGross(year1.getGrossSoldRoomnights());
        year2.setSoldRoomNightsGross(year2.getGrossSoldRoomnights());
        year3.setSoldRoomNightsGross(year3.getGrossSoldRoomnights());
        year4.setSoldRoomNightsGross(year4.getGrossSoldRoomnights());

// Calculating soldRoomNightsNet
        lastMonthReq.setSoldRoomNightsNet(lastMonthReq.getGrossSoldRoomnights() - lastMonthReq.getRoomnightsInCancellations());
        year1.setSoldRoomNightsNet(year1.getGrossSoldRoomnights() - year1.getRoomnightsInCancellations());
        year2.setSoldRoomNightsNet(year2.getGrossSoldRoomnights() - year2.getRoomnightsInCancellations());
        year3.setSoldRoomNightsNet(year3.getGrossSoldRoomnights() - year3.getRoomnightsInCancellations());
        year4.setSoldRoomNightsNet(year4.getGrossSoldRoomnights() - year4.getRoomnightsInCancellations());

// Calculating connectedChannels (assuming producingChannelsList is a List)
        lastMonthReq.setConnectedChannels((int) lastMonthReq.getProducingChannelsList().stream().distinct().count());
        year1.setConnectedChannels((int) year1.getProducingChannelsList().stream().distinct().count());
        year2.setConnectedChannels((int) year2.getProducingChannelsList().stream().distinct().count());
        year3.setConnectedChannels((int) year3.getProducingChannelsList().stream().distinct().count());
        year4.setConnectedChannels((int) year4.getProducingChannelsList().stream().distinct().count());

// Setting directRevenue
        lastMonthReq.setDirectRevenue(getDirectRevenue(lastMonthReq));
        year1.setDirectRevenue(getDirectRevenue(year1));
        year2.setDirectRevenue(getDirectRevenue(year2));
        year3.setDirectRevenue(getDirectRevenue(year3));
        year4.setDirectRevenue(getDirectRevenue(year4));

// Setting indirectRevenue
        lastMonthReq.setIndirectRevenue(getIndirectRevenue(lastMonthReq));
        year1.setIndirectRevenue(getIndirectRevenue(year1));
        year2.setIndirectRevenue(getIndirectRevenue(year2));
        year3.setIndirectRevenue(getIndirectRevenue(year3));
        year4.setIndirectRevenue(getIndirectRevenue(year4));

        // Process lastMonthReq
        double lastMonthGBV = round(lastMonthReq.getGbv() / 1000.0, 1);
        if (lastMonthGBV > 0) {
            lastMonthReq.setCancellationRate((round(lastMonthReq.getCancellationVolume() / 1000.0, 1) / lastMonthGBV) * 100);
        } else {
            lastMonthReq.setCancellationRate(0.0);
        }

        // Process year1
        double year1GBV = round(year1.getGbv() / 1000.0, 1);
        if (year1GBV > 0) {
            year1.setCancellationRate((round(year1.getCancellationVolume() / 1000.0, 1) / year1GBV) * 100);
        } else {
            year1.setCancellationRate(0.0);
        }


        // Process year2
        double year2GBV = round(year2.getGbv() / 1000.0, 1);
        if (year2GBV > 0) {
            year2.setCancellationRate((round(year2.getCancellationVolume() / 1000.0, 1) / year2GBV) * 100);
        } else {
            year2.setCancellationRate(0.0);
        }


        // Process year3
        double year3GBV = round(year3.getGbv() / 1000.0, 1);
        if (year3GBV > 0) {
            year3.setCancellationRate((round(year3.getCancellationVolume() / 1000.0, 1) / year3GBV) * 100);
        } else {
            year3.setCancellationRate(0.0);
        }


        // Process year4
        double year4GBV = round(year4.getGbv() / 1000.0, 1);
        if (year4GBV > 0) {
            year4.setCancellationRate((round(year4.getCancellationVolume() / 1000.0, 1) / year4GBV) * 100);
        } else {
            year4.setCancellationRate(0.0);
        }

        // Process lastMonthReq
        lastMonthReq.setRevenuePerBooking((lastMonthReq.getNetBookings() != null && lastMonthReq.getNetBookings() != 0) ?
                lastMonthReq.getRevenueFromDB() / lastMonthReq.getNetBookings() : 0.0);
        lastMonthReq.setBookingsPerDay((lastMonthReq.getGrossBookings() != null && lastMonthReq.getDaysOfCurrentMonth() > 0) ?
                lastMonthReq.getGrossBookings() / lastMonthReq.getDaysOfCurrentMonth() : 0.0);


        // Process year1
        year1.setRevenuePerBooking((year1.getNetBookings() != null && year1.getNetBookings() > 0) ?
                year1.getRevenueFromDB() / year1.getNetBookings() : 0.0);

        // Process year2
        year2.setRevenuePerBooking((year2.getNetBookings() != null && year2.getNetBookings() > 0) ?
                year2.getRevenueFromDB() / year2.getNetBookings() : 0.0);


        // Process year3
        year3.setRevenuePerBooking((year3.getNetBookings() != null && year3.getNetBookings() > 0) ?
                year3.getRevenueFromDB() / year3.getNetBookings() : 0.0);


        // Process year4
        year4.setRevenuePerBooking((year4.getNetBookings() != null && year4.getNetBookings() > 0) ?
                year4.getRevenueFromDB() / year4.getNetBookings() : 0.0);


        // Process year1
        lastMonthReq.setBookingsPerDay(lastMonthReq.getGrossBookings() != null && lastMonthReq.getDaysOfCurrentMonth() != null
                && lastMonthReq.getDaysOfCurrentMonth() > 0
                ? lastMonthReq.getGrossBookings() / lastMonthReq.getDaysOfCurrentMonth()
                : 0.0);

        // Process year1
        year1.setBookingsPerDay((year1.getGrossBookings() != null && year1.getDaysOfCurrentMonth() > 0) ?
                year1.getGrossBookings() / year1.getDaysOfCurrentMonth() : 0.0);

        // process year2
        year2.setBookingsPerDay((year2.getGrossBookings() != null && year2.getDaysOfCurrentMonth() > 0) ?
                year2.getGrossBookings() / year2.getDaysOfCurrentMonth() : 0.0);

        // process year3
        year3.setBookingsPerDay((year3.getGrossBookings() != null && year3.getDaysOfCurrentMonth() > 0) ?
                year3.getGrossBookings() / year3.getDaysOfCurrentMonth() : 0.0);

        //process year4
        year4.setBookingsPerDay((year4.getGrossBookings() != null && year4.getDaysOfCurrentMonth() > 0) ?
                year4.getGrossBookings() / year4.getDaysOfCurrentMonth() : 0.0);


        // Last month
        if (lastMonthReq.getGrossBookings() != null && lastMonthReq.getGrossBookings() != 0) {
            lastMonthReq.setRoomnightsPerBooking(lastMonthReq.getSoldRoomNightsGross() / lastMonthReq.getGrossBookings());
        } else {
            lastMonthReq.setRoomnightsPerBooking(0.0);
        }

        // Year 1
        if (year1.getGrossBookings() != null && year1.getGrossBookings() != 0) {
            year1.setRoomnightsPerBooking(year1.getSoldRoomNightsGross() / year1.getGrossBookings());
        } else {
            year1.setRoomnightsPerBooking(0.0);
        }

        // Year 2
        if (year2.getGrossBookings() != null && year2.getGrossBookings() != 0) {
            year2.setRoomnightsPerBooking(year2.getSoldRoomNightsGross() / year2.getGrossBookings());
        } else {
            year2.setRoomnightsPerBooking(0.0);
        }

        // Year 3
        if (year3.getGrossBookings() != null && year3.getGrossBookings() != 0) {
            year3.setRoomnightsPerBooking(year3.getSoldRoomNightsGross() / year3.getGrossBookings());
        } else {
            year3.setRoomnightsPerBooking(0.0);
        }

        // Year 4
        if (year4.getGrossBookings() != null && year4.getGrossBookings() != 0) {
            year4.setRoomnightsPerBooking(year4.getSoldRoomNightsGross() / year4.getGrossBookings());
        } else {
            year4.setRoomnightsPerBooking(0.0);
        }

        lastMonthReq.setRoomnightsToSell(hotelCapacity * lastMonthReq.getDaysOfCurrentMonth().doubleValue());
        year1.setRoomnightsToSell(hotelCapacity * year1.getDaysOfCurrentMonth().doubleValue());
        year2.setRoomnightsToSell(hotelCapacity * year2.getDaysOfCurrentMonth().doubleValue());
        year3.setRoomnightsToSell(hotelCapacity * year3.getDaysOfCurrentMonth().doubleValue());
        year4.setRoomnightsToSell(hotelCapacity * year4.getDaysOfCurrentMonth().doubleValue());

        // Calculate onlineOccupancyRate for lastMonthReq
        if (lastMonthReq.getRoomnightsToSell() > 0) {
            lastMonthReq.setOnlineOccupancyRate((lastMonthReq.getSoldRoomNightsNet() / lastMonthReq.getRoomnightsToSell()) * 100);
        } else {
            lastMonthReq.setOnlineOccupancyRate(0.0);
        }

        // Calculate onlineOccupancyRate for year1
        if (year1.getRoomnightsToSell() > 0) {
            year1.setOnlineOccupancyRate((year1.getSoldRoomNightsNet() / year1.getRoomnightsToSell()) * 100);
        } else {
            year1.setOnlineOccupancyRate(0.0);
        }

        // Calculate onlineOccupancyRate for year2
        if (year2.getRoomnightsToSell() > 0) {
            year2.setOnlineOccupancyRate((year2.getSoldRoomNightsNet() / year2.getRoomnightsToSell()) * 100);
        } else {
            year2.setOnlineOccupancyRate(0.0);
        }

        // Calculate onlineOccupancyRate for year3
        if (year3.getRoomnightsToSell() > 0) {
            year3.setOnlineOccupancyRate((year3.getSoldRoomNightsNet() / year3.getRoomnightsToSell()) * 100);
        } else {
            year3.setOnlineOccupancyRate(0.0);
        }

        // Calculate onlineOccupancyRate for year4
        if (year4.getRoomnightsToSell() > 0) {
            year4.setOnlineOccupancyRate((year4.getSoldRoomNightsNet() / year4.getRoomnightsToSell()) * 100);
        } else {
            year4.setOnlineOccupancyRate(0.0);
        }

        // Calculate ADR for lastMonthReq
        if (lastMonthReq.getSoldRoomNightsNet() != 0) {
            lastMonthReq.setAdr(lastMonthReq.getRevenueFromDB() / lastMonthReq.getSoldRoomNightsNet());
        } else {
            lastMonthReq.setAdr(0.0);
        }

        // Calculate ADR for year1
        if (year1.getSoldRoomNightsNet() != 0) {
            year1.setAdr(year1.getRevenueFromDB() / year1.getSoldRoomNightsNet());
        } else {
            year1.setAdr(0.0);
        }

        // Calculate ADR for year2
        if (year2.getSoldRoomNightsNet() != 0) {
            year2.setAdr(year2.getRevenueFromDB() / year2.getSoldRoomNightsNet());
        } else {
            year2.setAdr(0.0);
        }

        // Calculate ADR for year3
        if (year3.getSoldRoomNightsNet() != 0) {
            year3.setAdr(year3.getRevenueFromDB() / year3.getSoldRoomNightsNet());
        } else {
            year3.setAdr(0.0);
        }

        // Calculate ADR for year4
        if (year4.getSoldRoomNightsNet() != 0) {
            year4.setAdr(year4.getRevenueFromDB() / year4.getSoldRoomNightsNet());
        } else {
            year4.setAdr(0.0);
        }

        // Calculate onlineRevPAR for lastMonthReq
        if (lastMonthReq.getAdr() != 0 && lastMonthReq.getOnlineOccupancyRate() != 0) {
            lastMonthReq.setOnlineRevPAR((lastMonthReq.getAdr() * lastMonthReq.getOnlineOccupancyRate()) / 100);
        } else {
            lastMonthReq.setOnlineRevPAR(0.0);
        }

        // Calculate onlineRevPAR for year1
        if (year1.getAdr() != 0 && year1.getOnlineOccupancyRate() != 0) {
            year1.setOnlineRevPAR((year1.getAdr() * year1.getOnlineOccupancyRate()) / 100);
        } else {
            year1.setOnlineRevPAR(0.0);
        }

        // Calculate onlineRevPAR for year2
        if (year2.getAdr() != 0 && year2.getOnlineOccupancyRate() != 0) {
            year2.setOnlineRevPAR((year2.getAdr() * year2.getOnlineOccupancyRate()) / 100);
        } else {
            year2.setOnlineRevPAR(0.0);
        }

        // Calculate onlineRevPAR for year3
        if (year3.getAdr() != 0 && year3.getOnlineOccupancyRate() != 0) {
            year3.setOnlineRevPAR((year3.getAdr() * year3.getOnlineOccupancyRate()) / 100);
        } else {
            year3.setOnlineRevPAR(0.0);
        }

        // Calculate onlineRevPAR for year4
        if (year4.getAdr() != 0 && year4.getOnlineOccupancyRate() != 0) {
            year4.setOnlineRevPAR((year4.getAdr() * year4.getOnlineOccupancyRate()) / 100);
        } else {
            year4.setOnlineRevPAR(0.0);
        }

        Map<Integer, String> monthMap = new HashMap<>();
        monthMap.put(1, "jan");
        monthMap.put(2, "feb");
        monthMap.put(3, "mar");
        monthMap.put(4, "apr");
        monthMap.put(5, "may");
        monthMap.put(6, "jun");
        monthMap.put(7, "jul");
        monthMap.put(8, "aug");
        monthMap.put(9, "sep");
        monthMap.put(10, "oct");
        monthMap.put(11, "nov");
        monthMap.put(12, "dec");


        if (buName == null || buName.isEmpty()) {
            buName = "CultSwitch";
        }

        lastMonthReq.setNetBookings(lastMonthReq.getNetBookings());
        lastMonthReq.setGrossBookings(lastMonthReq.getGrossBookings());
        lastMonthReq.setCancellations(lastMonthReq.getCancellations());
        lastMonthReq.setGbv(round(lastMonthReq.getGbv() / 1000, 1));
        lastMonthReq.setCancellationVolume(round(lastMonthReq.getCancellationVolume() / 1000, 1));
        lastMonthReq.setRevenueFromDB(round(lastMonthReq.getRevenueFromDB() / 1000, 2));
        lastMonthReq.setSoldRoomNightsGross(lastMonthReq.getSoldRoomNightsGross());
        lastMonthReq.setSoldRoomNightsNet(lastMonthReq.getSoldRoomNightsNet());
        lastMonthReq.setRevenuePerBooking(round(lastMonthReq.getRevenuePerBooking(), 2));
        lastMonthReq.setAdr(round(lastMonthReq.getAdr(), 2));
        lastMonthReq.setDirectRevenue(round(lastMonthReq.getDirectRevenue(), 2));
        lastMonthReq.setIndirectRevenue(round(lastMonthReq.getIndirectRevenue(), 2));
        lastMonthReq.setCancellationRate(round(lastMonthReq.getCancellationRate(), 1));
        lastMonthReq.setBookingsPerDay(round(lastMonthReq.getBookingsPerDay(), 2));
        lastMonthReq.setRoomnightsPerBooking(round(lastMonthReq.getRoomnightsPerBooking(), 2));
        lastMonthReq.setConnectedChannels(lastMonthReq.getConnectedChannels());
        lastMonthReq.setDaysOfCurrentMonth(lastMonthReq.getDaysOfCurrentMonth());
        lastMonthReq.setHotelCapacity(hotelCapacity); // Replace $hotelCapacity with corresponding Java variable
        lastMonthReq.setRoomnightsToSell(lastMonthReq.getRoomnightsToSell());
        lastMonthReq.setOnlineOccupancyRate(round(lastMonthReq.getOnlineOccupancyRate(), 2));
        lastMonthReq.setOnlineRevPAR(round(lastMonthReq.getOnlineRevPAR(), 2));

        year1.setNetBookings(year1.getNetBookings());
        year1.setGrossBookings(year1.getGrossBookings());
        year1.setCancellations(year1.getCancellations());
        year1.setGbv(Math.round(year1.getGbv() / 1000.0 * 10) / 10.0); // Rounded to 1 decimal
        year1.setCancellationVolume(Math.round(year1.getCancellationVolume() / 1000.0 * 10) / 10.0);
        year1.setRevenueFromDB(Math.round(year1.getRevenueFromDB() / 1000.0 * 100) / 100.0); // Rounded to 2 decimals
        year1.setSoldRoomNightsGross(year1.getSoldRoomNightsGross());
        year1.setSoldRoomNightsNet(year1.getSoldRoomNightsNet());
        year1.setRevenuePerBooking(Math.round(year1.getRevenuePerBooking() * 100) / 100.0);
        year1.setAdr(Math.round(year1.getAdr() * 100) / 100.0); // ADR is mapped to `adr` in Java
        year1.setDirectRevenue(Math.round(year1.getDirectRevenue() * 100) / 100.0);
        year1.setIndirectRevenue(Math.round(year1.getIndirectRevenue() * 100) / 100.0);
        year1.setCancellationRate(Math.round(year1.getCancellationRate() * 10) / 10.0); // Rounded to 1 decimal
        year1.setBookingsPerDay(Math.round(year1.getBookingsPerDay() * 100) / 100.0);
        year1.setRoomnightsPerBooking(Math.round(year1.getRoomnightsPerBooking() * 100) / 100.0);
        year1.setConnectedChannels(year1.getConnectedChannels());
        year1.setDaysOfCurrentMonth(year1.getDaysOfCurrentMonth());
        year1.setHotelCapacity(hotelCapacity); // Replace $hotelCapacity with the corresponding Java variable
        year1.setRoomnightsToSell(year1.getRoomnightsToSell());
        year1.setOnlineOccupancyRate(Math.round(year1.getOnlineOccupancyRate() * 100) / 100.0);
        year1.setOnlineRevPAR(Math.round(year1.getOnlineRevPAR() * 100) / 100.0);

        year2.setNetBookings(year2.getNetBookings());
        year2.setGrossBookings(year2.getGrossBookings());
        year2.setCancellations(year2.getCancellations());
        year2.setGbv(round(year2.getGbv() / 1000, 1));
        year2.setCancellationVolume(round(year2.getCancellationVolume() / 1000, 1));
        year2.setRevenueFromDB(round(year2.getRevenueFromDB() / 1000, 2));
        year2.setSoldRoomNightsGross(year2.getSoldRoomNightsGross());
        year2.setSoldRoomNightsNet(year2.getSoldRoomNightsNet());
        year2.setRevenuePerBooking(round(year2.getRevenuePerBooking(), 2));
        year2.setAdr(round(year2.getAdr(), 2));
        year2.setDirectRevenue(round(year2.getDirectRevenue(), 2));
        year2.setIndirectRevenue(round(year2.getIndirectRevenue(), 2));
        year2.setCancellationRate(round(year2.getCancellationRate(), 1));
        year2.setBookingsPerDay(round(year2.getBookingsPerDay(), 2));
        year2.setRoomnightsPerBooking(round(year2.getRoomnightsPerBooking(), 2));
        year2.setConnectedChannels(year2.getConnectedChannels());
        year2.setDaysOfCurrentMonth(year2.getDaysOfCurrentMonth());
        year2.setHotelCapacity(hotelCapacity);
        year2.setRoomnightsToSell(year2.getRoomnightsToSell());
        year2.setOnlineOccupancyRate(round(year2.getOnlineOccupancyRate(), 2));
        year2.setOnlineRevPAR(round(year2.getOnlineRevPAR(), 2));


        year3.setNetBookings(year3.getNetBookings());
        year3.setGrossBookings(year3.getGrossBookings());
        year3.setCancellations(year3.getCancellations());
        year3.setGbv(round(year3.getGbv() / 1000, 1));
        year3.setCancellationVolume(round(year3.getCancellationVolume() / 1000, 1));
        year3.setRevenueFromDB(round(year3.getRevenueFromDB() / 1000, 2));
        year3.setSoldRoomNightsGross(year3.getSoldRoomNightsGross());
        year3.setSoldRoomNightsNet(year3.getSoldRoomNightsNet());
        year3.setRevenuePerBooking(round(year3.getRevenuePerBooking(), 2));
        year3.setAdr(round(year3.getAdr(), 2));
        year3.setDirectRevenue(round(year3.getDirectRevenue(), 2));
        year3.setIndirectRevenue(round(year3.getIndirectRevenue(), 2));
        year3.setCancellationRate(round(year3.getCancellationRate(), 1));
        year3.setBookingsPerDay(round(year3.getBookingsPerDay(), 2));
        year3.setRoomnightsPerBooking(round(year3.getRoomnightsPerBooking(), 2));
        year3.setConnectedChannels(year3.getConnectedChannels());
        year3.setDaysOfCurrentMonth(year3.getDaysOfCurrentMonth());
        year3.setHotelCapacity(hotelCapacity); // Replace $hotelCapacity with corresponding Java variable
        year3.setRoomnightsToSell(year3.getRoomnightsToSell());
        year3.setOnlineOccupancyRate(round(year3.getOnlineOccupancyRate(), 2));
        year3.setOnlineRevPAR(round(year3.getOnlineRevPAR(), 2));


        year4.setNetBookings(year4.getNetBookings());
        year4.setGrossBookings(year4.getGrossBookings());
        year4.setCancellations(year4.getCancellations());
        year4.setGbv(round(year4.getGbv() / 1000, 1));
        year4.setCancellationVolume(round(year4.getCancellationVolume() / 1000, 1));
        year4.setRevenueFromDB(round(year4.getRevenueFromDB() / 1000, 2));
        year4.setSoldRoomNightsGross(year4.getSoldRoomNightsGross());
        year4.setSoldRoomNightsNet(year4.getSoldRoomNightsNet());
        year4.setRevenuePerBooking(round(year4.getRevenuePerBooking(), 2));
        year4.setAdr(round(year4.getAdr(), 2));
        year4.setDirectRevenue(round(year4.getDirectRevenue(), 2));
        year4.setIndirectRevenue(round(year4.getIndirectRevenue(), 2));
        year4.setCancellationRate(round(year4.getCancellationRate(), 1));
        year4.setBookingsPerDay(round(year4.getBookingsPerDay(), 2));
        year4.setRoomnightsPerBooking(round(year4.getRoomnightsPerBooking(), 2));
        year4.setConnectedChannels(year4.getConnectedChannels());
        year4.setDaysOfCurrentMonth(year4.getDaysOfCurrentMonth());
        year4.setHotelCapacity(hotelCapacity); // Replace $hotelCapacity with corresponding Java variable
        year4.setRoomnightsToSell(year4.getRoomnightsToSell());
        year4.setOnlineOccupancyRate(round(year4.getOnlineOccupancyRate(), 2));
        year4.setOnlineRevPAR(round(year4.getOnlineRevPAR(), 2));

        lastMonthReq.setProducingChannelsList(new ArrayList<>());
        year1.setProducingChannelsList(new ArrayList<>());
        year2.setProducingChannelsList(new ArrayList<>());
        year3.setProducingChannelsList(new ArrayList<>());
        year4.setProducingChannelsList(new ArrayList<>());


        Map<String, Object> jsonRequest = new HashMap<>();
        jsonRequest.put("object_id", objectID);
        jsonRequest.put("object_name", objectName);
        jsonRequest.put("template_id", templateID);
        jsonRequest.put("business_unit_name", buName);
        jsonRequest.put("year", year);
        jsonRequest.put("month", month);

        Map<String, Object> monthlyData = new HashMap<>();
        monthlyData.put("last_month", lastMonthReq);
        monthlyData.put("year1", year1);
        monthlyData.put("year2", year2);
        monthlyData.put("yeartodate1", year3);
        monthlyData.put("yeartodate2", year4);

        String shortMonth = monthMap.get(month);
        Map<String, Object> data = new HashMap<>();
        data.put(shortMonth, monthlyData);

        jsonRequest.put("data", data);

        return jsonRequest.toString();
    }



    private static int calDaysInYear(int year, int endMonth) {
        int days = 0;
        for (int month = 1; month <= endMonth; month++) {
            days += YearMonth.of(year, month).lengthOfMonth();
        }
        return days;
    }
}