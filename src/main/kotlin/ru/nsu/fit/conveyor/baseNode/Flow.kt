package ru.nsu.fit.conveyor.baseNode

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import java.util.*


class DataQueue : Queue<Any> by LinkedList() {
    // TODO всякие удобюные методы с приведением типа

    // FIXME rename
    inline fun <reified T: Any> pollAs() : T {
        return poll() as T
    }
}

data class Connection(
    val from: BaseNode,
    val outputId: Int,
    val to: BaseNode,
    val inputId: Int
) {
    val data = DataQueue()
}

class Flow(description: String) : BaseNode(description) {
    private val nodes = mutableMapOf<String, Node>()
    private val connections: MutableList<Connection> = mutableListOf()

    fun addNode(name: String, node: Node): Node = node.copy().also {
        nodes[name] = it
    }

    fun connect(from: String, outputId: Int, to: String, inputId: Int) {
        val fromNode = nodes.getValue(from)
        val toNode = nodes.getValue(to)
        connect(fromNode, outputId, toNode, inputId)
    }

    private fun connect(fromNode: BaseNode, outputId: Int, toNode: BaseNode, inputId: Int) {
        if (fromNode.getOutputType(outputId) != toNode.getInputType(inputId)) {
            throw IllegalStateException("Different channels' types")
        }

        val connection = Connection(fromNode, outputId, toNode, inputId)
        if (connection in connections) {
            throw IllegalStateException("Such connection already exists")
        }
        //FIXME надо кидать исключение или удалять старое и добавлять новое?
        if (connections.find { it.from == fromNode && it.outputId == outputId } != null) {
            throw IllegalStateException("")
        }
        connections.add(connection)

    }

    fun Node.flowInput(flowInputId: Int, nodeInputId: Int): Node {
        connect(this@Flow, flowInputId, this, nodeInputId)
        return this
    }

    fun Node.flowOutput(nodeOutputId: Int, flowOutputId: Int): Node {
        connect(this, nodeOutputId, this@Flow, flowOutputId)
        return this
    }

    private fun runNode(node: Node) {
        TODO()
    }

    override suspend fun run(inputs: DataById): DataById {
        // Вносим данные в инпуты Flow
        inputs.forEach { (id, data) ->
            connections.find { it.from == this && it.outputId == id }
                ?.data?.addAll(data) ?: error("No input specified with id=$id")
        }
        // 1. Проверить какие ноды готовы к запуску(все инпуты не пустые) и запустить их
        // 2. Сидеть и ждать когда сработает колбек по завершении какой-либо ноды
        // 3. Полученные данные положить в connection
        // 4. Начать с 1.


        TODO()
    }
}

