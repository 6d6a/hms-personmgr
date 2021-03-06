package ru.majordomo.hms.personmgr.service.Cerb;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.majordomo.hms.personmgr.common.Department;
import ru.majordomo.hms.personmgr.dto.cerb.*;
import ru.majordomo.hms.personmgr.dto.cerb.api.*;
import ru.majordomo.hms.personmgr.exception.ParameterValidationException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CerbApiClient {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private String baseUrl;
    private String access;
    private String secret;
    private RestTemplate restTemplate;

    public CerbApiClient(
            @Value("${cerb.url}") String baseUrl,
            @Value("${cerb.access}") String accessKey,
            @Value("${cerb.secret}") String secretKey
    ) {
        this.baseUrl = baseUrl;
        this.access = accessKey;
        this.secret = secretKey;

        restTemplate = new RestTemplate();

        ObjectMapper objMapper = new ObjectMapper().configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objMapper);
        restTemplate.setMessageConverters(Arrays.asList(converter, new StringHttpMessageConverter(), new FormHttpMessageConverter(), new ByteArrayHttpMessageConverter()));
    }

    private String getFormattedDate() {
        LocalDateTime date = LocalDateTime.now(Clock.systemUTC());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yy HH:mm:ss +0000", Locale.US);
        return date.format(formatter);
    }

    private URI buildUri(String url) {
        return buildUri(url, null);
    }

    private URI buildUri(String url, MultiValueMap<String, String> queryParams) {
        if (queryParams != null) {
            Map<String, List<String>> sorted = new TreeMap<>(queryParams); //???????????????????? ?? ???????????????????? ?????????????? (??????????!)
            MultiValueMap<String, String> queryMap = new LinkedMultiValueMap<>();
            queryMap.putAll(sorted);
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + url).queryParams(queryMap);
            return builder.build().encode().toUri();
        } else {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + url);
            return builder.build().encode().toUri();
        }
    }

    private String getRequestSignature(
            HttpMethod method,
            String date,
            URI url
    ) {
        return getRequestSignature(method, date, url, null);
    }

    private String getRequestSignature(
            HttpMethod method,
            String date,
            URI url,
            String postParams
    ) {
        String toSig = method + "\n" +
                date + "\n" +
                url.getPath() + "\n" +
                (url.getRawQuery() == null ? "" : url.getRawQuery()) + "\n" +
                (postParams == null ? "" : postParams) + "\n" +
                DigestUtils.md5Hex(secret) + "\n";

        return DigestUtils.md5Hex(toSig);
    }

    private HttpEntity<String> preparePostRequestData(URI uri, MultiValueMap<String, String> postParams) throws UnsupportedEncodingException {
        String requestDate = getFormattedDate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Date", requestDate);
        headers.add("Cerb-Auth", access + ":"
                + getRequestSignature(HttpMethod.POST, requestDate, uri, postParamsToSignatureString(postParams)));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return new HttpEntity<>(postParamsToSignatureString(postParams), headers);
    }

    private HttpEntity<String> preparePutRequestData(URI uri, MultiValueMap<String, String> postParams) throws UnsupportedEncodingException {
        String requestDate = getFormattedDate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Date", requestDate);
        headers.add("Cerb-Auth", access + ":"
                + getRequestSignature(HttpMethod.PUT, requestDate, uri, postParamsToSignatureString(postParams)));
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        return new HttpEntity<>(postParamsToSignatureString(postParams), headers);
    }

    private HttpEntity<String> prepareGetRequestData(URI uri) {
        String requestDate = getFormattedDate();

        HttpHeaders headers = new HttpHeaders();
        headers.add("Date", requestDate);
        headers.add("Cerb-Auth", access + ":"
                + getRequestSignature(HttpMethod.GET, requestDate, uri));

        return new HttpEntity<>("parameters", headers);
    }

    private String postParamsToSignatureString(MultiValueMap<String, String> queryParams) throws UnsupportedEncodingException {
        StringBuilder queryString = new StringBuilder();

        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
                if (queryString.length() > 0) {
                    queryString.append("&");
                }

                queryString.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue().get(0), "UTF-8"));
            }
            queryString.append("&");
        }

        return queryString.toString();
    }

    private ResponseEntity<GetTicketApiResponse> getTicket(Integer ticketId) {
        try {
            URI uri = buildUri("/records/ticket/" + ticketId + ".json");
            HttpEntity<String> request = prepareGetRequestData(uri);
            return restTemplate.exchange(uri, HttpMethod.GET, request, GetTicketApiResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private void changeTicketStatus(Integer ticketId, TicketStatus ticketStatus) {
        try {
            MultiValueMap<String, String> postParams = new LinkedMultiValueMap<>();
            postParams.add("fields[status]", ticketStatus.toString().toLowerCase());
            ZoneId zoneId = ZoneId.systemDefault();
            long unixtime = LocalDateTime.now().atZone(zoneId).toEpochSecond();
            postParams.add("fields[updated]", Long.toString(unixtime));

            URI uri = buildUri("records/ticket/" + ticketId + ".json");
            HttpEntity<String> request = preparePutRequestData(uri, postParams);
            ResponseEntity<BaseApiResponse> response = restTemplate.exchange(uri, HttpMethod.PUT, request, BaseApiResponse.class);

            if (response.getBody() != null) {
                if (response.getBody().getStatus().equals("success")) {
                    return;
                }
            }

            logger.error("[CerbApiClient] ???????????? ?????? ???????????????????? ?????????????? Ticket. "
                    + (response.getBody() != null ? "?????????? ???? api: " + response.getBody() : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????????? ?????????????? Ticket. Exception: " + e.getMessage());
        }
    }

    private Integer findSenderByEmail(String email) {
        try {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("limit", "100");
            queryParams.add("q", "email:" + email);

            URI uri = buildUri("records/sender/search.json", queryParams);
            HttpEntity<String> request = prepareGetRequestData(uri);
            ResponseEntity<SearchSenderApiResponse> response = restTemplate.exchange(uri, HttpMethod.GET, request, SearchSenderApiResponse.class);

            if (response.getBody() != null) {
                if (response.getBody().getStatus().equals("success")) {
                    return response.getBody().getResults().get(0).getId();
                }
            }

            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Sender. "
                    + (response.getBody() != null ? "?????????? ???? api: " + response.getBody() : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Sender. Exception: " + e.getMessage());
        }

        return null;
    }

    private Integer createSender(String email) {
        try {
            MultiValueMap<String, String> postParams = new LinkedMultiValueMap<>();
            postParams.add("fields[email]", email);

            URI uri = buildUri("records/sender/create.json");
            HttpEntity<String> request = preparePostRequestData(uri, postParams);
            ResponseEntity<BaseObjectApiResponse> response = restTemplate.exchange(uri, HttpMethod.POST, request, BaseObjectApiResponse.class);

            if (response.getBody() != null) {
                if (response.getBody().getStatus().equals("success")) {
                    return response.getBody().getId();
                }
            }

            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Sender. "
                    + (response.getBody() != null ? "?????????? ???? api: " + response.getBody() : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Sender. Exception: " + e.getMessage());
        }

        return null;
    }

    private Integer createMessage(Integer ticketId, Integer senderId, String content) {
        String requestDate = getFormattedDate();

        try {
            MultiValueMap<String, String> postParams = new LinkedMultiValueMap<>();
            postParams.add("fields[ticket_id]", ticketId.toString());
            postParams.add("fields[content]", content);
            postParams.add("fields[sender_id]", senderId.toString());
            postParams.add("fields[headers]", "Date: " + requestDate);

            URI uri = buildUri("records/message/create.json");
            HttpEntity<String> request = preparePostRequestData(uri, postParams);
            ResponseEntity<BaseObjectApiResponse> response = restTemplate.exchange(uri, HttpMethod.POST, request, BaseObjectApiResponse.class);

            if (response.getBody() != null) {
                if (response.getBody().getStatus().equals("success")) return response.getBody().getId();
            }

            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? message. "
                    + (response.getBody() != null ? "?????????? ???? api: " + response.getBody() : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? message. Exception: " + e.getMessage());
        }

        return null;
    }

    private Integer createTicket(String subject, Department department, String participantEmail) {

        String bucketId = null;
        String groupId = null;

        switch (department) {
            case FIN:
                bucketId = "12";
                groupId = "7";
                break;
            case INFO:
                bucketId = "19";
                groupId = "19";
                break;
            case DOMAIN:
                bucketId = "17";
                groupId = "16";
                break;
            case TECH:
                bucketId = "27";
                groupId = "31";
                break;
        }

        try {
            MultiValueMap<String, String> postParams = new LinkedMultiValueMap<>();
            postParams.add("fields[bucket_id]", bucketId);
            postParams.add("fields[group_id]", groupId);
            postParams.add("fields[status]", "open");
            postParams.add("fields[subject]", subject);
            postParams.add("fields[participants]", participantEmail);

            URI uri = buildUri("records/ticket/create.json");
            HttpEntity<String> request = preparePostRequestData(uri, postParams);
            ResponseEntity<CreateTicketApiResponse> response = restTemplate.exchange(uri, HttpMethod.POST, request, CreateTicketApiResponse.class);

            if (response.getBody() != null) {
                if (response.getBody().getStatus().equals("success")) return response.getBody().getId();
            }

            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Ticket. "
                    + (response.getBody() != null ? "?????????? ???? api: " + response.getBody() : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Ticket. Exception: " + e.getMessage());
        }

        return null;
    }

    public Integer sendTicket(String subject, Department department, String contactEmails,  String content) {

        String groupId = null;

        switch (department) {
            case FIN:
                groupId = "7";
                break;
            case INFO:
                groupId = "19";
                break;
            case DOMAIN:
                groupId = "16";
                break;
            case TECH:
                groupId = "31";
                break;
            case PLAYGROUND:
                groupId = "73";
                break;
        }


        try {
            MultiValueMap<String, String> postParams = new LinkedMultiValueMap<>();
            postParams.add("content", content);
            postParams.add("content_format", "markdown");
            postParams.add("group_id", groupId);
            postParams.add("html_template_id", "2");
            postParams.add("subject", subject);
            postParams.add("to", contactEmails);

            URI uri = buildUri("tickets/compose.json");
            HttpEntity<String> request = preparePostRequestData(uri, postParams);
            ResponseEntity<CreateTicketApiResponse> response = restTemplate.exchange(uri, HttpMethod.POST, request, CreateTicketApiResponse.class);

            if (response.getBody() != null) {
                if (response.getBody().getStatus().equals("success")) return response.getBody().getId();
            }

            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Ticket. "
                    + (response.getBody() != null ? "?????????? ???? api: " + response.getBody() : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Ticket. Exception: " + e.getMessage());
        }

        return null;
    }

    @Cacheable("getTicketWithMessages")
    public Ticket getTicketWithMessages(Integer tiketId) {
        try {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            queryParams.add("q", "ticket.id:" + tiketId);
            queryParams.add("expand", "content,attachments");
            queryParams.add("limit", "100");

            URI uri = buildUri("records/message/search.json", queryParams);
            HttpEntity<String> request = prepareGetRequestData(uri);
            ResponseEntity<SearchMessageApiResponse> response = restTemplate.exchange(uri, HttpMethod.GET, request, SearchMessageApiResponse.class);

            if (response.getBody() != null && response.getBody().getStatus() != null && response.getBody().getStatus().equals("success")) {
                Ticket t = new Ticket();
                t.setTicketId(tiketId);

                ResponseEntity<GetTicketApiResponse> ticketResponse = this.getTicket(tiketId);

                t.setMask(ticketResponse.getBody().getMask());
                t.setStatus(TicketStatus.valueOf(ticketResponse.getBody().getStatus().toUpperCase()));
                t.setSubject(ticketResponse.getBody().getSubject());

                List<Message> messages = new ArrayList<>();
                response.getBody().getResults().forEach(item -> {
                    Message m = new Message();
                    m.setContent(item.getContent());
                    m.setMessageId(item.getMessageId());
                    m.setIsOurWorker(item.getWorkerId() != 0 || (item.getWorkerId() == 0 && item.getSenderId() == 70));
                    LocalDateTime createdTime = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.getCreated()),
                            TimeZone.getDefault().toZoneId());
                    m.setCreated(createdTime);
                    if (item.getAttachments() != null) {
                        m.setAttachments(item.getAttachments().values().stream()
                                .filter(attach -> attach != null && !"original_message.html".equals(attach.getName()))
                                .collect(Collectors.toList())
                        );
                    } else {
                        m.setAttachments(Collections.emptyList());
                    }
                    messages.add(m);
                });

                messages.sort((s1, s2) -> {
                    if (s1.getCreated().isAfter(s2.getCreated())) {
                        return -1;
                    } else if (s1.getCreated().isBefore(s2.getCreated())) {
                        return 1;
                    } else {
                        return 0;
                    }
                });

                t.setMessages(messages);
                return t;
            }

            logger.error("[CerbApiClient] ???????????? ?????? ???????????? ???????????????? Message. "
                    + (response.getBody() != null ? "?????????? ???? api: " + response.getBody() : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????? ???????????????? Message. Exception: " + e.getMessage());
        }

        return null;
    }

    public AttachmentDownloadResponse getAttachment(Integer attachmentId) {
        try {
            URI uri = buildUri("attachments/" + attachmentId + "/download.json");
            HttpEntity<String> request = prepareGetRequestData(uri);
            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, request, byte[].class);
            if (response.getBody() != null && response.getStatusCode().is2xxSuccessful()) {
                AttachmentDownloadResponse result = new AttachmentDownloadResponse();
                result.setBody(response.getBody());
                if (response.getHeaders().getContentType() != null) {
                    MediaType mt = response.getHeaders().getContentType();
                    result.setContentType(mt.toString());
                }
                return result;
            }
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ??????????. " + (response.getBody() != null ? "?????????? ???? api: "
                    + Arrays.toString(Arrays.copyOf(response.getBody(), Math.min(16, response.getBody().length)))
                    : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ??????????. Exception: " + e.getMessage());
        }

        return null;
    }

    public Ticket processUserRequestToCerberus(String subject, String email, String content, Department department) {
        return processUserRequestToCerberus(subject, email, content, department, null);
    }

    public Ticket processUserRequestToCerberus(String subject, String email, String content, Department department, Map<String, String> attachment) {
        Integer senderId = this.findSenderByEmail(email);
        if (senderId == null) {
            senderId = this.createSender(email);
        }

        if (senderId == null) {
            throw new ParameterValidationException("???? ?????????????? ?????????????? ???????????? ?? ???????????? ??????????????????.");
        }

        Integer ticketId = this.createTicket(subject, department, email);

        if (ticketId == null) {
            throw new ParameterValidationException("???? ?????????????? ?????????????? ???????????? ?? ???????????? ??????????????????.");
        }

        Integer messageId = this.createMessage(ticketId, senderId, content);

        if (messageId == null) {
            throw new ParameterValidationException("???? ?????????????? ?????????????? ???????????? ?? ???????????? ??????????????????.");
        }

        if (attachment != null && !attachment.isEmpty()) {
            createAttachment(
                    attachment.get("filename"),
                    attachment.get("mime_type"),
                    attachment.get("body"),
                    messageId
            );
        }

        Ticket ticket = this.getTicketWithMessages(ticketId);
        ticket.setSenderId(senderId);

        return ticket;
    }

    @CacheEvict(value="getTicketWithMessages", beforeInvocation = true, key = "#ticketId")
    public void processUserReplayToTicket(Integer ticketId, Integer senderId, String content, Map<String, String> attachment) {
        Integer messageId = this.createMessage(ticketId, senderId, content);

        if (messageId == null) {
            throw new ParameterValidationException("???? ?????????????? ?????????????? ??????????????????.");
        }

        changeTicketStatus(ticketId, TicketStatus.OPEN);

        if (attachment != null && !attachment.isEmpty()) {
            createAttachment(
                    attachment.get("filename"),
                    attachment.get("mime_type"),
                    attachment.get("body"),
                    messageId
            );
        }
    }

    private Integer createAttachment(String name, String mimeType, String content, Integer messageId) {
        try {
            MultiValueMap<String, String> postParams = new LinkedMultiValueMap<>();
            postParams.add("fields[name]", name);
            postParams.add("fields[mime_type]", mimeType);
            postParams.add("fields[content]", "data:application/octet-stream;base64," + content); //fields[content]=data:application/octet-stream;base64,[BASE64-ENCODED-CONTENT]
            postParams.add("fields[attach][]", "message:" + messageId);

            URI uri = buildUri("records/attachment/create.json");
            HttpEntity<String> request = preparePostRequestData(uri, postParams);
            ResponseEntity<BaseObjectApiResponse> response = restTemplate.exchange(uri, HttpMethod.POST, request, BaseObjectApiResponse.class);

            if (response.getBody() != null) {
                if (response.getBody().getStatus().equals("success")) return response.getBody().getId();
            }

            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Attachment. "
                    + (response.getBody() != null ? "?????????? ???? api: " + response.getBody() : "?????????? ???? api ???? ??????????????."));
        } catch (Exception e) {
            logger.error("[CerbApiClient] ???????????? ?????? ???????????????? ?????????????? Attachment. Exception: " + e.getMessage());
        }

        return null;
    }
}
