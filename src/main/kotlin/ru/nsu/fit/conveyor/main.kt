package ru.nsu.fit.conveyor

import kotlinx.coroutines.runBlocking
import ru.nsu.fit.conveyor.baseNode.DataQueue
import ru.nsu.fit.conveyor.baseNode.Flow
import ru.nsu.fit.conveyor.baseNode.Node


fun main() = runBlocking {
//    val a = DataQueue()
//    a.add(1)
//    a.add(2)
//    println(a.peek())
//    println(a.size)
//    println(a.take(1))
//    println(a.size)
//    TODO()

    val powNode = Node("simple pow").apply {
        addInput(0, Int::class.java)
        addOutput(0, Int::class.java)
        body = { map ->
            @Suppress("UNCHECKED_CAST")
            val a = map.getValue(0) as List<Int>
            mapOf(0 to a.map { it*it })
        }
    }

//    println(powNode.run(mapOf(0 to listOf(8))))

    val flow = Flow("pow twice").apply {
        addInput(0, Int::class.java)
        addOutput(0, Int::class.java)

        addNode("pow1", powNode).flowInput(0, 0)
        addNode("pow2", powNode)
        addNode("pow3", powNode).flowOutput(0, 0)
        connect("pow1", 0, "pow2", 0)
        connect("pow2", 0, "pow3", 0)
    }
    println(flow.run(mapOf(0 to listOf(1, 2, 3))))
}