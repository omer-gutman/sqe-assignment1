package ac.il.bgu.qa;

import ac.il.bgu.qa.errors.BookAlreadyBorrowedException;
import ac.il.bgu.qa.errors.BookNotFoundException;
import ac.il.bgu.qa.errors.UserNotRegisteredException;
import ac.il.bgu.qa.services.DatabaseService;
import ac.il.bgu.qa.services.NotificationService;
import ac.il.bgu.qa.services.ReviewService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    // ---------- returnBook ----------

    // ---------- notifyUserWithBookReviews ----------

    // ---------- getBookByISBN ----------
}
