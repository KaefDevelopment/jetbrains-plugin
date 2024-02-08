import io.nautime.jetbrains.NauPlugin.QueueView
import org.junit.Test
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class QueueViewTest {


    @Test
    fun fileTest() {
        val queue: Queue<Int> = ConcurrentLinkedQueue()
        for (i in 1..5) {
            queue += i
        }

        val view: QueueView<Int> = QueueView(queue, 3)
        for (i in 6..10) {
            queue += i
        }

        view.forEach { println(it) }


    }
}
