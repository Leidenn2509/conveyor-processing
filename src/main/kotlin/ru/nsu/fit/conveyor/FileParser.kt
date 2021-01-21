package ru.nsu.fit.conveyor

import com.github.h0tk3y.betterParse.combinators.*
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import ru.nsu.fit.conveyor.baseNode.Node
import kotlin.reflect.KClass

class FileParser : Grammar<String>() {
    val flow by literalToken("FLOW")
    //    val flowInputs by literalToken("FLOW_INPUTS")
//    val flowOutputs by literalToken("FLOW_OUTPUTS")
//    val colon by regexToken(" *: *")
    val colon by regexToken("\\s*:\\s*")
    val slash by regexToken("\\s*/\\s*")
    val ws by regexToken("\\s+", ignore = true)
    val id by regexToken("\\d+")
    val nodeId by id or flow
    val word by regexToken("\\w+")


    val flowNameParser by word map {
        flowName = it.text
        it.text}
    val nodeIdClassParser by id and colon and word map
            {it.t1.text + it.t2.text + it.t3.text}
    val nodeInputParser by id and colon and nodeId and slash and id map
            {it.t1.text + it.t2.text + it.t3.text + it.t4.text + it.t5.text}
    val nodeParser by nodeIdClassParser and skip(ws) and oneOrMore(nodeInputParser) map
            { it.t1 + it.t2}

    val flowParser by flowNameParser and skip(ws) and oneOrMore(nodeParser) map
            { it.t1 + it.t2}

    var flowName = "defaultflownamellalallalal"
    var nodeclasses = mutableListOf<>()

    override val rootParser: Parser<String> = flowParser
}