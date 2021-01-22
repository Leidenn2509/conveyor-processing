package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.nsu.fit.conveyor.commonNodes.img.*
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

@ExperimentalCoroutinesApi
class NodeTest {
    @Test
    fun testWrongType(): Unit = runBlocking {
        val node1 = ConstantNode()
        val node2 = GaussFilter()
        Assertions.assertThrows(IllegalStateException::class.java) {
            node1.connectOutputTo(0, node2, 0)
        }
    }

    @Test
    fun testConstantNode() = runBlocking {
        val node = ConstantNode()
        node.sendArg(0, 3)
        node.sendArg(1, 10)
        node.tryRun(this)
        repeat(10) {
            Assertions.assertTrue(node.receiveArg(0) as Int == 3)
        }
    }

    @Test
    fun testFilterNode() = runBlocking {
        val node = GaussFilter()
        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image = Image(width, height, type)

        node.sendArg(0, image)
        node.tryRun(this)

        val out = node.receiveArg(0) as Image
        Assertions.assertEquals(out.width, width)
        Assertions.assertEquals(out.height, height)
        Assertions.assertEquals(out.type, type)
        Assertions.assertEquals(out.operations.size, 1)
        Assertions.assertEquals(out.operations.first()::class, Gaussian::class)
    }

    @Test
    fun testTwoNodes() = runBlocking {
        val node = GaussFilter()
        val node2 = ChangeTypeToJPG()
        node.connectOutputTo(0, node2, 0)

        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image = Image(width, height, type)
        node.sendArg(0, image)
        node.tryRun(this)

        val out = node2.receiveArg(0) as Image
        Assertions.assertEquals(out.width, width)
        Assertions.assertEquals(out.height, height)
        Assertions.assertEquals(out.type, Image.Type.JPG)
        Assertions.assertEquals(out.operations.size, 2)
        Assertions.assertEquals(out.operations[0]::class, Gaussian::class)
        Assertions.assertEquals(out.operations[1], ChangeType(Image.Type.PNG, Image.Type.JPG))
    }

    @Test
    fun testPanoramaNode() = runBlocking {
        val node = PanoramaNode()

        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image1 = Image(width, height, type)
        val image2 = Image(2 * width, 2 * height, type)
        node.sendArg(1, 2)
        node.sendArg(0, image1)
        node.sendArg(0, image2)
        node.tryRun(this)

        val out = node.receiveArg(0) as Image
        Assertions.assertEquals(out.width, 3 * width)
        Assertions.assertEquals(out.height, 2 * height)
        Assertions.assertEquals(out.type, type)
        Assertions.assertEquals(out.operations.size, 1)
        Assertions.assertEquals(out.operations[0]::class, Panorama::class)
    }

    @Test
    fun testSlicingNode() = runBlocking {
        val node = SlicingNode()
        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image = Image(width, height, type)
        node.sendArg(0, image)
        node.sendArg(1, 3)
        node.tryRun(this)

        val images = mutableListOf<Image>()
        repeat(3) {
            images.add(node.receiveArg(0) as Image)
        }

        Assertions.assertTrue(images.all { it.height == height })
        Assertions.assertTrue(images.all { it.width == width / 3 })
        Assertions.assertTrue(images.all { it.operations.size == 1 })
        Assertions.assertTrue(images.all { it.operations.first()::class == Slicing::class })
    }

    @Test
    fun testChooseNode() = runBlocking {
        val node = ChooseNodeFromN(3)
        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image1 = Image(width, height, type)
        val image2 = Image(2 * width, 2 * height, type)
        val image3 = Image((1.5 * width).toInt(), (1.5 * height).toInt(), type)

        node.sendArg(0, image1)
        node.sendArg(1, image2)
        node.sendArg(2, image3)
        node.tryRun(this)

        val res = node.receiveArg<Image>(0)
        Assertions.assertTrue(
            res == image1 || res == image2 || res == image3
        )
    }

    @Test
    fun testTime() = runBlocking {
        val node1 = GaussFilter()
        val node2 = GaussFilter()
        val node3 = GaussFilter()

        node1.connectOutputTo(0, node2, 0)
        node2.connectOutputTo(0, node3, 0)

        repeat(5) {
            node1.sendArg(0, Image(300, 300, Image.Type.PNG))
        }
        val images = mutableListOf<Image>()
        val time = measureTimeMillis {
            node1.tryRun(this)
            repeat(5) {
                images.add(node3.receiveArg<Image>(0))
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