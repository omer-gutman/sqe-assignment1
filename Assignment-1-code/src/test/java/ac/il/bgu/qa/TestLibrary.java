package ac.il.bgu.qa;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import ac.il.bgu.qa.errors.BookAlreadyBorrowedException;
import ac.il.bgu.qa.errors.BookNotBorrowedException;
import ac.il.bgu.qa.errors.BookNotFoundException;
import ac.il.bgu.qa.errors.NoReviewsFoundException;
import ac.il.bgu.qa.errors.NotificationException;
import ac.il.bgu.qa.errors.ReviewException;
import ac.il.bgu.qa.errors.ReviewServiceUnavailableException;
import ac.il.bgu.qa.errors.UserNotRegisteredException;
import ac.il.bgu.qa.services.DatabaseService;
import ac.il.bgu.qa.services.NotificationService;
import ac.il.bgu.qa.services.ReviewService;

/**
 * Unit tests for the {@link Library} class.
 * All collaborators are mocked so only Library's logic is tested.
 */
public class TestLibrary {

    // ---------- Test data ----------

    private static final String VALID_ISBN = "9780306406157";
    private static final String VALID_TITLE = "Clean Code";
    private static final String VALID_AUTHOR = "Robert Martin";
    private static final String VALID_USER_ID = "123456789012";
    private static final String VALID_USER_NAME = "Dana";

    @Mock
    private DatabaseService mockDatabaseService;

    @Mock
    private ReviewService mockReviewService;

    @Mock
    private NotificationService mockNotificationService;

    @Mock
    private Book mockBook;

    @Mock
    private User mockUser;

    private Library library;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        library = new Library(mockDatabaseService, mockReviewService);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    // ---------- Shared helpers and parameter sources ----------

    /** Stubs mockBook to look like a valid book that is not borrowed. */
    private void stubValidBook() {
        when(mockBook.getISBN()).thenReturn(VALID_ISBN);
        when(mockBook.getTitle()).thenReturn(VALID_TITLE);
        when(mockBook.getAuthor()).thenReturn(VALID_AUTHOR);
        when(mockBook.isBorrowed()).thenReturn(false);
    }

    /** Stubs mockUser to look like a valid user. */
    private void stubValidUser() {
        when(mockUser.getId()).thenReturn(VALID_USER_ID);
        when(mockUser.getName()).thenReturn(VALID_USER_NAME);
        when(mockUser.getNotificationService()).thenReturn(mockNotificationService);
    }

    /** ISBNs that are not valid ISBN-13 values (null, wrong length, non-digits, wrong check digit). */
    static Stream<String> invalidIsbns() {
        return Stream.of(
                null,
                "",
                "978030640615",       // 12 digits
                "97803064061570",     // 14 digits
                "978030640615X",      // letter as the check digit
                "97803A6406157",      // letter in the middle
                "97803064 6157",      // space instead of a digit
                "9780306406158",      // wrong check digit
                "9783161484101"       // wrong check digit (correct one is 0)
        );
    }

    /** User ids that are not exactly 12 digits. */
    static Stream<String> invalidUserIds() {
        return Stream.of(
                null,
                "",
                "12345678901",        // 11 digits
                "1234567890123",      // 13 digits
                "12345678901a",       // 12 characters, one is a letter
                "-12345678901"        // 12 characters, one is a sign
        );
    }

    // ---------- addBook ----------

    @Test
    void GivenValidBook_WhenAddBook_ThenBookIsAddedToDatabase() {
        // Arrange
        stubValidBook();
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(null);

        // Act
        library.addBook(mockBook);

        // Assert
        verify(mockDatabaseService).addBook(VALID_ISBN, mockBook);
    }

