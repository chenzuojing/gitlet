import org.junit.Test;
import static org.junit.Assert.*;

public class ArrayDequeTest {

    @Test
    public void arrayLengthTest() {
        ArrayDeque<String> strArray = new ArrayDeque<>();
        assertEquals(8, strArray.size());
    }

    @Test
    public void addFirstTest() {
        ArrayDeque<String> strArray = new ArrayDeque<>();
        assertEquals(8, strArray.size());
        strArray.addFirst("zero");
        strArray.printDeque();
        strArray.addFirst("one");
        strArray.printDeque();
        strArray.addFirst("two");
        strArray.printDeque();
    }

    @Test
    public void addLastTest() {
        ArrayDeque<String> strArray = new ArrayDeque<>();
        assertEquals(8, strArray.size());
        strArray.addLast("zero");
        strArray.printDeque();
        strArray.addLast("one");
        strArray.printDeque();
        strArray.addLast("two");
        strArray.printDeque();
    }
}
