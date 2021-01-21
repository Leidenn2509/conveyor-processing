package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.nsu.fit.conveyor.commonNodes.img.FilterNode
import ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
import ru.nsu.fit.conveyor.commonNodes.img.Image
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
class OneInstanceTest {
    @Test
    fun testOneInstance() = runBlocking {
        val node1 = GaussFilter("1")
        val node2 = GaussFilter("2")
        val node3 = GaussFilter("3")

        val flow1 = Flow("f1").apply {
            addInput(0, Image::class)
            addOutput(0, Image::class)

            addNode(0, node1)
            addNode(1, node2)
            addNode(2, node3)

            connect(0, 0, 1, 0)
            connect(1, 0, 2, 0)

            connectFlowInput(0, node(0), 0)
            connectFlowOutput(0, node(2), 0)
        }

        val flow2 = Flow("f2").apply {
            addInput(0, Image::class)
            addOutput(0, Image::class)

            addNode(0, node1)
            addNode(1, node2)
            addNode(2, node3)

            connect(0, 0, 1, 0)
            connect(1, 0, 2, 0)

            connectFlowInput(0, node(0), 0)
            connectFlowOutput(0, node(2), 0)
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
}