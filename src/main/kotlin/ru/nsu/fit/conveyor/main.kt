package ru.nsu.fit.conveyor

//import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import ru.nsu.fit.conveyor.commonNodes.img.ConstantNode
import ru.nsu.fit.conveyor.commonNodes.img.Gaussian
import ru.nsu.fit.conveyor.commonNodes.img.Image
import ru.nsu.fit.conveyor.node.BaseNode
import ru.nsu.fit.conveyor.node.Flow

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")


@ExperimentalCoroutinesApi
fun main() = runBlocking {
    //    val word = regexToken("\\w+")
//    val cm = literalToken(",")
//    val ws = regexToken("\\s+", ignore = true)
//    val tokenizer = DefaultTokenizer(listOf(word, cm, ws))
//    val tokenMatches/*: Sequence<TokenMatch> */= tokenizer.tokenize("hello, world")
//    val result = word.tryParse(tokenMatches, 0)

//    val a = regexToken("a+")
//    val b = regexToken("b+")
//    val tokenMatches = DefaultTokenizer(listOf(a, b)).tokenize("aabbaaa")
//    val aresult = a.tryParse(tokenMatches, 0)
//    val bresult = b.tryParse(tokenMatches, 1)
//    println(aresult)
//    println(bresult)
    /*val tokenMatches = DefaultTokenizer(listOf(a, b)).tokenize("aabbaaa")
    val aba = a and b and a
    val abatext = a and b and a map {it.t1.text + " " + it.t2.text + " " + it.t3.text }
    val abaresult = aba.tryParse(tokenMatches, 0)
    val abatextresult = abatext.tryParse(tokenMatches, 0)
    println(abatextresult)*/
//    val id = regexToken("\\w+")
//    val aText = a map{ it.text } // Parser<String>, returns the matched text from the input sequence
//    val atextresult = aText.tryParse(tokenMatches, 0)
//    println(atextresult)


    val name = ConstantNode::class.simpleName
    val qname = ConstantNode::class.qualifiedName
    val class_ = Class.forName(qname)
    println(name)
    println(qname)
    println(class_.name)
    val cn = class_.newInstance()
//    val cn = ConstantNode::class.java.newInstance()
    println(cn)
//    var ct = ConstantNode.javaClass.getDeclaredConstructor()
//    var ct = ConstantNode::class.java.getDeclaredConstructor()
//    val inst = ct.newInstance()
//    println(inst)


    /*val expr = "purpurpur " + System.lineSeparator() +
            "0 :ru.nsu.fit.conveyor.commonNodes.img.GaussFilter       " + System.lineSeparator() +
            "0 : FLOW/0 "+ System.lineSeparator() +
            "1: FLOW/     1 " + System.lineSeparator() +
            "1 :ru.nsu.fit.conveyor.commonNodes.img.GaussFilter" + System.lineSeparator() +
            "0 : 0/0 "+
            "1: 0/1" + System.lineSeparator() +
            "FLOW" + System.lineSeparator() +
            "0 : 1 /0 " + System.lineSeparator() +
            "FLOW_INPUT_DATA" + System.lineSeparator() +
            "0 : ru.nsu.fit.conveyor.commonNodes.img.Image" + System.lineSeparator() +
            "FLOW_OUTPUT_DATA" + System.lineSeparator() +
            "0 : ru.nsu.fit.conveyor.commonNodes.img.Image"
*/

    val expr = """
      simple_conveyor
      0 : ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
      0 : FLOW /   0
    """.trimIndent()

    val fileParser = FileParser()
    val res = fileParser.parseToEnd(expr)
//    val num = regexToken("\\d+")
//    val tokenMatches = DefaultTokenizer(listOf(num)).tokenize("0")
//    val res = (num map {it.text}).tryParse(tokenMatches, 0)
//    println(res)
    println("flow name")
    println(fileParser.flowName)
    println("nodes classes")
    println(fileParser.nodesClasses)
    println("nodes inputs")
    println(fileParser.nodesInputs)
    println("flow outputs")
    println(fileParser.flowOutputs)
    println("flow input data")
    println(fileParser.flowInputData)
    println("flow output data")
    println(fileParser.flowOutputData)
    val flow = Flow(fileParser.flowName).apply {
        fileParser.nodesClasses.forEach { (id, clazz) ->
            addNode(id, clazz.newInstance() as BaseNode)
        }

        fileParser.nodesInputs.forEach { (id, nodeChannels) ->
            nodeChannels.forEach{ nc ->
                if (nc.otherNodeId != -1)
                    connect(id, nc.channelId, nc.otherNodeId, nc.otherChannelId)
                else
                    connectFlowInput(nc.otherChannelId, node(id), nc.channelId)
            }
        }

        fileParser.flowOutputs.forEach { nc ->
            connectFlowOutput(nc.channelId, node(nc.otherNodeId), nc.otherChannelId)
        }

        fileParser.flowInputData.forEach { (id, type) ->
            addInput(id, type.kotlin)
        }

        fileParser.flowOutputData.forEach { (id, type) ->
            addOutput(id, type.kotlin)
        }
    }

    val width = 300
    val height = 300
    val type = Image.Type.PNG

    val image = Image(width, height, type)
    flow.sendArg(0, image)
    flow.tryRun(this)

    val out = flow.receiveArg(0) as Image
    println(out.width == width)
    println(out.height == height)
    println(out.type == type)
    println(out.operations.size == 2)
    println(out.operations.first()::class == Gaussian::class)
    println(out.operations.last()::class == Gaussian::class)
}