    @Test
    void GivenNullBook_WhenAddBook_ThenThrowIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(null));

        // Assert
        assertEquals("Invalid book.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @MethodSource("invalidIsbns")
    void GivenInvalidIsbn_WhenAddBook_ThenThrowIllegalArgumentException(String invalidIsbn) {
        // Arrange
        stubValidBook();
        when(mockBook.getISBN()).thenReturn(invalidIsbn);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(mockBook));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "978-0-306-40615-7",  // hyphens are removed before validation
            "9783161484100",      // check digit computes to 10, which becomes 0
            "9780000000002"
    })
    void GivenValidIsbnFormat_WhenAddBook_ThenBookIsAddedToDatabase(String validIsbn) {
        // Arrange
        stubValidBook();
        when(mockBook.getISBN()).thenReturn(validIsbn);

        // Act
        library.addBook(mockBook);

        // Assert
        verify(mockDatabaseService).addBook(validIsbn, mockBook);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenInvalidTitle_WhenAddBook_ThenThrowIllegalArgumentException(String invalidTitle) {
        // Arrange
        stubValidBook();
        when(mockBook.getTitle()).thenReturn(invalidTitle);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(mockBook));

        // Assert
        assertEquals("Invalid title.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "1Robert",            // starts with a digit
            "-Robert",            // starts with a special character
            "Robert1",            // ends with a digit
            "Robert.",            // ends with a special character
            "Rob3rt Martin",      // digit inside the name
            "Robert@Martin",      // character that is not allowed
            "A@B",                // not allowed, just before the last letter
            "A3B",                // digit just before the last letter
            "Robert--Martin",     // consecutive hyphens
            "O''Brien",           // consecutive apostrophes
            "Robert  Martin",     // consecutive spaces
            "Robert..Martin"      // consecutive dots
    })
    void GivenInvalidAuthor_WhenAddBook_ThenThrowIllegalArgumentException(String invalidAuthor) {
        // Arrange
        stubValidBook();
        when(mockBook.getAuthor()).thenReturn(invalidAuthor);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(mockBook));

        // Assert
        assertEquals("Invalid author.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "R",                  // single letter
            "Jean-Paul Sartre",   // hyphen and space
            "O'Brien",            // apostrophe
            "J.R.R. Tolkien",     // dots, and a dot followed by a space
            "Robert C. Martin"
    })
    void GivenValidAuthor_WhenAddBook_ThenBookIsAddedToDatabase(String validAuthor) {
        // Arrange
        stubValidBook();
        when(mockBook.getAuthor()).thenReturn(validAuthor);

        // Act
        library.addBook(mockBook);

        // Assert
        verify(mockDatabaseService).addBook(VALID_ISBN, mockBook);
    }

    @Test
    void GivenBorrowedBook_WhenAddBook_ThenThrowIllegalArgumentException() {
        // Arrange
        stubValidBook();
        when(mockBook.isBorrowed()).thenReturn(true);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(mockBook));

        // Assert
        assertEquals("Book with invalid borrowed state.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @Test
    void GivenBookAlreadyInDatabase_WhenAddBook_ThenThrowIllegalArgumentException() {
        // Arrange
        stubValidBook();
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(mockBook));

        // Assert
        assertEquals("Book already exists.", exception.getMessage());
        verify(mockDatabaseService, never()).addBook(anyString(), any(Book.class));
    }

    // ---------- registerUser ----------

    @Test
    void GivenValidUser_WhenRegisterUser_ThenUserIsRegisteredInDatabase() {
        // Arrange
        stubValidUser();
        when(mockDatabaseService.getUserById(VALID_USER_ID)).thenReturn(null);

        // Act
        library.registerUser(mockUser);

        // Assert
        verify(mockDatabaseService).registerUser(VALID_USER_ID, mockUser);
    }

    @Test
    void GivenNullUser_WhenRegisterUser_ThenThrowIllegalArgumentException() {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(null));

        // Assert
        assertEquals("Invalid user.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIds")
    void GivenInvalidUserId_WhenRegisterUser_ThenThrowIllegalArgumentException(String invalidUserId) {
        // Arrange
        stubValidUser();
        when(mockUser.getId()).thenReturn(invalidUserId);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(mockUser));

        // Assert
        assertEquals("Invalid user Id.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenInvalidUserName_WhenRegisterUser_ThenThrowIllegalArgumentException(String invalidName) {
        // Arrange
        stubValidUser();
        when(mockUser.getName()).thenReturn(invalidName);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(mockUser));

        // Assert
        assertEquals("Invalid user name.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @Test
    void GivenNullNotificationService_WhenRegisterUser_ThenThrowIllegalArgumentException() {
        // Arrange
        stubValidUser();
        when(mockUser.getNotificationService()).thenReturn(null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(mockUser));

        // Assert
        assertEquals("Invalid notification service.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @Test
    void GivenUserAlreadyInDatabase_WhenRegisterUser_ThenThrowIllegalArgumentException() {
        // Arrange
        stubValidUser();
        when(mockDatabaseService.getUserById(VALID_USER_ID)).thenReturn(mockUser);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.registerUser(mockUser));

        // Assert
        assertEquals("User already exists.", exception.getMessage());
        verify(mockDatabaseService, never()).registerUser(anyString(), any(User.class));
    }

    // ---------- borrowBook ----------

    @Test
    void GivenAvailableBookAndRegisteredUser_WhenBorrowBook_ThenBookIsBorrowed() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        when(mockDatabaseService.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        when(mockBook.isBorrowed()).thenReturn(false);

        // Act
        library.borrowBook(VALID_ISBN, VALID_USER_ID);

        // Assert
        verify(mockBook).borrow();
        verify(mockDatabaseService).borrowBook(VALID_ISBN, VALID_USER_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "978-0-306-40615-7",  // hyphens: must reach the database unchanged
            "9783161484100"       // check digit 0
    })
    void GivenValidIsbnFormat_WhenBorrowBook_ThenBookIsBorrowedWithSameIsbn(String validIsbn) {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validIsbn)).thenReturn(mockBook);
        when(mockDatabaseService.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        when(mockBook.isBorrowed()).thenReturn(false);

        // Act
        library.borrowBook(validIsbn, VALID_USER_ID);

        // Assert
        verify(mockBook).borrow();
        verify(mockDatabaseService).borrowBook(validIsbn, VALID_USER_ID);
    }

    @ParameterizedTest
    @MethodSource("invalidIsbns")
    void GivenInvalidIsbn_WhenBorrowBook_ThenThrowIllegalArgumentException(String invalidIsbn) {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook(invalidIsbn, VALID_USER_ID));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService);
    }

    @Test
    void GivenBookNotInDatabase_WhenBorrowBook_ThenThrowBookNotFoundException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(null);

        // Act
        BookNotFoundException exception = assertThrows(BookNotFoundException.class,
                () -> library.borrowBook(VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("Book not found!", exception.getMessage());
        verify(mockDatabaseService, never()).borrowBook(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("invalidUserIds")
    void GivenInvalidUserId_WhenBorrowBook_ThenThrowIllegalArgumentException(String invalidUserId) {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> library.borrowBook(VALID_ISBN, invalidUserId));

        // Assert
        assertEquals("Invalid user Id.", exception.getMessage());
        verify(mockBook, never()).borrow();
        verify(mockDatabaseService, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void GivenUserNotInDatabase_WhenBorrowBook_ThenThrowUserNotRegisteredException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        when(mockDatabaseService.getUserById(VALID_USER_ID)).thenReturn(null);

        // Act
        UserNotRegisteredException exception = assertThrows(UserNotRegisteredException.class,
                () -> library.borrowBook(VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("User not found!", exception.getMessage());
        verify(mockBook, never()).borrow();
        verify(mockDatabaseService, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void GivenBookAlreadyBorrowed_WhenBorrowBook_ThenThrowBookAlreadyBorrowedException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        when(mockDatabaseService.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        when(mockBook.isBorrowed()).thenReturn(true);

        // Act
        BookAlreadyBorrowedException exception = assertThrows(BookAlreadyBorrowedException.class,
                () -> library.borrowBook(VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("Book is already borrowed!", exception.getMessage());
        verify(mockBook, never()).borrow();
        verify(mockDatabaseService, never()).borrowBook(anyString(), anyString());
    }

    @Test
    void GivenAvailableBook_WhenBorrowBook_ThenUpdateBookBeforeDatabase() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        when(mockDatabaseService.getUserById(VALID_USER_ID)).thenReturn(mockUser);
        when(mockBook.isBorrowed()).thenReturn(false);

        // Act
        library.borrowBook(VALID_ISBN, VALID_USER_ID);

        // Assert
        InOrder order = inOrder(mockBook, mockDatabaseService);
        order.verify(mockBook).borrow();
        order.verify(mockDatabaseService).borrowBook(VALID_ISBN, VALID_USER_ID);
    }

    // ---------- returnBook ----------
    @ParameterizedTest
    @ValueSource(strings = {
            "9780306406157",
            "978-0-306-40615-7",
            "9783161484100"
    })
    void GivenBorrowedBook_WhenReturnBook_ThenBookIsReturned(String validIsbn) {
        // Arrange
        when(mockDatabaseService.getBookByISBN(validIsbn)).thenReturn(mockBook);
        when(mockBook.isBorrowed()).thenReturn(true);

        // Act
        library.returnBook(validIsbn);

        // Assert
        verify(mockBook).returnBook();
        verify(mockDatabaseService).returnBook(validIsbn);
    }

    @ParameterizedTest
    @MethodSource("invalidIsbns")
    void GivenInvalidIsbn_WhenReturnBook_ThenThrowIllegalArgumentException(
            String invalidIsbn) {
        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> library.returnBook(invalidIsbn));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(mockDatabaseService, mockBook);
    }

    @Test
    void GivenBookNotInDatabase_WhenReturnBook_ThenThrowBookNotFoundException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(null);

        // Act
        BookNotFoundException exception = assertThrows(
                BookNotFoundException.class,
                () -> library.returnBook(VALID_ISBN));

        // Assert
        assertEquals("Book not found!", exception.getMessage());
        verify(mockDatabaseService, never()).returnBook(any());
        verifyNoInteractions(mockBook);
    }

    @Test
    void GivenBookNotBorrowed_WhenReturnBook_ThenThrowBookNotBorrowedException() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        when(mockBook.isBorrowed()).thenReturn(false);

        // Act
        BookNotBorrowedException exception = assertThrows(
                BookNotBorrowedException.class,
                () -> library.returnBook(VALID_ISBN));

        // Assert
        assertEquals("Book wasn't borrowed!", exception.getMessage());
        verify(mockBook, never()).returnBook();
        verify(mockDatabaseService, never()).returnBook(any());
    }
    @Test
    void GivenBorrowedBook_WhenReturnBook_ThenUpdateBookBeforeDatabase() {
        // Arrange
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(mockBook);
        when(mockBook.isBorrowed()).thenReturn(true);

        // Act
        library.returnBook(VALID_ISBN);

        // Assert
        InOrder order = inOrder(mockBook, mockDatabaseService);
        order.verify(mockBook).returnBook();
        order.verify(mockDatabaseService).returnBook(VALID_ISBN);
    }

    // ---------- notifyUserWithBookReviews ----------
    private void stubValidReviewNotification() {
        stubValidBook();
        stubValidUser();

        when(mockDatabaseService.getBookByISBN(VALID_ISBN))
                .thenReturn(mockBook);
        when(mockDatabaseService.getUserById(VALID_USER_ID))
                .thenReturn(mockUser);
        when(mockReviewService.getReviewsForBook(VALID_ISBN))
                .thenReturn(Arrays.asList("First review", "Second review"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9780306406157",
            "978-0-306-40615-7",
            "9783161484100"
    })
    void GivenBookAndUserWithReviews_WhenNotifyUserWithBookReviews_ThenSendFormattedReviews(
            String validIsbn) {
        // Arrange
        stubValidReviewNotification();
        when(mockDatabaseService.getBookByISBN(validIsbn))
                .thenReturn(mockBook);
        when(mockReviewService.getReviewsForBook(validIsbn))
                .thenReturn(Arrays.asList("First review", "Second review"));

        String expectedMessage = "Reviews for '" + VALID_TITLE
                + "':\nFirst review\nSecond review";

        // Act
        library.notifyUserWithBookReviews(validIsbn, VALID_USER_ID);

        // Assert
        verify(mockDatabaseService).getBookByISBN(validIsbn);
        verify(mockDatabaseService).getUserById(VALID_USER_ID);
        verify(mockReviewService).getReviewsForBook(validIsbn);
        verify(mockReviewService).close();
        verify(mockUser).sendNotification(expectedMessage);
    }

    @ParameterizedTest
    @MethodSource("invalidIsbns")
    void GivenInvalidIsbn_WhenNotifyUserWithBookReviews_ThenThrowIllegalArgumentException(
            String invalidIsbn) {
        // Arrange
        stubValidReviewNotification();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> library.notifyUserWithBookReviews(
                        invalidIsbn, VALID_USER_ID));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(
                mockDatabaseService, mockReviewService, mockUser);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIds")
    void GivenInvalidUserId_WhenNotifyUserWithBookReviews_ThenThrowIllegalArgumentException(
            String invalidUserId) {
        // Arrange
        stubValidReviewNotification();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> library.notifyUserWithBookReviews(
                        VALID_ISBN, invalidUserId));

        // Assert
        assertEquals("Invalid user Id.", exception.getMessage());
        verifyNoInteractions(
                mockDatabaseService, mockReviewService, mockUser);
    }

    @Test
    void GivenBookNotInDatabase_WhenNotifyUserWithBookReviews_ThenThrowBookNotFoundException() {
        // Arrange
        stubValidReviewNotification();
        when(mockDatabaseService.getBookByISBN(VALID_ISBN))
                .thenReturn(null);

        // Act
        BookNotFoundException exception = assertThrows(
                BookNotFoundException.class,
                () -> library.notifyUserWithBookReviews(
                        VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("Book not found!", exception.getMessage());
        verify(mockDatabaseService, never()).getUserById(any());
        verifyNoInteractions(mockReviewService, mockUser);
    }

    @Test
    void GivenUserNotInDatabase_WhenNotifyUserWithBookReviews_ThenThrowUserNotRegisteredException() {
        // Arrange
        stubValidReviewNotification();
        when(mockDatabaseService.getUserById(VALID_USER_ID))
                .thenReturn(null);

        // Act
        UserNotRegisteredException exception = assertThrows(
                UserNotRegisteredException.class,
                () -> library.notifyUserWithBookReviews(
                        VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("User not found!", exception.getMessage());
        verifyNoInteractions(mockReviewService, mockUser);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenNoReviews_WhenNotifyUserWithBookReviews_ThenThrowNoReviewsFoundException(
            List<String> reviews) {
        // Arrange
        stubValidReviewNotification();
        when(mockReviewService.getReviewsForBook(VALID_ISBN))
                .thenReturn(reviews);

        // Act
        NoReviewsFoundException exception = assertThrows(
                NoReviewsFoundException.class,
                () -> library.notifyUserWithBookReviews(
                        VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("No reviews found!", exception.getMessage());
        verify(mockReviewService).close();
        verify(mockUser, never()).sendNotification(any());
    }

    @Test
    void GivenReviewServiceFails_WhenNotifyUserWithBookReviews_ThenThrowReviewServiceUnavailableException() {
        // Arrange
        stubValidReviewNotification();
        when(mockReviewService.getReviewsForBook(VALID_ISBN))
                .thenThrow(new ReviewException("Review lookup failed"));

        // Act
        ReviewServiceUnavailableException exception = assertThrows(
                ReviewServiceUnavailableException.class,
                () -> library.notifyUserWithBookReviews(
                        VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("Review service unavailable!", exception.getMessage());
        verify(mockReviewService).close();
        verify(mockUser, never()).sendNotification(any());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void GivenNotificationInitiallyFails_WhenNotifyUserWithBookReviews_ThenRetryUntilSuccess(
            int failuresBeforeSuccess) {
        // Arrange
        stubValidReviewNotification();

        Throwable[] failures = new Throwable[failuresBeforeSuccess];
        for (int i = 0; i < failures.length; i++) {
            failures[i] = new NotificationException("Temporary delivery failure");
        }

        doThrow(failures)
                .doNothing()
                .when(mockUser).sendNotification(any());

        String expectedMessage = "Reviews for '" + VALID_TITLE
                + "':\nFirst review\nSecond review";

        // Act
        library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID);

        // Assert
        verify(mockUser, times(failuresBeforeSuccess + 1))
                .sendNotification(expectedMessage);
        verify(mockReviewService).getReviewsForBook(VALID_ISBN);
        verify(mockReviewService).close();
    }

    @Test
    void GivenNotificationAlwaysFails_WhenNotifyUserWithBookReviews_ThenThrowAfterFiveAttempts() {
        // Arrange
        stubValidReviewNotification();
        doThrow(new NotificationException("Delivery failed"))
                .when(mockUser).sendNotification(any());

        String expectedMessage = "Reviews for '" + VALID_TITLE
                + "':\nFirst review\nSecond review";

        // Act
        NotificationException exception = assertThrows(
                NotificationException.class,
                () -> library.notifyUserWithBookReviews(
                        VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("Notification failed!", exception.getMessage());
        verify(mockUser, times(5)).sendNotification(expectedMessage);
        verify(mockReviewService).getReviewsForBook(VALID_ISBN);
        verify(mockReviewService).close();
    }
    @Test
    void GivenReviewsAvailable_WhenNotifyUserWithBookReviews_ThenFetchBeforeClosingAndSending() {
        // Arrange
        stubValidReviewNotification();
        String expectedMessage = "Reviews for '" + VALID_TITLE
                + "':\nFirst review\nSecond review";

        // Act
        library.notifyUserWithBookReviews(VALID_ISBN, VALID_USER_ID);

        // Assert
        InOrder order = inOrder(mockReviewService, mockUser);
        order.verify(mockReviewService).getReviewsForBook(VALID_ISBN);
        order.verify(mockReviewService).close();
        order.verify(mockUser).sendNotification(expectedMessage);
    }

    // ---------- getBookByISBN ----------
        @ParameterizedTest
    @ValueSource(strings = {
            "9780306406157",
            "978-0-306-40615-7",
            "9783161484100"
    })
    void GivenAvailableBook_WhenGetBookByISBN_ThenReturnBookAndNotifyUser(
            String validIsbn) {
        // Arrange
        stubValidReviewNotification();
        when(mockDatabaseService.getBookByISBN(validIsbn))
                .thenReturn(mockBook);
        when(mockReviewService.getReviewsForBook(validIsbn))
                .thenReturn(Arrays.asList("First review", "Second review"));

        String expectedMessage = "Reviews for '" + VALID_TITLE
                + "':\nFirst review\nSecond review";

        // Act
        Book result = library.getBookByISBN(validIsbn, VALID_USER_ID);

        // Assert
        assertSame(mockBook, result);
        verify(mockDatabaseService, times(2)).getBookByISBN(validIsbn);
        verify(mockReviewService).getReviewsForBook(validIsbn);
        verify(mockReviewService).close();
        verify(mockUser).sendNotification(expectedMessage);
        verify(mockBook, never()).borrow();
        verify(mockDatabaseService, never()).borrowBook(any(), any());
    }

    @ParameterizedTest
    @MethodSource("invalidIsbns")
    void GivenInvalidIsbn_WhenGetBookByISBN_ThenThrowIllegalArgumentException(
            String invalidIsbn) {
        // Arrange
        stubValidReviewNotification();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> library.getBookByISBN(invalidIsbn, VALID_USER_ID));

        // Assert
        assertEquals("Invalid ISBN.", exception.getMessage());
        verifyNoInteractions(
                mockDatabaseService, mockReviewService, mockUser);
    }

    @ParameterizedTest
    @MethodSource("invalidUserIds")
    void GivenInvalidUserId_WhenGetBookByISBN_ThenThrowIllegalArgumentException(
            String invalidUserId) {
        // Arrange
        stubValidReviewNotification();

        // Act
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> library.getBookByISBN(VALID_ISBN, invalidUserId));

        // Assert
        assertEquals("Invalid user Id.", exception.getMessage());
        verifyNoInteractions(
                mockDatabaseService, mockReviewService, mockUser);
    }

    @Test
    void GivenBookNotInDatabase_WhenGetBookByISBN_ThenThrowBookNotFoundException() {
        // Arrange
        stubValidReviewNotification();
        when(mockDatabaseService.getBookByISBN(VALID_ISBN))
                .thenReturn(null);

        // Act
        BookNotFoundException exception = assertThrows(
                BookNotFoundException.class,
                () -> library.getBookByISBN(VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("Book not found!", exception.getMessage());
        verify(mockDatabaseService, never()).getUserById(any());
        verifyNoInteractions(mockReviewService, mockUser);
    }

    @Test
    void GivenBorrowedBook_WhenGetBookByISBN_ThenThrowBookAlreadyBorrowedException() {
        // Arrange
        stubValidReviewNotification();
        when(mockBook.isBorrowed()).thenReturn(true);

        // Act
        BookAlreadyBorrowedException exception = assertThrows(
                BookAlreadyBorrowedException.class,
                () -> library.getBookByISBN(VALID_ISBN, VALID_USER_ID));

        // Assert
        assertEquals("Book was already borrowed!", exception.getMessage());
        verify(mockDatabaseService, never()).getUserById(any());
        verifyNoInteractions(mockReviewService, mockUser);
    }

    @Test
    void GivenUserNotInDatabase_WhenGetBookByISBN_ThenReturnBook() {
        // Arrange
        stubValidReviewNotification();
        when(mockDatabaseService.getUserById(VALID_USER_ID))
                .thenReturn(null);

        // Act
        Book result = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);

        // Assert
        assertSame(mockBook, result);
        verifyNoInteractions(mockReviewService, mockUser);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void GivenNoReviews_WhenGetBookByISBN_ThenReturnBook(List<String> reviews) {
        // Arrange
        stubValidReviewNotification();
        when(mockReviewService.getReviewsForBook(VALID_ISBN))
                .thenReturn(reviews);

        // Act
        Book result = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);

        // Assert
        assertSame(mockBook, result);
        verify(mockReviewService).close();
        verify(mockUser, never()).sendNotification(any());
    }

    @Test
    void GivenReviewServiceFails_WhenGetBookByISBN_ThenReturnBook() {
        // Arrange
        stubValidReviewNotification();
        when(mockReviewService.getReviewsForBook(VALID_ISBN))
                .thenThrow(new ReviewException("Review lookup failed"));

        // Act
        Book result = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);

        // Assert
        assertSame(mockBook, result);
        verify(mockReviewService).close();
        verify(mockUser, never()).sendNotification(any());
    }

    @Test
    void GivenNotificationAlwaysFails_WhenGetBookByISBN_ThenReturnBook() {
        // Arrange
        stubValidReviewNotification();
        doThrow(new NotificationException("Delivery failed"))
                .when(mockUser).sendNotification(any());

        String expectedMessage = "Reviews for '" + VALID_TITLE
                + "':\nFirst review\nSecond review";

        // Act
        Book result = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);

        // Assert
        assertSame(mockBook, result);
        verify(mockUser, times(5)).sendNotification(expectedMessage);
        verify(mockReviewService).close();
    }

    @Test
    void GivenReviewServiceCloseFails_WhenGetBookByISBN_ThenReturnBook() {
        // Arrange
        stubValidReviewNotification();
        doThrow(new IllegalStateException("Closing failed"))
                .when(mockReviewService).close();

        // Act
        Book result = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);

        // Assert
        assertSame(mockBook, result);
        verify(mockReviewService).close();
        verify(mockUser, never()).sendNotification(any());
    }

    @Test
    void GivenBookDisappearsDuringNotification_WhenGetBookByISBN_ThenReturnOriginallyFoundBook() {
        // Arrange
        stubValidReviewNotification();
        when(mockDatabaseService.getBookByISBN(VALID_ISBN))
                .thenReturn(mockBook)
                .thenReturn(null);

        // Act
        Book result = library.getBookByISBN(VALID_ISBN, VALID_USER_ID);

        // Assert
        assertSame(mockBook, result);
        verify(mockDatabaseService, times(2)).getBookByISBN(VALID_ISBN);
        verifyNoInteractions(mockReviewService, mockUser);
    }
}
