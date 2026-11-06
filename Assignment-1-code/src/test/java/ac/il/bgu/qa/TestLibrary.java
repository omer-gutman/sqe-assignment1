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
 * All collaborators are mocked so only Library's logic is tested.
 */
public class TestLibrary {

    // ---------- Test data ----------

    private static final String VALID_ISBN = "9780306406157";
    private static final String VALID_TITLE = "Clean Code";
    private static final String VALID_AUTHOR = "Robert Martin";
    private static final String VALID_USER_ID = "123456789012";

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

    // ---------- addBook ----------

    @Test
    void GivenValidBook_WhenAddBook_ThenBookIsAddedToDatabase() {
        // Arrange
        when(mockBook.getISBN()).thenReturn(VALID_ISBN);
        when(mockBook.getTitle()).thenReturn(VALID_TITLE);
        when(mockBook.getAuthor()).thenReturn(VALID_AUTHOR);
        when(mockBook.isBorrowed()).thenReturn(false);
        when(mockDatabaseService.getBookByISBN(VALID_ISBN)).thenReturn(null);

        // Act
        library.addBook(mockBook);

        // Assert
        verify(mockDatabaseService).addBook(VALID_ISBN, mockBook);
    }

    // ---------- registerUser ----------

    // ---------- borrowBook ----------

    // ---------- returnBook ----------

    // ---------- notifyUserWithBookReviews ----------

    // ---------- getBookByISBN ----------
}
