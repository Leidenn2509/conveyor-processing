package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
class NewFlow(description: String) : NewBaseNode(description) {
    private val nodes: MutableList<NewBaseNode> = mutableListOf()

    fun node(id: Int) = nodes[id]

    fun <T : Any> addInput(id: Int, type: KClass<T>) {
        inputs[id] = NodeInput(this, id, type)
    }

    fun <T : Any> addOutput(id: Int, type: KClass<T>) {
        outputs[id] = NodeOutput(this, id, type)
    }

    fun addNode(id: Int, node: NewBaseNode) {
        nodes.add(id, node)
    }

    fun connect(from: Int, fromOutputId: Int, to: Int, toInputId: Int) {
        connect(nodes[from], fromOutputId, nodes[to], toInputId)
    }

    fun connect(from: NewBaseNode, fromOutputId: Int, to: NewBaseNode, toInputId: Int) {
        from.connectOutputTo(fromOutputId, to, toInputId)
    }

    fun connectFlowInput(flowInputId: Int, to: NewBaseNode, inputToId: Int) {
        inputs[flowInputId]?.let {
            val nodeInput = to.inputs[flowInputId]!!
            if (it.type != nodeInput.type)
                error("[$description] Type of input and input don't match: ${it.type} vs ${nodeInput.type}")
            nodeInput.channel = it.channel
            it.connectedTo = to
//            it.channel = nodeInput.channel
//            it.connectedTo = to
        }
    }

    fun connectFlowOutput(flowOutputId: Int, from: NewBaseNode, nodeOutputId: Int) {
        outputs[flowOutputId]?.let {
            val nodeOutput = from.outputs[nodeOutputId]!!
            if (it.type != nodeOutput.type)
                error("[$description] Type of output and output don't match: ${it.type} vs ${nodeOutput.type}")
            it.channel = nodeOutput.channel
            it.connectedTo = from
        }
    }


    override suspend fun tryRun(coroutineScope: CoroutineScope): Job = coroutineScope.launch {
        log("Try run flow")


        isRunning = true
        inputs.forEach { (_, input) ->
           input.connectedTo!!.tryRun(this)
        }
        body()
    }

    override suspend fun body() {
        while (nodes.any { it.isRunning }) {

        }
    }

}