package ru.nsu.fit.conveyor

import kotlinx.coroutines.runBlocking
import ru.nsu.fit.conveyor.baseNode.DataQueue
import ru.nsu.fit.conveyor.baseNode.Flow
import ru.nsu.fit.conveyor.baseNode.Node

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

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
        addInput(0, Double::class.java)
        addOutput(0, Double::class.java)
        body = { map ->
            @Suppress("UNCHECKED_CAST")
            val a = map.getValue(0) as List<Double>
            mapOf(0 to a.map { it * it })
        }
    }

    val dividerNode = Node("2 to 3").apply {
        addInput(0, Double::class.java)
        addOutput(0, Double::class.java)
        addOutput(1, Double::class.java)
        body = { map ->
            val a = map.getValue(0).map { (it as Double) / 5.0 }
            mapOf(
                0 to a.map { 2.0 * it },
                1 to a.map { 3.0 * it }
            )
        }
    }

    val subNode = Node("-3").apply {
        addInput(0, Double::class.java)
        addOutput(0, Double::class.java)
        body = { map ->
            val a = map.getValue(0).map { (it as Double) - 3.0 }
            mapOf(
                0 to a
            )
        }
    }

    val sumNode = Node("+").apply {
        addInput(0, Double::class.java)
        addInput(1, Double::class.java)
        addOutput(0, Double::class.java)
        body = { map ->
            val a = map.getValue(0).map { it as Double }
            val b = map.getValue(1).map { it as Double }
            mapOf(
                0 to a.zip(b).map { it.first + it.second }
            )
        }
    }

//    println(powNode.run(mapOf(0 to listOf(8))))

    val flow = Flow("pow twice").apply {
        addInput(0, Double::class.java)
        addOutput(0, Double::class.java)

        addNode("pow1", powNode).flowInput(0, 0)
        addNode("pow2", powNode)
        addNode("pow3", powNode).flowOutput(0, 0)
        connect("pow1", 0, "pow2", 0)
        connect("pow2", 0, "pow3", 0)
    }
//    println(flow.run(mapOf(0 to listOf(1, 2, 3))))

    val complexFlow = Flow("hard math").apply {
        addInput(0, Double::class.java)
        addOutput(0, Double::class.java)

        addNode("pow3", flow).flowInput(0, 0)

        addNode("divider", dividerNode)
//        addNode("divider", dividerNode).flowInput(0, 0)
        addNode("sub", subNode)
        addNode("pow", powNode)
        addNode("sum", sumNode).flowOutput(0, 0)

        connect("pow3", 0, "divider", 0)

        connect("divider", 0, "sub", 0)
        connect("divider", 1, "pow", 0)
        connect("sub", 0, "sum", 0)
        connect("pow", 0, "sum", 1)
    }
    println(complexFlow.run(mapOf(0 to listOf(5.0, 10.0, 3.0))))
}