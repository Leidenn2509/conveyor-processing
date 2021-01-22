package ru.nsu.fit.conveyor

//import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import ru.nsu.fit.conveyor.commonNodes.img.ConstantNode
import ru.nsu.fit.conveyor.commonNodes.img.Gaussian
import ru.nsu.fit.conveyor.commonNodes.img.Image
import ru.nsu.fit.conveyor.commonNodes.img.SlicingNode
import ru.nsu.fit.conveyor.node.BaseNode
import ru.nsu.fit.conveyor.node.Flow

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")


@ExperimentalCoroutinesApi
fun main() = runBlocking {
  /*  val name = ConstantNode::class.simpleName
    val qname = ConstantNode::class.qualifiedName
    val class_ = Class.forName(qname)
    println(name)
    println(qname)
    println(class_.name)
    val cn = class_.newInstance()
    println(cn)*/
//    var ct = ConstantNode.javaClass.getDeclaredConstructor()
//    var ct = ConstantNode::class.java.getDeclaredConstructor()
//    val inst = ct.newInstance()
//    println(inst)


  /*  val expr = """
        purpurpur
        0 :ru.nsu.fit.conveyor.commonNodes.img.SlicingNode       
            0 : FLOW/0  
            1        :  FLOW/     1
        1 :ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
            0 : 0/0 
        FLOW
            0 : 1 /0 
        FLOW_INPUT_DATA
            0 : ru.nsu.fit.conveyor.commonNodes.img.Image
            1 : java.lang.Integer
        FLOW_OUTPUT_DATA
            0 : ru.nsu.fit.conveyor.commonNodes.img.Image
        
    """.trimIndent()
    *//*val expr = """
      simple_conveyor
      0 : ru.nsu.fit.conveyor.commonNodes.img.GaussFilter
        0 : FLOW /   0
      FLOW
        0 : 0/0
      FLOW_INPUT_DATA
        0 : ru.nsu.fit.conveyor.commonNodes.img.Image
      FLOW_OUTPUT_DATA
        0 : ru.nsu.fit.conveyor.commonNodes.img.Image
    """.trimIndent()
*//*
    val fileParser = FileParser()
    val res = fileParser.parseToEnd(expr)
//    val num = regexToken("\\d+")
//    val tokenMatches = DefaultTokenizer(listOf(num)).tokenize("0")
//    val res = (num map {it.text}).tryParse(tokenMatches, 0)
//    println(res)
    println("flow name:\n\t${fileParser.flowName}")
    println("nodes classes:\n\t${fileParser.nodesClasses}")
    println("nodes inputs:\n\t${fileParser.nodesInputs}")
    println("flow outputs:\n\t${fileParser.flowOutputs}")
    println("flow input data:\n\t${fileParser.flowInputData}")
    println("flow output data:\n\t${fileParser.flowOutputData}")
    val flow = Flow(fileParser.flowName).apply {
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
    }

    val width = 300
    val height = 300
    val type = Image.Type.PNG

    val image = Image(width, height, type)
    flow.sendArg(0, image)
//    flow.sendArg(1, 2)
    flow.tryRun(this)

    val out = flow.receiveArg(0) as Image
   *//* println(out.width == width)
    println(out.height == height)
    println(out.type == type)
    println(out.operations.size == 1)
    println(out.operations.first()::class == Gaussian::class)
    println(out.operations.last()::class == Gaussian::class)*//*

    println(out.width == width/2)
    println(out.height == height)
    println(out.type == type)
    println(out.operations.size == 2)
    println(out.operations.first()::class == SlicingNode::class)
    println(out.operations.last()::class == Gaussian::class)
*/
}