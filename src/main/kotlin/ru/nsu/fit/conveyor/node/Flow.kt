package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.select
import java.util.*


class DataQueue : Queue<Any> by LinkedList() {
    // TODO всякие удобные методы с приведением типа

    fun take(n: Int): List<Any> {
        val res = mutableListOf<Any>()
        repeat(n) {
            res.add(poll())
        }
        return res
    }

}

enum class Status {
    RUN,
    IDLE
}

data class NodeInput(
    val node: BaseNode,
    val id: Int
) {
    val dataBuffer = DataQueue()
}

data class NodeOutput(
    val node: BaseNode,
    val id: Int
)

data class Connection(
    val output: NodeOutput,
    val input: NodeInput
)

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

    fun addNode(name: String, node: BaseNode): BaseNode = node.copy().also { copyNode ->
        if (nodeByName(name) != null) error("this name already exist")
        nodes.add(NodeWithInfo(copyNode, name, Status.IDLE))
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

        val nodeOutput = connections
            .find { it.output.node == fromNode && it.output.id == outputId }?.output
            ?: NodeOutput(fromNode, outputId)

        val nodeInput = connections
            .find { it.input.node == toNode && it.input.id == inputId }?.input
            ?: NodeInput(toNode, inputId)


        val connection = Connection(nodeOutput, nodeInput)
        if (connection in connections) {
            throw IllegalStateException("Such connection already exists")
        }
        if (connections.find { it.output == nodeOutput } != null) {
            throw IllegalStateException("Cant split output with id=$outputId of node $fromNode")
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
        connections.forEach {
            it.input.dataBuffer.clear()
        }
        // Вносим данные в инпуты Flow
        log("setup inputs")
        inputs.forEach { (id, data) ->
            connections.find { it.output.node == this && it.output.id == id }
                ?.input?.dataBuffer?.addAll(data) ?: error("No input specified with id=$id")
        }

        coroutineScope {
            val nodeResults = Channel<List<Pair<NodeOutput, Any>>>()

            while (true) {
                //1. Проверить какие ноды готовы к запуску(все инпуты не пустые) и запустить их
                log("check nodes")
                for (nodeWithInfo in nodes) {
                    log("check $nodeWithInfo")
                    if (nodeWithInfo.status == Status.RUN) {
                        continue
                    }
                    val nodeInputs = nodeWithInfo.node.inConnections().map { it.input }.distinct()
                    if (nodeInputs.all { !it.dataBuffer.isEmpty() }) {
                        val dataReady = nodeInputs.minOf { it.dataBuffer.size }
                        nodeWithInfo.status = Status.RUN
                        runAsync(
                            nodeWithInfo.node,
                            nodeInputs.map { it.id to it.dataBuffer.take(dataReady) }.toMap(),
                            nodeResults
                        )

                    }
                }
                log("end of check")

                nodes.forEach {
                    if (it.status == Status.RUN)
                        log("${it.name} is still running")
                }
                log("break?")
                if (nodes.all { it.status == Status.IDLE }) {
                    nodeResults.cancel()
                    break
                }

                // 2. Сидеть и ждать когда сработает колбёк по завершении какой-либо ноды
                log("wait")

                select<Unit> {
                    nodeResults.onReceive { list ->
                        log("receive from ${list.first().first}: ${list.map { it.second }}")

                        list.forEach { (nodeOutput, data) ->
                            val nodeInput = connections.find { it.output == nodeOutput }!!.input
                            nodeInput.dataBuffer.addAll(data as List<*>)
                        }
                        nodes.find { it.node == list.first().first.node }!!.status = Status.IDLE
                    }
                }


            }
        }
        log("collect res")
        val res = mutableMapOf<Int, List<Any>>()
        connections.filter { it.input.node == this }.forEach {
            res[it.input.id] = it.input.dataBuffer.toList()
        }
        return res
    }

    private fun BaseNode.inConnections() = connections
        .filter { it.input.node == this }

    private fun CoroutineScope.runAsync(
        node: BaseNode,
        inputs: DataById,
        channel: Channel<List<Pair<NodeOutput, Any>>>
    ) {
        launch {
            log("run '${node.description}'")
            inputs.forEach { (id, data) ->
                log("args: $id ---- $data")
            }
            val outputData = node.run(inputs)
            log("results of '${node.description}: $outputData'")

//            val connection = connections.find { it.output.node == node && it.output.id == id } ?: error("a")
            val forSend = connections
                .filter { it.output.node == node }
                .map { it.output }
                .map { it to outputData.getValue(it.id) }
            log("send $forSend")
            channel.send(
                forSend
            )


//            outputData.forEach { (id, data) ->
//                val connection = connections.find { it.output.node == node && it.output.id == id } ?: error("a")
//                 3. Полученные данные положить в connection
//                log("send $data to ${connection.input}")
//                channel.send(connection to data)
//            }
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

            this@Flow.connections.forEach {
                when {
                    it.output.node == this@Flow ->
                        this.nodeByName(this@Flow.nodeInfo(it.input.node)!!.name)!!.node.flowInput(
                            it.output.id,
                            it.input.id
                        )
                    it.input.node == this@Flow ->
                        this.nodeByName(this@Flow.nodeInfo(it.output.node)!!.name)!!.node.flowOutput(
                            it.input.id,
                            it.output.id
                        )
                    else -> this.connect(
                        this@Flow.nodeInfo(it.output.node)!!.name,
                        it.output.id,
                        this@Flow.nodeInfo(it.input.node)!!.name,
                        it.input.id
                    )
                }
            }
//            this@Flow.connections.map { (output, input) ->
//
//
////                val from = if (output.node == this@Flow) {
////                    this
////                } else {
////                    this.nodes.find { it.name == this@Flow.nodeInfo(c.from)!!.name }!!.node
////                }
////                val to = if (c.to == this@Flow) {
////                    this
////                } else {
////                    this.nodes.find { it.name == this@Flow.nodeInfo(c.to)!!.name }!!.node
////                }
//
//                Connection(
//                    if (from.node == this@Flow) this else from,
//                    c.outputId,
//                    to,
//                    c.inputId
//                )
//            }.forEach(this.connections::add)
        }
    }
}

