package com.apulia.loanservice.service;

import com.apulia.loanservice.client.resilience.BookServiceClient;
import com.apulia.loanservice.client.resilience.MemberServiceClient;
import com.apulia.loanservice.dto.LoanDetailsDTO;
import com.apulia.loanservice.dto.LoanRequestDTO;
import com.apulia.loanservice.exception.LoanNotFoundException;
import com.apulia.loanservice.exception.ValidationException;
import com.apulia.loanservice.model.Loan;
import com.apulia.loanservice.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookServiceClient bookServiceClient;
    private final MemberServiceClient memberServiceClient;

    public LoanService(LoanRepository loanRepository,
                       BookServiceClient bookServiceClient,
                       MemberServiceClient memberServiceClient) {
        this.loanRepository = loanRepository;
        this.bookServiceClient = bookServiceClient;
        this.memberServiceClient = memberServiceClient;
    }

    // GET ALL
    @Transactional(readOnly = true)
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    // GET BY ID
    @Transactional(readOnly = true)
    public Loan getLoanById(Integer id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new LoanNotFoundException("Loan with ID " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public LoanDetailsDTO getLoanDetailsById(Integer id) {
        Loan loan = getLoanById(id);
        var book = bookServiceClient.getBookById(loan.getBookId());
        var member = memberServiceClient.getMemberById(loan.getMemberId());
        return new LoanDetailsDTO(
                loan,
                book,
                member,
                loan.getLoanDate(),
                loan.getReturnDate()
        );
    }



    // CREATE MULTIPLE LOANS
    @Transactional
    public List<Loan> createLoans(LoanRequestDTO request) {

        validateRequest(request);
        validateMember(request.getMemberId());
        return request.getBookIds().stream()
                .map(bookId -> createSingleLoan(bookId, request.getMemberId()))
                .toList();
    }

    private Loan createSingleLoan(Integer bookId, Integer memberId) {
        validateBook(bookId);
        if (isBookAlreadyLoaned(bookId)) {
            throw new ValidationException("Book with ID " + bookId + " is already loaned");
        }

        Loan loan = new Loan(bookId, memberId, LocalDate.now());
        return loanRepository.save(loan);
    }

    // RETURN BOOK
    @Transactional
    public Loan returnBook(Integer id) {
        Loan loan = getLoanById(id);

        if (loan.getReturnDate() != null) {
            throw new ValidationException("Loan with ID " + id + " is already returned");
        }

        loan.setReturnDate(LocalDate.now());
        return loanRepository.save(loan);
    }

    // DELETE
    @Transactional
    public void deleteLoan(Integer id) {
        Loan loan = getLoanById(id);
        loanRepository.delete(loan);
    }

    // CHECK IF BOOK IS ALREADY LOANED
    public boolean isBookAlreadyLoaned(Integer bookId) {
        return loanRepository.findByBookIdAndReturnDateIsNull(bookId).isPresent();
    }


    // VALIDATION HELPERS
    private void validateRequest(LoanRequestDTO request) {
        if (request.getBookIds() == null || request.getBookIds().isEmpty()) {
            throw new ValidationException("Book list cannot be empty");
        }
    }

    private void validateBook(Integer bookId) {
        bookServiceClient.getBookById(bookId);
    }

    private void validateMember(Integer memberId) {
        memberServiceClient.getMemberById(memberId);
    }

    public List<LoanDetailsDTO> getLoanDetails() {
        List<Loan> loans = loanRepository.findAll();
        return loans.stream()
                .map(loan -> {
                    var book = bookServiceClient.getBookById(loan.getBookId());
                    var member = memberServiceClient.getMemberById(loan.getMemberId());
                    return new LoanDetailsDTO(
                            loan,
                            book,
                            member,
                            loan.getLoanDate(),
                            loan.getReturnDate()
                    );
                })
                .toList();
    }

}
