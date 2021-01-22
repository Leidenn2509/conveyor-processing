package ru.nsu.fit.conveyor

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken

class FileParser() : Grammar<Any>() {
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

    val nodeChannelParser by id and colon and nodeId and slash and id map
            { (inputChannelId, _, outputNodeId, _, outputChannelId) ->
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

    val data by id and skip(colon) and classname map {(channelId, type) ->
        Pair<Int, Class<*>>(channelId.text.toInt(), Class.forName(type.text))
    }

    val flowInputDataParser by skip(flowInput) and skip(ws) and oneOrMore(data) map { datas ->
        datas.forEach { data ->
            flowInputData.put(data.first, data.second)
        }
    }

    val flowOutputDataParser by skip(flowOutput) and skip(ws) and oneOrMore(data) map { datas ->
        datas.forEach { data ->
            flowOutputData.put(data.first, data.second)
        }
    }

    val flowParser by flowNameParser and skip(ws) and oneOrMore(nodeParser) and
            skip(ws) and flowOutputsParser and flowInputDataParser and flowOutputDataParser

    override val rootParser = flowParser

}

data class NodeChannel(
    val channelId: Int,
    val otherNodeId: Int,
    val otherChannelId: Int
)