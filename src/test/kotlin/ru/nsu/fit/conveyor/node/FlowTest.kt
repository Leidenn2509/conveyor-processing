package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.nsu.fit.conveyor.commonNodes.img.FilterNode
import ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
import ru.nsu.fit.conveyor.commonNodes.img.Gaussian
import ru.nsu.fit.conveyor.commonNodes.img.Image
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
class FlowTest {
    @Test
    fun testSimpleFlow() = runBlocking {
        val flow = Flow("").apply {
            addInput(0, Image::class)
            addOutput(0, Image::class)
            addNode(0, GaussFilter())
            connectFlowInput(0, node(0), 0)
            connectFlowOutput(0, node(0), 0)
        }
        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image = Image(width, height, type)
        flow.sendArg(0, image)

        flow.tryRun(this)

        val out = flow.receiveArg(0) as Image
        Assertions.assertEquals(out.width, width)
        Assertions.assertEquals(out.height, height)
        Assertions.assertEquals(out.type, type)
        Assertions.assertEquals(out.operations.size, 1)
        Assertions.assertEquals(out.operations.first()::class, Gaussian::class)
    }

    @Test
    fun testFlow() = runBlocking {
        val flow = Flow("").apply {
            addInput(0, Image::class)
            addOutput(0, Image::class)
            addNode(0, GaussFilter())
            addNode(1, GaussFilter())

            connect(0, 0, 1, 0)

            connectFlowInput(0, node(0), 0)
            connectFlowOutput(0, node(1), 0)
        }
        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image = Image(width, height, type)
        flow.sendArg(0, image)

        flow.tryRun(this)

        val out = flow.receiveArg(0) as Image
        Assertions.assertEquals(out.width, width)
        Assertions.assertEquals(out.height, height)
        Assertions.assertEquals(out.type, type)
        Assertions.assertEquals(out.operations.size, 2)
        Assertions.assertEquals(out.operations.first()::class, Gaussian::class)
        Assertions.assertEquals(out.operations.last()::class, Gaussian::class)
    }

    @Test
    fun testThree() = runBlocking {
        val node1 = GaussFilter("1")
        val node2 = GaussFilter("2")
        val node3 = GaussFilter("3")
        val node4 = GaussFilter("4")

        val flow = Flow("f1").apply {
            addInput(0, Image::class)
            addOutput(0, Image::class)

            addNode(0, node1)
            addNode(1, node2)
            addNode(2, node3)
            addNode(3, node4)

            connect(0, 0, 1, 0)
            connect(1, 0, 2, 0)
            connect(2, 0, 3, 0)

            connectFlowInput(0, node(0), 0)
//            connectFlowOutput(0, node(2), 0)
            connectFlowOutput(0, node(3), 0)
        }

        repeat(5) {
            flow.sendArg(0, Image(300, 300, Image.Type.PNG))
        }

        val images = mutableListOf<Image>()

        val time = measureTimeMillis {
            flow.tryRun(this)
            repeat(5) {
                images.add(flow.receiveArg(0))
            }
        }
        println(images.joinToString(separator = "\n") {
            it.toString()
        })
        println(time)
        val delay = FilterNode.DELAY
        Assertions.assertTrue(time < 10 * delay)
    }
}