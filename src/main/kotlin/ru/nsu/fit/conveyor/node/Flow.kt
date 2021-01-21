package ru.nsu.fit.conveyor.node

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

@ExperimentalCoroutinesApi
class Flow(description: String) : BaseNode(description) {
    private val nodes: MutableList<BaseNode> = mutableListOf()

    private inner class FlowContext : Context()

    fun node(id: Int) = nodes[id]

    fun <T : Any> addInput(id: Int, type: KClass<T>, withContext: Any? = null) =
        with(contexts[withContext] as FlowContext) {
            inputs[id] = NodeInput(this@Flow, id, type)
        }

    fun <T : Any> addOutput(id: Int, type: KClass<T>, withContext: Any? = null) =
        with(contexts[withContext] as FlowContext) {
            outputs[id] = NodeOutput(this@Flow, id, type)
        }

    fun addNode(id: Int, node: BaseNode) {
        nodes.add(id, node).also {
            node.initContext(this)
        }
    }

    fun connect(from: Int, fromOutputId: Int, to: Int, toInputId: Int) {
        connect(nodes[from], fromOutputId, nodes[to], toInputId)
    }

    fun connect(from: BaseNode, fromOutputId: Int, to: BaseNode, toInputId: Int) {
        from.connectOutputTo(fromOutputId, to, toInputId, this)
    }

    override fun initContext(withContext: Any?) {
        contexts[withContext] = FlowContext()
    }

    init {
        initContext(null)
    }

    fun connectFlowInput(flowInputId: Int, to: BaseNode, inputToId: Int, withContext: Any? = null) =
        with(contexts[withContext] as FlowContext) {
            inputs[flowInputId]?.let {
                val nodeInput = to.contexts[this@Flow]!!.inputs[flowInputId]!!
                if (it.type != nodeInput.type)
                    error("[$description] Type of input and input don't match: ${it.type} vs ${nodeInput.type}")
                nodeInput.channel = it.channel
                it.connectedTo = to
//            it.channel = nodeInput.channel
//            it.connectedTo = to
            }
        }

    fun connectFlowOutput(flowOutputId: Int, from: BaseNode, nodeOutputId: Int, withContext: Any? = null) =
        with(contexts[withContext] as FlowContext) {
            outputs[flowOutputId]?.let {
                val nodeOutput = from.contexts[this@Flow]!!.outputs[nodeOutputId]!!
                if (it.type != nodeOutput.type)
                    error("[$description] Type of output and output don't match: ${it.type} vs ${nodeOutput.type}")
                it.channel = nodeOutput.channel
                it.connectedTo = from
            }
        }


    override suspend fun tryRun(coroutineScope: CoroutineScope, withContext: Any?): Job = coroutineScope.launch {
        log("Try run flow", contexts[withContext]!!)
        val context = contexts[withContext] as FlowContext

        isRunning = true
        context.inputs.forEach { (_, input) ->
            input.connectedTo!!.tryRun(this, this@Flow)
        }
        body(context)
    }

    override suspend fun body(context: Context) {
//        while (nodes.any { it.isRunning }) {
//        }
    }
}