package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.nsu.fit.conveyor.commonNodes.img.FilterNode
import ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
import ru.nsu.fit.conveyor.commonNodes.img.Image
import ru.nsu.fit.conveyor.commonNodes.img.SlicingNode
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
class OneInstanceTest {
    @Test
    fun testOneInstance() = runBlocking {
        val node1 = GaussFilter()
        val node2 = GaussFilter()
        val node3 = GaussFilter()

        val flow1 = Flow("f1").apply {
            addInput(0, Image::class)
            addOutput(0, Image::class)

            addNode(0, node1)
            addNode(1, node2)
            addNode(2, node3)

            connect(0, 0, 1, 0)
            connect(1, 0, 2, 0)

            connectFlowInput(0, node(0)!!, 0)
            connectFlowOutput(0, node(2)!!, 0)
        }

        val flow2 = Flow("f2").apply {
            addInput(0, Image::class)
            addOutput(0, Image::class)

            addNode(0, node1)
            addNode(1, node2)
            addNode(2, node3)

            connect(0, 0, 1, 0)
            connect(1, 0, 2, 0)

            connectFlowInput(0, node(0)!!, 0)
            connectFlowOutput(0, node(2)!!, 0)
        }


        repeat(5) {
            flow1.sendArg(0, Image(300, 300, Image.Type.PNG))
            flow2.sendArg(0, Image(300, 300, Image.Type.PNG))
        }

        val images1 = async(start = CoroutineStart.LAZY) {
            val images = mutableListOf<Image>()
            flow1.tryRun(this)
            repeat(5) {
                images.add(flow1.receiveArg(0))
            }
            images
        }
        val images2 = async(start = CoroutineStart.LAZY) {
            val images = mutableListOf<Image>()
            flow2.tryRun(this)
            repeat(5) {
                images.add(flow2.receiveArg(0))
            }
            images
        }


        val s1: String
        val s2: String
        val time = measureTimeMillis {

            images1.start()
            images2.start()

            images1.await()
            images2.await()

            s1 = images1.await().joinToString(separator = "\n") {
                it.toString()
            }
            s2 = images2.await().joinToString(separator = "\n") {
                it.toString()
            }
        }
        println(s1)
        println(s2)
        println(time)
        val delay = FilterNode.DELAY
        Assertions.assertTrue(time < 10 * delay)
    }

    @Test
    fun testSlicingNode() = runBlocking {
        val slicingNode = SlicingNode()

        val flow1 = Flow("1").apply {
            addInput(0, Image::class)
            addInput(1, Int::class)
            addOutput(0, Image::class)

            addNode(0, slicingNode)

            connectFlowInput(0, node(0)!!, 0)
            connectFlowInput(1, node(0)!!, 1)
            connectFlowOutput(0, node(0)!!, 0)
        }

        val flow2 = Flow("2").apply {
            addInput(0, Image::class)
            addInput(1, Int::class)
            addOutput(0, Image::class)

            addNode(0, slicingNode)

            connectFlowInput(0, node(0)!!, 0)
            connectFlowInput(1, node(0)!!, 1)
            connectFlowOutput(0, node(0)!!, 0)
        }

        flow1.sendArg(0, Image(300, 300, Image.Type.PNG))
        flow1.sendArg(1, 2)

        flow2.sendArg(0, Image(300, 300, Image.Type.PNG))
        flow2.sendArg(1, 4)

        val images1 = async(start = CoroutineStart.LAZY) {
            val images = mutableListOf<Image>()
            flow1.tryRun(this)
            repeat(2) {
                images.add(flow1.receiveArg(0))
            }
            images
        }
        val images2 = async(start = CoroutineStart.LAZY) {
            val images = mutableListOf<Image>()
            flow2.tryRun(this)
            repeat(4) {
                images.add(flow2.receiveArg(0))
            }
            images
        }

        val s1: String
        val s2: String
        val time = measureTimeMillis {
            images1.start()
            images2.start()

            images1.await()
            images2.await()

            s1 = images1.await().joinToString(separator = "\n") {
                it.toString()
            }
            s2 = images2.await().joinToString(separator = "\n") {
                it.toString()
            }
        }

        println(s1)
        println(s2)
        println(time)
        Assertions.assertTrue(time < 5000)

    }

}