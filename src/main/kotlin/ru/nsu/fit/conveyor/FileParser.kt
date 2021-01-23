package ru.nsu.fit.conveyor

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.MismatchedToken
import com.github.h0tk3y.betterParse.parser.ParseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import ru.nsu.fit.conveyor.node.BaseNode
import ru.nsu.fit.conveyor.node.Flow
import java.io.File

@ExperimentalCoroutinesApi
class FileParser() {
    private inner class FlowParser : Grammar<Any>() {
        var flowName = ""

        //nodeId could be FLOW
        // nodeId->nodeClass
        var nodesClasses = mutableMapOf<Int, Class<*>>()

        // nodeId-> <inputid, nodeInput(outnodeid, outputid)>
        var nodesInputs = mutableMapOf<Int, List<NodeChannel>>()

        // flowoutputid, nodeid, nodeoutputid
        var flowOutputs = mutableListOf<NodeChannel>()

        //to, what
        var flowInputData = mutableMapOf<Int, Class<*>>()
        var flowOutputData = mutableMapOf<Int, Class<*>>()


        val FLOW_INPUT = "FLOW_INPUT_DATA"
        val FLOW_OUTPUT = "FLOW_OUTPUT_DATA"
        val FLOW = "FLOW"


        val flowInput by literalToken(FLOW_INPUT)
        val flowOutput by literalToken(FLOW_OUTPUT)
        val flow by literalToken(FLOW)

        val colon by regexToken("\\s*:\\s*")
        val arrow by regexToken("\\s*->\\s*")
        val slash by regexToken("\\s*/\\s*")

        //    val point by literalToken(".?")
        val ws by regexToken("\\s+", ignore = true)
        val id by regexToken("\\d+")
        val nodeId by id or flow

        val classname by regexToken("(\\w+\\.?\\$?)+")
//    val word by regexToken("\\w+")


        val flowNameParser by classname map {
            flowName = it.text
            it.text
        }
        val nodeIdClassParser by id and skip(colon) and classname map { (id, classname) ->
            val nodeClass = Class.forName(classname.text)
            nodesClasses.put(id.text.toInt(), nodeClass)
            id.text.toInt()
        }

        val nodeChannelParser by id and skip(arrow) and nodeId and skip(slash) and id map
                { (inputChannelId, outputNodeId, outputChannelId) ->
                    val idStr = outputNodeId.text
                    var id = -1
                    if (idStr != FLOW)
                        id = idStr.toInt()
                    NodeChannel(
                        inputChannelId.text.toInt(),
                        id,
                        outputChannelId.text.toInt()
                    )
                }

        val nodeParser by nodeIdClassParser and skip(ws) and oneOrMore(nodeChannelParser) map { (nodeId, nodeChannels) ->
            nodesInputs.put(nodeId, nodeChannels)
        }

        val flowOutputsParser by skip(flow) and skip(ws) and oneOrMore(nodeChannelParser) map {
            flowOutputs.addAll(it)
        }

        //TODO можно проскипать вообще все символы же типа двоеточия и запятых и слешей ы

        val data by id and skip(colon) and classname map { (channelId, type) ->
            Pair<Int, Class<*>>(channelId.text.toInt(), Class.forName(type.text))
        }

        val flowInputDataParser by skip(flowInput) and skip(ws) and oneOrMore(data) map { datas ->
            datas.forEach { data ->
                flowInputData.put(data.first, data.second)
            }
        }

        val flowOutputDataParser by skip(flowOutput) and skip(ws) and oneOrMore(data) map { datas ->
//            try {
                datas.forEach { data ->
                    flowOutputData.put(data.first, data.second)
                }
//            } catch (e : ParseException) {
//                throw Exception("Error parsing flow output data")
//            }
        }

        val flowParser by flowNameParser and skip(ws) and oneOrMore(nodeParser) and
                skip(ws) and flowOutputsParser and flowInputDataParser and flowOutputDataParser

        override val rootParser = flowParser
    }

    private var flowParser = FlowParser()

    private fun createFlow() : Flow {
        val flow = Flow(flowParser.flowName).apply {
            flowParser.flowInputData.forEach { (id, type) ->
                addInput(id, type.kotlin)
            }

            flowParser.flowOutputData.forEach { (id, type) ->
                addOutput(id, type.kotlin)
            }

            flowParser.nodesClasses.forEach { (id, clazz) ->
                addNode(id, clazz.newInstance() as BaseNode)
            }

            flowParser.nodesInputs.forEach { (nodeId, nodeChannels) ->
                node(nodeId)?.let {
                    nodeChannels.forEach { nc ->
                        node(nc.otherNodeId)?.let {
                            if (nc.otherNodeId != -1)
                                connect(nc.otherNodeId, nc.otherChannelId, nodeId, nc.channelId)
                            else
                                node(nodeId)?.let { connectFlowInput(nc.otherChannelId, it, nc.channelId) }
                        } ?: error("Trying to connect to not existing node with id ${nc.otherNodeId}")
                    }
                } ?: error("Trying to connect not existing node with id $nodeId")
            }

            flowParser.flowOutputs.forEach { nc ->
                node(nc.otherNodeId)?.let { connectFlowOutput(nc.channelId, it, nc.otherChannelId) }
                    ?: error("Trying to get output from not existing node with id ${nc.otherNodeId}")
            }
        }
        return flow
    }

    fun parseFlow(expr: String): Flow {
        flowParser.parseToEnd(expr)
        return createFlow()
    }

    fun parseFile(filename : String) : Flow {
        val expr = File(filename).readText()
        return parseFlow(expr)
    }
}


data class NodeChannel(
    val channelId: Int,
    val otherNodeId: Int,
    val otherChannelId: Int
)