package ac.il.bgu.qa;

import ac.il.bgu.qa.services.DatabaseService;
import ac.il.bgu.qa.services.NotificationService;
import ac.il.bgu.qa.services.ReviewService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the {@link Library} class.
 *
 * Isolation: every collaborator of Library (DatabaseService, ReviewService,
 * Book, User, NotificationService) is replaced by a Mockito mock, so only the
 * logic inside Library is tested.
 *
 * Naming convention (from practical session 1):
 *   Given[Condition]_When[MethodUnderTest]_Then[ExpectedResult]
 *
 * Every test follows the AAA pattern: Arrange / Act / Assert.
 *
 * NOTE: the official grader compiles this file at Java 8 language level and
 * without mockito-junit-jupiter, so:
 *   - do not use @ExtendWith(MockitoExtension.class) - we use openMocks() instead
 *   - do not use 'var', List.of(...), text blocks, switch expressions, etc.
 */
public class TestLibrary {

    // ---------- Shared test data ----------

    /** A valid ISBN-13 (check digit is correct). */
    private static final String VALID_ISBN = "9780306406157";
    /** A valid user id - exactly 12 digits. */
    private static final String VALID_USER_ID = "123456789012";
    private static final String VALID_TITLE = "Clean Code";
    private static final String VALID_AUTHOR = "Robert Martin";
    private static final String VALID_USER_NAME = "Dana";

    // ---------- Mocks (collaborators of the class under test) ----------

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

    // ---------- Class under test ----------

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

    // ---------- Helpers ----------

    /** Stubs mockBook to look like a valid, not-borrowed book. */
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

    // =====================================================================
    // addBook  (owner: Partner A)
    // =====================================================================

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
        // Act + Assert
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(null));
        assertEquals("Invalid book.", e.getMessage());
        verify(mockDatabaseService, never()).addBook(anyString(), any(Book.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "123", "97803064061571", "978030640615X", "9780306406158"})
    void GivenInvalidIsbn_WhenAddBook_ThenThrowIllegalArgumentException(String invalidIsbn) {
        // Arrange
        when(mockBook.getISBN()).thenReturn(invalidIsbn);

        // Act + Assert
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> library.addBook(mockBook));
        assertEquals("Invalid ISBN.", e.getMessage());
        verify(mockDatabaseService, never()).addBook(anyString(), any(Book.class));
    }

    // TODO Partner A: null ISBN, ISBN with hyphens, check digit == 0 case,
    //      invalid/empty title, invalid authors (see isAuthorValid rules),
    //      already-borrowed book, book that already exists in DB.

    // =====================================================================
    // registerUser  (owner: Partner A)
    // =====================================================================

    // TODO Partner A

    // =====================================================================
    // borrowBook  (owner: Partner A)
    // =====================================================================

    // TODO Partner A

    // =====================================================================
    // returnBook  (owner: Partner B)
    // =====================================================================

    // TODO Partner B

    // =====================================================================
    // notifyUserWithBookReviews  (owner: Partner B)
    // =====================================================================

    // TODO Partner B: null/empty reviews, ReviewException, reviewService.close()
    //      always called, exact message format, retry up to 5 times.

    // =====================================================================
    // getBookByISBN  (owner: Partner B)
    // =====================================================================

    // TODO Partner B
}
