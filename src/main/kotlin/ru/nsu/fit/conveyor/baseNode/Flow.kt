package ru.nsu.fit.conveyor.baseNode

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import java.util.*


class DataQueue : Queue<Any> by LinkedList() {
    // TODO всякие удобные методы с приведением типа

    fun take(n: Int): List<Any> {
        val res = mutableListOf<Any>()
        repeat(n) {
            // FIXME может нужно развернуть?
            res.add(poll())
        }
        return res
    }

    // FIXME rename
    inline fun <reified T : Any> pollAs(): T {
        return poll() as T
    }
}

enum class Status {
    RUN,
    IDLE
}

data class Connection(
    val from: BaseNode,
    val outputId: Int,
    val to: BaseNode,
    val inputId: Int
) {
    val data: DataQueue = DataQueue()

//    override fun equals(other: Any?): Boolean {
//        if (other !is Connection) return false
//        return from == other.from && outputId == other.outputId && to == other.to && inputId == other.inputId
//    }

//    override fun hashCode(): Int {
//        var result = from.hashCode()
//        result = 31 * result + outputId
//        result = 31 * result + to.hashCode()
//        result = 31 * result + inputId
//        return result
//    }
}

class Flow(description: String) : BaseNode(description) {
    private data class NodeWithInfo(
        val node: BaseNode,
        val name: String,
        var status: Status
    )

    private val nodes = mutableListOf<NodeWithInfo>()
    private fun nodeByName(name: String): NodeWithInfo? = nodes.find { it.name == name }
    private fun nodeInfo(node: BaseNode): NodeWithInfo? = nodes.find { it.node == node }


    private val connections: MutableList<Connection> = mutableListOf()

    fun addNode(name: String, node: BaseNode): BaseNode = node.copy().also {
        if (nodeByName(name) != null) error("this name already exist")
        nodes.add(NodeWithInfo(it, name, Status.IDLE))
    }

    fun connect(from: String, outputId: Int, to: String, inputId: Int) {
        val fromNode = nodeByName(from)?.node ?: error("connect")
        val toNode = nodeByName(to)?.node ?: error("connect")
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

    fun BaseNode.flowInput(flowInputId: Int, nodeInputId: Int): BaseNode {
        connect(this@Flow, flowInputId, this, nodeInputId)
        return this
    }

    fun BaseNode.flowOutput(nodeOutputId: Int, flowOutputId: Int): BaseNode {
        connect(this, nodeOutputId, this@Flow, flowOutputId)
        return this
    }

    override suspend fun run(inputs: DataById): DataById {
        // Вносим данные в инпуты Flow
        log("setup inputs")
        inputs.forEach { (id, data) ->
            connections.find { it.from == this && it.outputId == id }
                ?.data?.addAll(data) ?: error("No input specified with id=$id")
        }

        coroutineScope {
            val nodeResults = Channel<Pair<Connection, Any>>()

            while (true) {
                //1. Проверить какие ноды готовы к запуску(все инпуты не пустые) и запустить их
                log("check nodes")
                for (nodeWithInfo in nodes) {
                    if (nodeWithInfo.status == Status.RUN) {
                        continue
                    }
                    val inConnections = nodeWithInfo.node.inConnections()
                    if (inConnections.all { !it.data.isEmpty() }) {
                        val dataReady = inConnections.minOf { it.data.size }

                        runAsync(
                            nodeWithInfo.node,
                            inConnections.map { it.inputId to it.data.take(dataReady) }.toMap(),
                            nodeResults
                        )
                        nodeWithInfo.status = Status.RUN
                    }
                }

                if (nodes.all { it.status == Status.IDLE }) break

                // 2. Сидеть и ждать когда сработает колбёк по завершении какой-либо ноды
                log("wait")
                select<Unit> {
                    nodeResults.onReceive { (connection, data) ->
                        nodes.find { it.node == connection.from }?.status = Status.IDLE
                        val withData = connections.find { it == connection } ?: error("b")
                        withData.data.addAll(data as List<*>)
                    }
                }
            }
        }
        val res = mutableMapOf<Int, List<Any>>()
        connections.filter { it.to == this }.forEach {
            res[it.inputId] = it.data.toList()
        }
        return res
    }

    private fun BaseNode.inConnections() = connections
        .filter { it.to == this }
        .takeIf { it.size == this.inputsCount } ?: error("Not enough connections in node $this")

    private fun CoroutineScope.runAsync(node: BaseNode, inputs: DataById, channel: Channel<Pair<Connection, Any>>) {
        launch {
            log("run ${node.description}")
            val outputData = node.run(inputs)
            outputData.forEach { (id, data) ->
                val connection = connections.find { it.from == node && it.outputId == id } ?: error("a")
                // 3. Полученные данные положить в connection
                channel.send(connection to data)

            }
        }
    }

    private fun log(msg: String) = println("[${Thread.currentThread().name}][${this.description}]$msg")

    override fun copy(): Flow {
        return Flow(description).apply {
            this@Flow.inputTypes.forEach(this::addInput)
            this@Flow.outputTypes.forEach(this::addOutput)
            this@Flow.nodes.forEach {
                addNode(it.name, it.node)
            }
            this@Flow.connections.map { c ->
                val from = if (c.from == this@Flow) {
                    this
                } else {
                    this.nodes.find { it.name == this@Flow.nodeInfo(c.from)!!.name }!!.node
                }
                val to = if (c.to == this@Flow) {
                    this
                } else {
                    this.nodes.find { it.name == this@Flow.nodeInfo(c.to)!!.name }!!.node
                }

                Connection(
                    from,
                    c.outputId,
                    to,
                    c.inputId
                )
            }.forEach(this.connections::add)
        }
    }
}

