package ru.nsu.fit.conveyor

abstract class BaseNode(
    val description: String
) {
    // FIXME нужна ли какая-то оченедь входных данных?

    protected var inputTypes: MutableMap<Int, Class<*>> = mutableMapOf()
    protected var outputTypes: MutableMap<Int, Class<*>> = mutableMapOf()

    fun addInput(id: Int, type: Class<*>) {
        inputTypes[id] = type
    }

    fun addOutput(id: Int, type: Class<*>) {
        outputTypes[id] = type
    }

    fun getInputType(id: Int): Class<*>? = inputTypes[id]

    fun getOutputType(id: Int): Class<*>? = outputTypes[id]

    abstract fun run(inputs: Map<Int, Any>): Map<Int, Any>
}

class Node(
    description: String = ""
) : BaseNode(description) {
    var body: (Map<Int, Any>) -> Map<Int, Any> = { mapOf() }

    fun copy(): Node {
        return Node(description).apply {
            this@Node.inputTypes.forEach(this::addInput)
            this@Node.outputTypes.forEach(this::addOutput)
            this.body = this@Node.body
        }
    }

    override fun run(inputs: Map<Int, Any>): Map<Int, Any> = body(inputs)
}


data class Connection(
    val from: BaseNode,
    val outputId: Int,
    val to: BaseNode,
    val inputId: Int

)

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
            throw IllegalStateException("nu tupoi")
        }
        connections.add(connection)

    }

    fun Node.input(flowInputId: Int, nodeInputId: Int): Node {
        connect(this@Flow, flowInputId, this, nodeInputId)
        return this
    }

    fun Node.output(nodeOutputId: Int, flowOutputId: Int): Node {
        connect(this, nodeOutputId, this@Flow, flowOutputId)
        return this
    }


    override fun run(inputs: Map<Int, Any>): Map<Int, Any> {
        var currentInputs = inputs
        var currentNode: BaseNode = this
        while (true) {
            val connection = connections.find { it.from == currentNode } ?: error("HAHAHA")
            if (connection.to == this) break
            currentInputs = connection.to.run(currentInputs)
            currentNode = connection.to
        }
        return currentInputs
    }
}


fun main() {
    val powNode = Node("simple pow").apply {
        addInput(0, Int::class.java)
        addOutput(0, Int::class.java)
        body = { map ->
            val a = map.getValue(0) as Int
            mapOf(0 to a * a)
        }
    }

    println(powNode.run(mapOf(0 to 8)))

    val flow = Flow("pow twice").apply {
        addInput(0, Int::class.java)
        addOutput(0, Int::class.java)

        addNode("pow1", powNode).input(0, 0)
        addNode("pow2", powNode)
        addNode("pow3", powNode).output(0, 0)
        connect("pow1", 0, "pow2", 0)
        connect("pow2", 0, "pow3", 0)

    }
    println(flow.run(mapOf(0 to 2)))
}
