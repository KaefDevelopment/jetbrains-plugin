import junit.framework.TestCase.assertTrue
import java.io.File

internal class FileSenderTest {

//    @Test
    fun fileTest() {
        val TEST_TEXT = "event"

        val file = File("src/test/resources/eventLog.txt")
        file.writeText("")
        file.appendText(TEST_TEXT)

        val text = file.readText()

        assertTrue(text.contains(TEST_TEXT))
    }

}
