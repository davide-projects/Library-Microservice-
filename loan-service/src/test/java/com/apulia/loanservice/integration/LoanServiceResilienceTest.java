package com.apulia.loanservice.integration;

import com.apulia.loanservice.dto.LoanDetailsDTO;
import com.apulia.loanservice.dto.LoanRequestDTO;
import com.apulia.loanservice.exception.ServiceUnavailableException;
import com.apulia.loanservice.exception.ValidationException;
import com.apulia.loanservice.model.Loan;
import com.apulia.loanservice.repository.LoanRepository;
import com.apulia.loanservice.service.LoanService;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class LoanServiceResilienceTest {

    private static WireMockServer bookServiceMock;
    private static WireMockServer memberServiceMock;

    @Autowired
    private LoanService loanService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private LoanRepository loanRepository;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        bookServiceMock = new WireMockServer(options().dynamicPort());
        bookServiceMock.start();

        memberServiceMock = new WireMockServer(options().dynamicPort());
        memberServiceMock.start();

        registry.add("microservices.book-service.url", () -> "http://localhost:" + bookServiceMock.port() + "/book");
        registry.add("microservices.member-service.url", () -> "http://localhost:" + memberServiceMock.port() + "/members");
    }

    @AfterAll
    static void cleanup() {
        if (bookServiceMock != null) bookServiceMock.stop();
        if (memberServiceMock != null) memberServiceMock.stop();
    }

    @BeforeEach
    void reset() {
        bookServiceMock.resetAll();
        memberServiceMock.resetAll();
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(cb -> cb.reset());
    }

    @Test
    void getLoanDetailsById_shouldReturnDetails_whenBothServicesRespond() {
        int loanId = 1;
        int bookId = 10;
        int memberId = 100;

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(createLoan(loanId, bookId, memberId)));

        bookServiceMock.stubFor(get(urlEqualTo("/book/" + bookId))
                .willReturn(okJson("{\"id\":10,\"title\":\"Test Book\",\"author\":\"Author\",\"publisher\":\"Publisher\"}")));

        memberServiceMock.stubFor(get(urlEqualTo("/members/" + memberId))
                .willReturn(okJson("{\"id\":100,\"firstName\":\"John\",\"lastName\":\"Doe\",\"city\":\"Rome\",\"phone\":\"+391234567890\"}")));

        LoanDetailsDTO details = loanService.getLoanDetailsById(loanId);

        assertThat(details).isNotNull();
        assertThat(details.book().getId()).isEqualTo(bookId);
        assertThat(details.member().getId()).isEqualTo(memberId);
    }

    @Test
    void getLoanDetailsById_shouldThrowValidationException_whenBookReturns404() {
        int loanId = 1;
        int bookId = 10;
        int memberId = 100;

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(createLoan(loanId, bookId, memberId)));

        bookServiceMock.stubFor(get(urlEqualTo("/book/" + bookId))
                .willReturn(aResponse().withStatus(404)));

        assertThrows(ValidationException.class, () -> loanService.getLoanDetailsById(loanId));
    }

    @Test
    void getLoanDetailsById_shouldThrowServiceUnavailable_whenBookServiceFails() {
        int loanId = 1;
        int bookId = 10;
        int memberId = 100;

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(createLoan(loanId, bookId, memberId)));

        bookServiceMock.stubFor(get(urlEqualTo("/book/" + bookId))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(ServiceUnavailableException.class, () -> loanService.getLoanDetailsById(loanId));
    }

    @Test
    void getLoanDetailsById_shouldThrowServiceUnavailable_whenMemberServiceFails() {
        int loanId = 1;
        int bookId = 10;
        int memberId = 100;

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(createLoan(loanId, bookId, memberId)));

        bookServiceMock.stubFor(get(urlEqualTo("/book/" + bookId))
                .willReturn(okJson("{\"id\":10,\"title\":\"Test Book\",\"author\":\"Author\",\"publisher\":\"Publisher\"}")));

        memberServiceMock.stubFor(get(urlEqualTo("/members/" + memberId))
                .willReturn(aResponse().withStatus(500)));

        assertThrows(ServiceUnavailableException.class, () -> loanService.getLoanDetailsById(loanId));
    }

    @Test
    void createLoans_shouldCreateLoans_whenBookAndMemberExist() {
        int bookId = 10;
        int memberId = 100;

        when(loanRepository.save(any(Loan.class))).thenReturn(createLoan(null, bookId, memberId));
        when(loanRepository.findByBookIdAndReturnDateIsNull(bookId)).thenReturn(Optional.empty());

        bookServiceMock.stubFor(get(urlEqualTo("/book/" + bookId))
                .willReturn(okJson("{\"id\":10,\"title\":\"Test Book\",\"author\":\"Author\",\"publisher\":\"Publisher\"}")));

        memberServiceMock.stubFor(get(urlEqualTo("/members/" + memberId))
                .willReturn(okJson("{\"id\":100,\"firstName\":\"John\",\"lastName\":\"Doe\",\"city\":\"Rome\",\"phone\":\"+391234567890\"}")));

        LoanRequestDTO request = new LoanRequestDTO(List.of(bookId), memberId);

        List<Loan> loans = loanService.createLoans(request);

        assertThat(loans).hasSize(1);
    }

    @Test
    void createLoans_shouldThrowValidationException_whenMemberNotFound() {
        int bookId = 10;
        int memberId = 999;

        LoanRequestDTO request = new LoanRequestDTO(List.of(bookId), memberId);

        assertThrows(ValidationException.class, () -> loanService.createLoans(request));
    }

    @Test
    void deleteLoan_shouldWork_withoutCallingExternalServices() {
        int loanId = 1;
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(createLoan(loanId, 10, 100)));

        loanService.deleteLoan(loanId);

        // Should not throw - external services are not called
    }

    @Test
    void returnBook_shouldWork_withoutCallingExternalServices() {
        int loanId = 1;
        Loan loan = createLoan(loanId, 10, 100);
        loan.setReturnDate(null);
        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.returnBook(loanId);

        assertThat(result.getReturnDate()).isNotNull();
    }

    private static Loan createLoan(Integer id, int bookId, int memberId) {
        Loan loan = new Loan(bookId, memberId, LocalDate.of(2026, 6, 1));
        if (id != null) {
            loan.setId(id);
        }
        return loan;
    }
}
