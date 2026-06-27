package com.apulia.loanservice.client;

import com.apulia.loanservice.dto.MemberDTO;
import com.apulia.loanservice.exception.ValidationException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

class MemberClientTest {

    private WireMockServer wireMock;
    private MemberClient memberClient;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(options().dynamicPort());
        wireMock.start();

        memberClient = new MemberClient(new RestTemplate(), "http://localhost:" + wireMock.port() + "/members");
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void getMemberById_shouldReturnMember_whenServiceResponds200() {
        wireMock.stubFor(get(urlEqualTo("/members/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"firstName\":\"John\",\"lastName\":\"Doe\",\"city\":\"Rome\",\"phone\":\"+391234567890\"}")));

        MemberDTO member = memberClient.getMemberById(1);

        assertNotNull(member);
        assertEquals(1, member.getId());
        assertEquals("John", member.getFirstName());
        assertEquals("Doe", member.getLastName());
    }

    @Test
    void getMemberById_shouldThrowValidationException_whenServiceResponds404() {
        wireMock.stubFor(get(urlEqualTo("/members/999"))
                .willReturn(aResponse().withStatus(404)));

        assertThrows(ValidationException.class, () -> memberClient.getMemberById(999));
    }

    @Test
    void getMemberById_shouldPropagateException_whenServiceResponds500() {
        wireMock.stubFor(get(urlEqualTo("/members/1"))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(Exception.class, () -> memberClient.getMemberById(1));
    }
}
