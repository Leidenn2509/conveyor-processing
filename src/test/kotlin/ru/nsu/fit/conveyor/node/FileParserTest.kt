package ru.nsu.fit.conveyor.node

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.nsu.fit.conveyor.FileParser
import ru.nsu.fit.conveyor.commonNodes.img.*
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
class FileParserTest {
//    private var fileParser = FileParser()

    /*private fun parse(text: String): Flow {
        val fileParser = FileParser().apply {
            parseFlow(text)
        }

        return Flow(fileParser.flowName).apply {
            fileParser.flowInputData.forEach { (id, type) ->
                addInput(id, type.kotlin)
            }

            fileParser.flowOutputData.forEach { (id, type) ->
                addOutput(id, type.kotlin)
            }

            fileParser.nodesClasses.forEach { (id, clazz) ->
                addNode(id, clazz.newInstance() as BaseNode)
            }

            fileParser.nodesInputs.forEach { (id, nodeChannels) ->
                nodeChannels.forEach { nc ->
                    if (nc.otherNodeId != -1)
                        connect(id, nc.channelId, nc.otherNodeId, nc.otherChannelId)
                    else
                        connectFlowInput(nc.otherChannelId, node(id), nc.channelId)
                }
            }

            fileParser.flowOutputs.forEach { nc ->
                connectFlowOutput(nc.channelId, node(nc.otherNodeId), nc.otherChannelId)
            }
        }
    }
*/

    private fun parse(text: String): Flow {
        val fileParser = FileParser()
        return fileParser.parseFlow(text)
    }

    @Test
    fun testSimpleFlow() = runBlocking {
        val text = """
            simple
                0: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
                    0: FLOW/0
                FLOW
                    0: 0/0
                FLOW_INPUT_DATA
                    0: ru.nsu.fit.conveyor.commonNodes.img.Image
                FLOW_OUTPUT_DATA
                    0: ru.nsu.fit.conveyor.commonNodes.img.Image
        """.trimIndent()
        val flow = parse(text)

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
        val text = """
            simple
                0: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
                    0: FLOW/0
                1: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
                    0: 0/0
                FLOW
                    0: 1/0
                FLOW_INPUT_DATA
                    0: ru.nsu.fit.conveyor.commonNodes.img.Image
                FLOW_OUTPUT_DATA
                    0: ru.nsu.fit.conveyor.commonNodes.img.Image
        """.trimIndent()
        val flow = parse(text)

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
        Assertions.assertEquals(2, out.operations.size)
        Assertions.assertEquals(out.operations.first()::class, Gaussian::class)
        Assertions.assertEquals(out.operations.last()::class, Gaussian::class)
    }

    @Test
    fun testSlicingFlow() = runBlocking {
        val text = """
            slicing
                0: ru.nsu.fit.conveyor.commonNodes.img.SlicingNode
                    0: FLOW/0
                    1: FLOW/1
                1: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
                    0: 0/0
                FLOW
                    0: 1/0
                FLOW_INPUT_DATA
                    0: ru.nsu.fit.conveyor.commonNodes.img.Image
                    1: java.lang.Integer
                FLOW_OUTPUT_DATA
                    0: ru.nsu.fit.conveyor.commonNodes.img.Image
        """.trimIndent()
        val flow = parse(text)

        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image = Image(width, height, type)
        flow.sendArg(0, image)
        flow.sendArg(1, 2)

        flow.tryRun(this)

        val out = mutableListOf<Image>()

        repeat(2) {
            out.add(flow.receiveArg(0) as Image)
        }

        Assertions.assertEquals(out.size, 2)

        out.forEach {
            Assertions.assertEquals(it.width, width / 2)
            Assertions.assertEquals(it.height, height)
            Assertions.assertEquals(it.type, type)
            Assertions.assertEquals(it.operations.size, 2)
            Assertions.assertEquals(it.operations.first()::class, Slicing::class)
            Assertions.assertEquals(it.operations.last()::class, Gaussian::class)
        }
    }

    @Test
    fun testThree() = runBlocking {
        val text = """
            simple
                0: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
                    0: FLOW/0
                1: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
                    0: 0/0
                2: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
                    0: 1/0
                3: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
                    0: 2/0
                FLOW
                    0: 3/0
                FLOW_INPUT_DATA
                    0: ru.nsu.fit.conveyor.commonNodes.img.Image
                FLOW_OUTPUT_DATA
                    0: ru.nsu.fit.conveyor.commonNodes.img.Image
        """.trimIndent()

        val flow = parse(text)

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