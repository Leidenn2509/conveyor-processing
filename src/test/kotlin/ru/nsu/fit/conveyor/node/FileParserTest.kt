package ru.nsu.fit.conveyor.node

import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.parser.ParseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.nsu.fit.conveyor.FileParser
import ru.nsu.fit.conveyor.commonNodes.img.*
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
class FileParserTest {

    @Test
    fun testSimpleFlow() = runBlocking {
        val filename = "src/conveyor_configurations/simple.txt"
        val fileParser = FileParser()
        val flow = fileParser.parseFile(filename)

        val width = 300
        val height = 300
        val type = Image.Type.PNG

        val image = Image(width, height, type)
        flow.sendArg(0, image)

        flow.tryRun(this)

        val out = flow.receiveArg(0) as Image
        Assertions.assertEquals(width, out.width)
        Assertions.assertEquals(height, out.height)
        Assertions.assertEquals(type, out.type)
        Assertions.assertEquals(1, out.operations.size)
        Assertions.assertEquals(Gaussian::class, out.operations.first()::class)
    }

    @Test
    fun testFlow() = runBlocking {
        val filename = "src/conveyor_configurations/base.txt"
        val fileParser = FileParser()
        val flow = fileParser.parseFile(filename)

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
        Assertions.assertEquals(Gaussian::class, out.operations.first()::class)
        Assertions.assertEquals(Gaussian::class, out.operations.last()::class)
    }

    @Test
    fun testSlicingFlow() = runBlocking {
        val filename = "src/conveyor_configurations/slicing.txt"
        val fileParser = FileParser()
        val flow = fileParser.parseFile(filename)


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
            Assertions.assertEquals(width / 2, it.width)
            Assertions.assertEquals(height, it.height)
            Assertions.assertEquals(type, it.type)
            Assertions.assertEquals(2, it.operations.size)
            Assertions.assertEquals(Slicing::class, it.operations.first()::class)
            Assertions.assertEquals(Gaussian::class, it.operations.last()::class)
        }
    }

    @Test
    fun testThree() = runBlocking {
        val filename = "src/conveyor_configurations/three.txt"
        val fileParser = FileParser()
        val flow = fileParser.parseFile(filename)


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

    @Test
    fun testSimpleWithSyntaxErrorsFlow() : Unit = runBlocking {
        val text = """
            simple
            0: ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
            0: FLOW/0
            FLOW
            0: 0/    0
            FLOW_INPUT_DATA
            0: ru.nsu.fit.conveyor.commonNodes.img.Image
            FLOW_OUTPUT_DATAcnjkdscnkjdsnc
            0  : ru.nsu.fit.conveyor.commonNodes.img.Image
        """.trimIndent()
        Assertions.assertThrows(ParseException::class.java) {
            FileParser().parseFlow(text)
        }
    }
}