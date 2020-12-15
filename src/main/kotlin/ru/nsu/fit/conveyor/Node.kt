package ru.nsu.fit.conveyor

data class Node(
    var name: String = ""
) {
    // FIXME нужна ли какая-то оченедь входных данных?

    private var inputTypes: MutableMap<Int, Class<*>> = mutableMapOf()
    private var outputTypes: MutableMap<Int, Class<*>> = mutableMapOf()

    var body: (Map<Int, Any>) -> Map<Int, Any> = { mapOf() }

    fun addInput(id: Int, type: Class<*>) {
        inputTypes[id] = type
    }

    fun addOutput(id: Int, type: Class<*>) {
        outputTypes[id] = type
    }

    fun getInputType(id: Int): Class<*>? = inputTypes[id]

    fun getOutputType(id: Int): Class<*>? = outputTypes[id]

    fun tryRun(inputs: Map<Int, Any>): Map<Int, Any> = body(inputs)

}


data class Connection(
    val from: Node,
    val outputId: Int,
    val to: Node,
    val inputId: Int
)

class Flow {
    val nodes = mutableMapOf<String, Node>()
    private val connections: MutableList<Connection> = mutableListOf()

    fun addNode(node: Node): Node? = nodes.put(node.name, node)

    companion object {
        fun builder(init: FlowBuilder.() -> Unit = {}): FlowBuilder {
            //also let apply run
            return FlowBuilder().apply(init)
        }
    }
}

class FlowBuilder {
    private val flow = Flow()

    // FIXME
    // Такое поле уже есть во Flow. Сделать билдер иннером Flow? Получится тройная вложенность, что как-то ниоч


    fun node(name: String, init: NodeConfiguration.() -> Unit = {}): Node {
        return NodeConfiguration(Node(name), this).apply(init).build().also(flow::addNode)
    }

    fun node(name: String, node: Node, init: NodeConfiguration.() -> Unit = {}): Node {
        val newNode = node.copy(name = name)
        NodeConfiguration(newNode, this).apply(init).build()
        return newNode
    }

    fun build(): Flow = flow

    // FIXME maybe not inner?\
    // FIXME нельзя настроить существующую ноду...
    class NodeConfiguration(val node: Node, private val flowBuilder: FlowBuilder? = null) {
        fun setInput(nodeName: String, outputId: Int, inputId: Int) {
            flowBuilder
                ?: error("FLowBuilder is null in $this. Cant set input outside of Flow.")
            val outputNode = flowBuilder.nodes.find { it.name == nodeName } ?: error("")
            assert(node.getInputType(inputId) == outputNode.getOutputType(outputId))

            TODO()
        }


        fun build(): Node = node

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

    println(powNode.tryRun(mapOf(0 to 8)))

    val flow = Flow.builder {
        node("pow1", powNode)
        node("pow2", powNode) {
            setInput("pow1", 0, 0)
        }
    }.build()
}
